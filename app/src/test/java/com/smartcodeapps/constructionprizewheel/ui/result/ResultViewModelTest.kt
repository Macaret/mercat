package com.smartcodeapps.constructionprizewheel.ui.result

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.smartcodeapps.constructionprizewheel.data.Branding
import com.smartcodeapps.constructionprizewheel.data.PrizeRepository
import com.smartcodeapps.constructionprizewheel.data.SettingsRepository
import com.smartcodeapps.constructionprizewheel.domain.Prize
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ResultViewModelTest {

    private val prizes = listOf(
        Prize(10, "Hammer", "Heavy duty", "/img/10.jpg", 50, true, 1),
        Prize(20, "Gloves", null, "/img/20.jpg", 50, true, 2)
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(prizeId: Long?, branding: Branding = Branding()): ResultViewModel {
        val prizeRepository = mockk<PrizeRepository>()
        every { prizeRepository.observePrizes() } returns flowOf(prizes)
        val settingsRepository = mockk<SettingsRepository>()
        every { settingsRepository.branding } returns flowOf(branding)
        val handle = SavedStateHandle(if (prizeId == null) emptyMap() else mapOf(PRIZE_ID_ARG to prizeId))
        return ResultViewModel(prizeRepository, settingsRepository, handle)
    }

    @Test
    fun `winning prize details are shown`() = runTest {
        viewModel(10L).uiState.test {
            val state = expectMostRecentItem()
            assertTrue(state.loaded)
            assertEquals(ResultPrize("Hammer", "Heavy duty", "/img/10.jpg"), state.prize)
        }
    }

    @Test
    fun `prize without description has none`() = runTest {
        viewModel(20L).uiState.test {
            assertNull(expectMostRecentItem().prize?.description)
        }
    }

    @Test
    fun `branding titles are used`() = runTest {
        val branding = Branding(congratsTitle = "WELL DONE", congratsMessage = "ENJOY")
        viewModel(10L, branding).uiState.test {
            val state = expectMostRecentItem()
            assertEquals("WELL DONE", state.congratsTitle)
            assertEquals("ENJOY", state.congratsMessage)
        }
    }

    @Test
    fun `unknown prize id yields no prize`() = runTest {
        viewModel(999L).uiState.test {
            assertNull(expectMostRecentItem().prize)
        }
    }

    @Test
    fun `missing prize id yields no prize`() = runTest {
        viewModel(null).uiState.test {
            assertNull(expectMostRecentItem().prize)
        }
    }
}
