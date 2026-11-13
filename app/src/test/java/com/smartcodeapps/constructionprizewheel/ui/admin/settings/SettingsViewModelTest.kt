package com.smartcodeapps.constructionprizewheel.ui.admin.settings

import com.smartcodeapps.constructionprizewheel.data.SpinRecordRepository
import com.smartcodeapps.constructionprizewheel.domain.RestoreConfigurationUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val spinRecords = mockk<SpinRecordRepository>()
    private val restore = mockk<RestoreConfigurationUseCase>()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        coEvery { spinRecords.resetHistory() } returns Unit
        coEvery { restore() } returns Unit
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = SettingsViewModel(spinRecords, restore)

    @Test
    fun `starts with no message and not restored`() {
        val state = viewModel().uiState.value
        assertNull(state.message)
        assertFalse(state.restored)
    }

    @Test
    fun `resetting history deletes the records and reports it`() = runTest {
        val vm = viewModel()
        vm.resetSpinHistory()

        coVerify(exactly = 1) { spinRecords.resetHistory() }
        assertEquals(SettingsMessage.HISTORY_RESET, vm.uiState.value.message)
    }

    @Test
    fun `the message can be dismissed`() = runTest {
        val vm = viewModel()
        vm.resetSpinHistory()
        vm.onMessageShown()
        assertNull(vm.uiState.value.message)
    }

    @Test
    fun `restoring runs the use case and marks the settings as restored`() = runTest {
        val vm = viewModel()
        assertFalse(vm.uiState.value.restored)

        vm.restoreConfiguration()

        coVerify(exactly = 1) { restore() }
        assertTrue(vm.uiState.value.restored)
    }

    @Test
    fun `restoring does not touch the spin history`() = runTest {
        viewModel().restoreConfiguration()
        coVerify(exactly = 0) { spinRecords.resetHistory() }
    }
}
