package com.smartcodeapps.constructionprizewheel.ui.admin.settings

import android.net.Uri
import com.smartcodeapps.constructionprizewheel.data.Branding
import com.smartcodeapps.constructionprizewheel.data.SettingsRepository
import com.smartcodeapps.constructionprizewheel.data.SpinRecordRepository
import com.smartcodeapps.constructionprizewheel.data.image.ImageStore
import com.smartcodeapps.constructionprizewheel.domain.RestoreConfigurationUseCase
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val savedBranding = MutableStateFlow(
        Branding(logoPath = "/logo/a.png", mainTitle = "WIN!", congratsTitle = "YAY!", congratsMessage = "YOU WON")
    )
    private val settings = mockk<SettingsRepository>()
    private val spinRecords = mockk<SpinRecordRepository>()
    private val restore = mockk<RestoreConfigurationUseCase>()
    private val imageStore = mockk<ImageStore>()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { settings.branding } returns savedBranding
        coEvery { settings.updateBranding(any()) } coAnswers { savedBranding.value = firstArg() }
        coEvery { spinRecords.resetHistory() } returns Unit
        coEvery { restore() } returns Unit
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = SettingsViewModel(settings, spinRecords, restore, imageStore)

    @Test
    fun `form starts with the saved branding and no changes`() {
        val state = viewModel().uiState.value
        assertTrue(state.loaded)
        assertEquals("WIN!", state.mainTitle)
        assertEquals("YAY!", state.congratsTitle)
        assertEquals("YOU WON", state.congratsMessage)
        assertEquals("/logo/a.png", state.logoPath)
        assertFalse(state.dirty)
        assertFalse(state.canSave)
    }

    @Test
    fun `editing a field makes the form dirty and savable`() {
        val vm = viewModel()
        vm.onMainTitleChange("SPIN AND WIN")
        val state = vm.uiState.value
        assertTrue(state.dirty)
        assertTrue(state.canSave)
        assertFalse(state.showBlankHint)
    }

    @Test
    fun `changing a value back to the saved one is not a change`() {
        val vm = viewModel()
        vm.onMainTitleChange("SPIN AND WIN")
        vm.onMainTitleChange("WIN!")
        assertFalse(vm.uiState.value.dirty)
    }

    @Test
    fun `surrounding whitespace alone is not a change`() {
        val vm = viewModel()
        vm.onMainTitleChange("  WIN!  ")
        assertFalse(vm.uiState.value.dirty)
    }

    @Test
    fun `a blank title cannot be saved and shows the hint`() {
        val vm = viewModel()
        vm.onMainTitleChange("   ")
        val state = vm.uiState.value
        assertTrue(state.dirty)
        assertFalse(state.canSave)
        assertTrue(state.showBlankHint)
    }

    @Test
    fun `a blank congratulations message cannot be saved`() {
        val vm = viewModel()
        vm.onCongratsMessageChange("")
        assertFalse(vm.uiState.value.canSave)
    }

    @Test
    fun `saving writes the trimmed branding and reports it`() = runTest {
        val vm = viewModel()
        vm.onMainTitleChange("  SPIN AND WIN  ")
        vm.onCongratsTitleChange("WELL DONE")
        vm.saveBranding()

        coVerify(exactly = 1) {
            settings.updateBranding(Branding("/logo/a.png", "SPIN AND WIN", "WELL DONE", "YOU WON"))
        }
        val state = vm.uiState.value
        assertEquals(SettingsMessage.BRANDING_SAVED, state.message)
        assertFalse(state.dirty)
        assertEquals("SPIN AND WIN", state.mainTitle)
    }

    @Test
    fun `saving is ignored while a field is blank`() = runTest {
        val vm = viewModel()
        vm.onCongratsTitleChange("")
        vm.saveBranding()
        coVerify(exactly = 0) { settings.updateBranding(any()) }
    }

    @Test
    fun `picking a logo marks the form dirty and saving keeps the new path`() = runTest {
        val vm = viewModel()
        vm.onLogoPicked("/logo/b.png")
        assertTrue(vm.uiState.value.dirty)

        vm.saveBranding()
        coVerify { settings.updateBranding(match { it.logoPath == "/logo/b.png" }) }
    }

    @Test
    fun `using the default logo clears the path`() = runTest {
        val vm = viewModel()
        vm.onUseDefaultLogo()
        assertNull(vm.uiState.value.logoPath)
        assertTrue(vm.uiState.value.dirty)

        vm.saveBranding()
        coVerify { settings.updateBranding(match { it.logoPath == null }) }
    }

    @Test
    fun `the message can be dismissed`() = runTest {
        val vm = viewModel()
        vm.onMainTitleChange("NEW")
        vm.saveBranding()
        vm.onMessageShown()
        assertNull(vm.uiState.value.message)
    }

    @Test
    fun `resetting history deletes the records and reports it`() = runTest {
        val vm = viewModel()
        vm.resetSpinHistory()

        coVerify(exactly = 1) { spinRecords.resetHistory() }
        assertEquals(SettingsMessage.HISTORY_RESET, vm.uiState.value.message)
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

    @Test
    fun `imported logos are delegated to the image store`() = runTest {
        val uri = mockk<Uri>()
        coEvery { imageStore.saveLogo(uri) } returns "/logo/new.png"
        assertEquals("/logo/new.png", viewModel().importLogo(uri))
    }
}
