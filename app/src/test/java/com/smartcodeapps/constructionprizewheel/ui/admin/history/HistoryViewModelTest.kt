package com.smartcodeapps.constructionprizewheel.ui.admin.history

import app.cash.turbine.test
import com.smartcodeapps.constructionprizewheel.data.SpinRecordRepository
import com.smartcodeapps.constructionprizewheel.domain.RedemptionStatus
import com.smartcodeapps.constructionprizewheel.domain.SpinRecord
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {

    private val records = MutableStateFlow(
        listOf(
            SpinRecord(2, Instant.parse("2026-11-13T12:00:00Z"), 10, "Hammer", RedemptionStatus.NOT_REDEEMED),
            SpinRecord(1, Instant.parse("2026-11-13T10:00:00Z"), 20, "Gloves", RedemptionStatus.REDEEMED)
        )
    )
    private val repository = mockk<SpinRecordRepository>()
    private val clock = Clock.fixed(Instant.parse("2026-11-13T12:00:00Z"), ZoneOffset.UTC)

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { repository.observeHistory() } returns records
        coEvery { repository.setRedemptionStatus(any(), any()) } returns Unit
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = HistoryViewModel(repository, clock)

    @Test
    fun `history entries are loaded newest first`() = runTest {
        viewModel().uiState.test {
            val state = expectMostRecentItem()
            assertTrue(state.loaded)
            assertEquals(listOf(2L, 1L), state.entries.map { it.id })
            assertEquals(listOf("Hammer", "Gloves"), state.entries.map { it.prizeName })
        }
    }

    @Test
    fun `status reflects each record`() = runTest {
        viewModel().uiState.test {
            val state = expectMostRecentItem()
            assertFalse(state.entries[0].redeemed)
            assertTrue(state.entries[1].redeemed)
            assertEquals(1, state.notRedeemedCount)
        }
    }

    @Test
    fun `switching on marks the record as redeemed`() = runTest {
        viewModel().onRedeemedChange(2, true)
        coVerify(exactly = 1) { repository.setRedemptionStatus(2, RedemptionStatus.REDEEMED) }
    }

    @Test
    fun `switching off marks the record as not redeemed`() = runTest {
        viewModel().onRedeemedChange(1, false)
        coVerify(exactly = 1) { repository.setRedemptionStatus(1, RedemptionStatus.NOT_REDEEMED) }
    }

    @Test
    fun `new spins appear in the list as they are recorded`() = runTest {
        viewModel().uiState.test {
            assertEquals(2, expectMostRecentItem().totalCount)

            records.value = listOf(
                SpinRecord(3, Instant.parse("2026-11-13T13:00:00Z"), 30, "Drill", RedemptionStatus.NOT_REDEEMED)
            ) + records.value

            val state = expectMostRecentItem()
            assertEquals(3, state.totalCount)
            assertEquals(3L, state.entries.first().id)
        }
    }

    @Test
    fun `an empty history is shown as loaded and empty`() = runTest {
        records.value = emptyList()
        viewModel().uiState.test {
            val state = expectMostRecentItem()
            assertTrue(state.loaded)
            assertTrue(state.entries.isEmpty())
        }
    }
}
