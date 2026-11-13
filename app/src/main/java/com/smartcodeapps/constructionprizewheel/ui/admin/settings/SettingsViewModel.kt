package com.smartcodeapps.constructionprizewheel.ui.admin.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartcodeapps.constructionprizewheel.data.Branding
import com.smartcodeapps.constructionprizewheel.data.SettingsRepository
import com.smartcodeapps.constructionprizewheel.data.SpinRecordRepository
import com.smartcodeapps.constructionprizewheel.data.image.ImageStore
import com.smartcodeapps.constructionprizewheel.domain.RestoreConfigurationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SettingsMessage { BRANDING_SAVED, HISTORY_RESET }

data class SettingsUiState(
    val loaded: Boolean = false,
    val logoPath: String? = null,
    val mainTitle: String = "",
    val congratsTitle: String = "",
    val congratsMessage: String = "",
    val dirty: Boolean = false,
    val canSave: Boolean = false,
    val message: SettingsMessage? = null,
    val restored: Boolean = false
) {
    val showBlankHint: Boolean get() = dirty && !canSave
}

private data class BrandingForm(
    val logoPath: String?,
    val mainTitle: String,
    val congratsTitle: String,
    val congratsMessage: String
)

private fun Branding.toForm() = BrandingForm(logoPath, mainTitle, congratsTitle, congratsMessage)

private fun BrandingForm.toBranding() = Branding(logoPath, mainTitle.trim(), congratsTitle.trim(), congratsMessage.trim())

private fun Branding.isComplete() = mainTitle.isNotEmpty() && congratsTitle.isNotEmpty() && congratsMessage.isNotEmpty()

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val spinRecordRepository: SpinRecordRepository,
    private val restoreConfigurationUseCase: RestoreConfigurationUseCase,
    private val imageStore: ImageStore
) : ViewModel() {

    private val form = MutableStateFlow<BrandingForm?>(null)
    private val message = MutableStateFlow<SettingsMessage?>(null)
    private val restored = MutableStateFlow(false)

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.branding, form, message, restored
    ) { saved, current, pendingMessage, wasRestored ->
        if (current == null) {
            SettingsUiState(message = pendingMessage, restored = wasRestored)
        } else {
            val edited = current.toBranding()
            val dirty = edited != saved
            SettingsUiState(
                loaded = true,
                logoPath = current.logoPath,
                mainTitle = current.mainTitle,
                congratsTitle = current.congratsTitle,
                congratsMessage = current.congratsMessage,
                dirty = dirty,
                canSave = dirty && edited.isComplete(),
                message = pendingMessage,
                restored = wasRestored
            )
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SettingsUiState())

    init {
        viewModelScope.launch { form.value = settingsRepository.branding.first().toForm() }
    }

    fun onMainTitleChange(value: String) = updateForm { it.copy(mainTitle = value) }

    fun onCongratsTitleChange(value: String) = updateForm { it.copy(congratsTitle = value) }

    fun onCongratsMessageChange(value: String) = updateForm { it.copy(congratsMessage = value) }

    fun onLogoPicked(path: String) = updateForm { it.copy(logoPath = path) }

    fun onUseDefaultLogo() = updateForm { it.copy(logoPath = null) }

    suspend fun importLogo(source: Uri): String = imageStore.saveLogo(source)

    fun saveBranding() {
        val edited = form.value?.toBranding() ?: return
        if (!edited.isComplete()) return
        viewModelScope.launch {
            settingsRepository.updateBranding(edited)
            form.value = edited.toForm()
            message.value = SettingsMessage.BRANDING_SAVED
        }
    }

    fun resetSpinHistory() {
        viewModelScope.launch {
            spinRecordRepository.resetHistory()
            message.value = SettingsMessage.HISTORY_RESET
        }
    }

    fun restoreConfiguration() {
        viewModelScope.launch {
            restoreConfigurationUseCase()
            restored.value = true
        }
    }

    fun onMessageShown() {
        message.value = null
    }

    private fun updateForm(change: (BrandingForm) -> BrandingForm) {
        form.update { current -> current?.let(change) }
    }
}
