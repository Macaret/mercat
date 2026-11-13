package com.smartcodeapps.constructionprizewheel.ui.admin.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartcodeapps.constructionprizewheel.data.SpinRecordRepository
import com.smartcodeapps.constructionprizewheel.domain.RestoreConfigurationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SettingsMessage { HISTORY_RESET }

data class SettingsUiState(
    val message: SettingsMessage? = null,
    val restored: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val spinRecordRepository: SpinRecordRepository,
    private val restoreConfigurationUseCase: RestoreConfigurationUseCase
) : ViewModel() {

    private val message = MutableStateFlow<SettingsMessage?>(null)
    private val restored = MutableStateFlow(false)

    val uiState: StateFlow<SettingsUiState> = combine(message, restored) { pendingMessage, wasRestored ->
        SettingsUiState(message = pendingMessage, restored = wasRestored)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SettingsUiState())

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
}
