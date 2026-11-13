package com.smartcodeapps.constructionprizewheel.ui.wheel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartcodeapps.constructionprizewheel.data.PrizeRepository
import com.smartcodeapps.constructionprizewheel.data.SettingsRepository
import com.smartcodeapps.constructionprizewheel.data.SpinRecordRepository
import com.smartcodeapps.constructionprizewheel.domain.PrizeSelectionEngine
import com.smartcodeapps.constructionprizewheel.domain.wheelPrizes
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
import kotlin.random.Random

@HiltViewModel
class WheelViewModel @Inject constructor(
    private val prizeRepository: PrizeRepository,
    settingsRepository: SettingsRepository,
    private val spinRecordRepository: SpinRecordRepository,
    private val selectionEngine: PrizeSelectionEngine,
    private val random: Random
) : ViewModel() {

    private val spinState = MutableStateFlow(SpinState())

    val uiState: StateFlow<WheelUiState> = combine(
        prizeRepository.observePrizes(),
        settingsRepository.branding,
        spinState,
        ::buildWheelUiState
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WheelUiState())

    fun onSpin() {
        val current = spinState.value
        if (current.busy || !spinState.compareAndSet(current, current.copy(busy = true))) return

        viewModelScope.launch {
            val wheel = prizeRepository.observePrizes().first().wheelPrizes()
            val winner = selectionEngine.select(wheel)
            if (winner == null) {
                spinState.update { it.copy(busy = false) }
                return@launch
            }
            val winnerIndex = wheel.indexOfFirst { it.id == winner.id }
            spinState.update {
                it.copy(
                    plan = SpinPlan(
                        winnerPrizeId = winner.id,
                        winnerIndex = winnerIndex,
                        targetRotation = SpinPlanner.targetRotation(
                            it.restingRotation, winnerIndex, wheel.size, random
                        )
                    )
                )
            }
        }
    }

    fun onSpinFinished() {
        val plan = spinState.value.plan ?: return
        viewModelScope.launch {
            val winner = prizeRepository.observePrizes().first().firstOrNull { it.id == plan.winnerPrizeId }
            if (winner != null) spinRecordRepository.recordSpin(winner)
            spinState.update {
                it.copy(
                    restingRotation = SpinPlanner.normalize(plan.targetRotation),
                    plan = null,
                    resultPrizeId = winner?.id,
                    busy = winner != null
                )
            }
        }
    }

    fun onResultShown() {
        spinState.update { it.copy(resultPrizeId = null) }
    }

    fun onResultDismissed() {
        spinState.update { it.copy(busy = false) }
    }
}
