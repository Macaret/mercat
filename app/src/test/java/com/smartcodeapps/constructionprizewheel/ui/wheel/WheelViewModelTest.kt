package com.smartcodeapps.constructionprizewheel.ui.wheel

import app.cash.turbine.test
import com.smartcodeapps.constructionprizewheel.data.Branding
import com.smartcodeapps.constructionprizewheel.data.PrizeRepository
import com.smartcodeapps.constructionprizewheel.data.SettingsRepository
import com.smartcodeapps.constructionprizewheel.data.SpinRecordRepository
import com.smartcodeapps.constructionprizewheel.domain.Prize
import com.smartcodeapps.constructionprizewheel.domain.PrizeSelectionEngine
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class WheelViewModelTest {

    private class FixedRandom(private val draw: Int) : Random() {
        override fun nextBits(bitCount: Int) = 0
        override fun nextInt(until: Int) = draw
        override fun nextFloat() = 0.5f
    }

    private fun prize(id: Long, probability: Int, order: Int, active: Boolean = true) =
        Prize(id, "Prize $id", null, "/img/$id.jpg", probability, active, if (active) order else null)

    private val spinRecords = mockk<SpinRecordRepository>()

    private fun viewModel(prizes: List<Prize>, draw: Int = 0): WheelViewModel {
        val prizeRepository = mockk<PrizeRepository>()
        every { prizeRepository.observePrizes() } returns flowOf(prizes)
        val settingsRepository = mockk<SettingsRepository>()
        every { settingsRepository.branding } returns flowOf(Branding())
        return WheelViewModel(
            prizeRepository,
            settingsRepository,
            spinRecords,
            PrizeSelectionEngine(FixedRandom(draw)),
            FixedRandom(draw)
        )
    }

    private val threePrizes = listOf(prize(10, 20, 1), prize(20, 50, 2), prize(30, 30, 3))

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        coEvery { spinRecords.recordSpin(any()) } returns 1L
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `spin is idle and enabled before the first tap`() = runTest {
        viewModel(threePrizes).uiState.test {
            val state = expectMostRecentItem()
            assertTrue(state.canSpin)
            assertNull(state.spinPlan)
        }
    }

    @Test
    fun `tapping spin creates a plan with the winner already chosen`() = runTest {
        val vm = viewModel(threePrizes, draw = 25)
        vm.uiState.test {
            vm.onSpin()
            val state = expectMostRecentItem()

            val plan = state.spinPlan
            assertNotNull(plan)
            assertEquals(20L, plan!!.winnerPrizeId)
            assertEquals(1, plan.winnerIndex)
            assertEquals(1, SpinPlanner.segmentUnderPointer(plan.targetRotation, 3))
            assertTrue(state.spinning)
            assertFalse(state.canSpin)
            coVerify(exactly = 0) { spinRecords.recordSpin(any()) }
        }
    }

    @Test
    fun `winner index follows wheel order rather than id order`() = runTest {
        val reordered = listOf(prize(10, 50, order = 3), prize(20, 50, order = 1))
        val vm = viewModel(reordered, draw = 0)
        vm.uiState.test {
            vm.onSpin()
            val plan = expectMostRecentItem().spinPlan!!
            assertEquals(20L, plan.winnerPrizeId)
            assertEquals(0, plan.winnerIndex)
        }
    }

    @Test
    fun `a second tap while spinning does not change the plan`() = runTest {
        val vm = viewModel(threePrizes, draw = 5)
        vm.uiState.test {
            vm.onSpin()
            val first = expectMostRecentItem().spinPlan
            vm.onSpin()
            expectNoEvents()
            assertEquals(first, vm.uiState.value.spinPlan)
        }
    }

    @Test
    fun `finishing the spin saves the record before exposing the result`() = runTest {
        val vm = viewModel(threePrizes, draw = 60)
        var resultAtSaveTime: Long? = -1L
        coEvery { spinRecords.recordSpin(any()) } answers {
            resultAtSaveTime = vm.uiState.value.resultPrizeId
            1L
        }

        vm.uiState.test {
            vm.onSpin()
            expectMostRecentItem()
            vm.onSpinFinished()
            val state = expectMostRecentItem()

            coVerify(exactly = 1) { spinRecords.recordSpin(match { it.id == 20L }) }
            assertNull("result must not be visible while saving", resultAtSaveTime)
            assertEquals(20L, state.resultPrizeId)
        }
    }

    @Test
    fun `finishing the spin keeps the landing angle and keeps the button disabled`() = runTest {
        val vm = viewModel(threePrizes, draw = 60)
        vm.uiState.test {
            vm.onSpin()
            val target = expectMostRecentItem().spinPlan!!.targetRotation

            vm.onSpinFinished()
            val state = expectMostRecentItem()

            assertNull(state.spinPlan)
            assertTrue(state.spinning)
            assertFalse(state.canSpin)
            assertEquals(SpinPlanner.normalize(target), state.restingRotation, 0.01f)
        }
    }

    @Test
    fun `button stays disabled after navigation and re-enables when the result is dismissed`() = runTest {
        val vm = viewModel(threePrizes, draw = 60)
        vm.uiState.test {
            vm.onSpin()
            expectMostRecentItem()
            vm.onSpinFinished()
            expectMostRecentItem()

            vm.onResultShown()
            val shown = expectMostRecentItem()
            assertNull(shown.resultPrizeId)
            assertFalse(shown.canSpin)

            vm.onResultDismissed()
            assertTrue(expectMostRecentItem().canSpin)
        }
    }

    @Test
    fun `a second spin can be started after dismissing the result`() = runTest {
        val vm = viewModel(threePrizes, draw = 10)
        vm.uiState.test {
            vm.onSpin()
            expectMostRecentItem()
            vm.onSpinFinished()
            vm.onResultShown()
            vm.onResultDismissed()
            expectMostRecentItem()

            vm.onSpin()
            assertNotNull(expectMostRecentItem().spinPlan)
            coVerify(exactly = 1) { spinRecords.recordSpin(any()) }
        }
    }

    @Test
    fun `no active prizes means spin does nothing`() = runTest {
        val vm = viewModel(listOf(prize(10, 100, 1, active = false)))
        vm.uiState.test {
            vm.onSpin()
            val state = expectMostRecentItem()
            assertFalse(state.canSpin)
            assertNull(state.spinPlan)
            assertFalse(state.spinning)
        }
    }
}
