package com.smartcodeapps.constructionprizewheel.ui.admin.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartcodeapps.constructionprizewheel.data.SpinRecordRepository
import com.smartcodeapps.constructionprizewheel.domain.RedemptionStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val spinRecordRepository: SpinRecordRepository,
    clock: Clock
) : ViewModel() {

    val uiState: StateFlow<HistoryUiState> = spinRecordRepository.observeHistory()
        .map { buildHistoryUiState(it, clock.zone, Locale.getDefault()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    fun onRedeemedChange(recordId: Long, redeemed: Boolean) {
        viewModelScope.launch {
            val status = if (redeemed) RedemptionStatus.REDEEMED else RedemptionStatus.NOT_REDEEMED
            spinRecordRepository.setRedemptionStatus(recordId, status)
        }
    }
}
