package com.smartcodeapps.constructionprizewheel.ui.result

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartcodeapps.constructionprizewheel.data.PrizeRepository
import com.smartcodeapps.constructionprizewheel.data.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

const val PRIZE_ID_ARG = "prizeId"

@HiltViewModel
class ResultViewModel @Inject constructor(
    prizeRepository: PrizeRepository,
    settingsRepository: SettingsRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val prizeId: Long = savedStateHandle[PRIZE_ID_ARG] ?: -1L

    val uiState: StateFlow<ResultUiState> = combine(
        prizeRepository.observePrizes(),
        settingsRepository.branding
    ) { prizes, branding -> buildResultUiState(prizes, prizeId, branding) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ResultUiState())
}
