package com.smartcodeapps.constructionprizewheel.ui.admin

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartcodeapps.constructionprizewheel.security.AdminAuthentication
import com.smartcodeapps.constructionprizewheel.security.PinPolicy
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class AdminAccessMode { LOADING, LOGIN, SETUP_ENTER, SETUP_CONFIRM }

enum class AdminAccessError { INCORRECT_PIN, INVALID_LENGTH, MISMATCH }

data class AdminAccessUiState(
    val mode: AdminAccessMode = AdminAccessMode.LOADING,
    val pin: String = "",
    val error: AdminAccessError? = null,
    val authenticated: Boolean = false
) {
    val canSubmit: Boolean
        get() = mode != AdminAccessMode.LOADING && pin.length == PinPolicy.LENGTH && !authenticated
}

const val FORCE_SETUP_ARG = "forceSetup"

@HiltViewModel
class AdminAccessViewModel @Inject constructor(
    private val authentication: AdminAuthentication,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val forceSetup: Boolean = savedStateHandle[FORCE_SETUP_ARG] ?: false

    private val _uiState = MutableStateFlow(AdminAccessUiState())
    val uiState: StateFlow<AdminAccessUiState> = _uiState.asStateFlow()

    private var firstEntry: String? = null

    init {
        viewModelScope.launch {
            val needsSetup = forceSetup || !authentication.hasPin()
            val mode = if (needsSetup) AdminAccessMode.SETUP_ENTER else AdminAccessMode.LOGIN
            _uiState.update { it.copy(mode = mode) }
        }
    }

    fun onDigit(digit: Int) {
        require(digit in 0..9)
        val current = _uiState.value
        if (current.mode == AdminAccessMode.LOADING || current.pin.length >= PinPolicy.LENGTH) return
        val updated = current.copy(pin = current.pin + digit, error = null)
        _uiState.value = updated
        // Submit as soon as the last digit is entered, so there is no separate confirm button.
        if (updated.pin.length == PinPolicy.LENGTH) onSubmit()
    }

    fun onBackspace() {
        _uiState.update { it.copy(pin = it.pin.dropLast(1), error = null) }
    }

    fun onSubmit() {
        val state = _uiState.value
        if (!state.canSubmit) return
        when (state.mode) {
            AdminAccessMode.LOGIN -> submitLogin(state.pin)
            AdminAccessMode.SETUP_ENTER -> submitFirstEntry(state.pin)
            AdminAccessMode.SETUP_CONFIRM -> submitConfirmation(state.pin)
            AdminAccessMode.LOADING -> Unit
        }
    }

    private fun submitLogin(pin: String) {
        viewModelScope.launch {
            if (authentication.verify(pin)) {
                _uiState.update { it.copy(authenticated = true, pin = "", error = null) }
            } else {
                _uiState.update { it.copy(pin = "", error = AdminAccessError.INCORRECT_PIN) }
            }
        }
    }

    private fun submitFirstEntry(pin: String) {
        firstEntry = pin
        _uiState.update { it.copy(mode = AdminAccessMode.SETUP_CONFIRM, pin = "", error = null) }
    }

    private fun submitConfirmation(pin: String) {
        if (pin != firstEntry) {
            firstEntry = null
            _uiState.update { it.copy(mode = AdminAccessMode.SETUP_ENTER, pin = "", error = AdminAccessError.MISMATCH) }
            return
        }
        viewModelScope.launch {
            if (authentication.setPin(pin)) {
                firstEntry = null
                _uiState.update { it.copy(authenticated = true, pin = "", error = null) }
            } else {
                firstEntry = null
                _uiState.update { it.copy(mode = AdminAccessMode.SETUP_ENTER, pin = "", error = AdminAccessError.INVALID_LENGTH) }
            }
        }
    }
}
