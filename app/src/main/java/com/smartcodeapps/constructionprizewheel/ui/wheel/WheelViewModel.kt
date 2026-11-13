package com.smartcodeapps.constructionprizewheel.ui.wheel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartcodeapps.constructionprizewheel.data.PrizeRepository
import com.smartcodeapps.constructionprizewheel.data.SettingsRepository
import com.smartcodeapps.constructionprizewheel.data.SpinRecordRepository
import com.smartcodeapps.constructionprizewheel.domain.PrizeSelectionEngine
import com.smartcodeapps.constructionprizewheel.domain.SpinOutcome
import com.smartcodeapps.constructionprizewheel.domain.WHEEL_SLOTS
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
            val outcome = selectionEngine.select(prizeRepository.observePrizes().first())
            spinState.update {
                it.copy(
                    plan = SpinPlan(
                        winnerPrizeId = (outcome as? SpinOutcome.Win)?.prize?.id,
                        winnerIndex = outcome.slotIndex,
                        targetRotation = SpinPlanner.targetRotation(
                            it.restingRotation, outcome.slotIndex, WHEEL_SLOTS, random
                        )
                    )
                )
            }
        }
    }

    /** A win is recorded and shown on the result screen. A bust is not recorded and simply re-enables spinning. */
    fun onSpinFinished() {
        val plan = spinState.value.plan ?: return
        viewModelScope.launch {
            val winner = plan.winnerPrizeId?.let { id ->
                prizeRepository.observePrizes().first().firstOrNull { it.id == id }
            }
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
