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

    /** Returns [draws] in order from nextInt, then keeps repeating the last one. */
    private class FixedRandom(private vararg val draws: Int) : Random() {
        private var next = 0
        override fun nextBits(bitCount: Int) = 0
        override fun nextInt(until: Int) = draws[minOf(next++, draws.lastIndex)]
        override fun nextFloat() = 0.5f
    }

    private fun prize(id: Long, probability: Int, order: Int, active: Boolean = true) =
        Prize(id, "Prize $id", null, "/img/$id.jpg", probability, active, if (active) order else null)

    private val spinRecords = mockk<SpinRecordRepository>()

    private fun viewModel(prizes: List<Prize>, vararg draws: Int = intArrayOf(0)): WheelViewModel {
        val prizeRepository = mockk<PrizeRepository>()
        every { prizeRepository.observePrizes() } returns flowOf(prizes)
        val settingsRepository = mockk<SettingsRepository>()
        every { settingsRepository.branding } returns flowOf(Branding())
        return WheelViewModel(
            prizeRepository,
            settingsRepository,
            spinRecords,
            PrizeSelectionEngine(FixedRandom(*draws)),
            FixedRandom(0)
        )
    }

    // Slots 0, 2 and 5 hold the prizes; the rest are busts with no chance (total is 100).
    private val threePrizes = listOf(prize(10, 20, 1), prize(20, 50, 2), prize(30, 30, 3))

    // Slots 0 and 4 hold the prizes; busts (1, 2, 3, 5, 6, 7) share the remaining 70%.
    private val twoPrizes = listOf(prize(10, 10, 1), prize(20, 20, 2))

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
        val vm = viewModel(threePrizes, 25)
        vm.uiState.test {
            vm.onSpin()
            val state = expectMostRecentItem()

            val plan = state.spinPlan
            assertNotNull(plan)
            assertEquals(20L, plan!!.winnerPrizeId)
            assertEquals(2, plan.winnerIndex)
            assertEquals(2, SpinPlanner.segmentUnderPointer(plan.targetRotation, 8))
            assertTrue(state.spinning)
            assertFalse(state.canSpin)
            coVerify(exactly = 0) { spinRecords.recordSpin(any()) }
        }
    }

    @Test
    fun `winner index follows wheel order rather than id order`() = runTest {
        val reordered = listOf(prize(10, 50, order = 3), prize(20, 50, order = 1))
        val vm = viewModel(reordered, 0)
        vm.uiState.test {
            vm.onSpin()
            val plan = expectMostRecentItem().spinPlan!!
            assertEquals(20L, plan.winnerPrizeId)
            assertEquals(0, plan.winnerIndex)
        }
    }

    @Test
    fun `a second tap while spinning does not change the plan`() = runTest {
        val vm = viewModel(threePrizes,5)
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
        val vm = viewModel(threePrizes,60)
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
        val vm = viewModel(threePrizes,60)
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
        val vm = viewModel(threePrizes,60)
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
        val vm = viewModel(threePrizes,10)
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
    fun `a bust lands on a bust slot`() = runTest {
        val vm = viewModel(twoPrizes, 30, 2)
        vm.uiState.test {
            vm.onSpin()
            val plan = expectMostRecentItem().spinPlan!!
            assertNull(plan.winnerPrizeId)
            assertEquals(3, plan.winnerIndex)
            assertEquals(3, SpinPlanner.segmentUnderPointer(plan.targetRotation, 8))
        }
    }

    @Test
    fun `finishing a bust records nothing, shows no result and re-enables spinning`() = runTest {
        val vm = viewModel(twoPrizes, 99, 5)
        vm.uiState.test {
            vm.onSpin()
            val target = expectMostRecentItem().spinPlan!!.targetRotation
            vm.onSpinFinished()
            val state = expectMostRecentItem()

            coVerify(exactly = 0) { spinRecords.recordSpin(any()) }
            assertNull(state.resultPrizeId)
            assertNull(state.spinPlan)
            assertFalse(state.spinning)
            assertTrue(state.canSpin)
            assertEquals(SpinPlanner.normalize(target), state.restingRotation, 0.01f)
        }
    }

    @Test
    fun `no active prizes still spins and always busts`() = runTest {
        val vm = viewModel(listOf(prize(10, 100, 1, active = false)), 0, 6)
        vm.uiState.test {
            val idle = expectMostRecentItem()
            assertEquals(8, idle.segments.size)
            assertTrue(idle.canSpin)

            vm.onSpin()
            val plan = expectMostRecentItem().spinPlan!!
            assertNull(plan.winnerPrizeId)
            assertEquals(6, plan.winnerIndex)

            vm.onSpinFinished()
            assertTrue(expectMostRecentItem().canSpin)
            coVerify(exactly = 0) { spinRecords.recordSpin(any()) }
        }
    }
}
