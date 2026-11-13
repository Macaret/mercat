package com.smartcodeapps.constructionprizewheel.ui.admin.history

import com.smartcodeapps.constructionprizewheel.domain.RedemptionStatus
import com.smartcodeapps.constructionprizewheel.domain.SpinRecord
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class HistoryEntry(
    val id: Long,
    val prizeName: String,
    val date: String,
    val time: String,
    val redeemed: Boolean
)

data class HistoryUiState(
    val loaded: Boolean = false,
    val entries: List<HistoryEntry> = emptyList()
) {
    val totalCount: Int get() = entries.size
    val notRedeemedCount: Int get() = entries.count { !it.redeemed }
}

fun SpinRecord.toHistoryEntry(zone: ZoneId, locale: Locale): HistoryEntry {
    val local = spunAt.atZone(zone)
    return HistoryEntry(
        id = id,
        prizeName = prizeName,
        date = DateTimeFormatter.ofPattern("d MMM yyyy", locale).format(local),
        time = DateTimeFormatter.ofPattern("HH:mm:ss", locale).format(local),
        redeemed = redemptionStatus == RedemptionStatus.REDEEMED
    )
}

fun buildHistoryUiState(records: List<SpinRecord>, zone: ZoneId, locale: Locale): HistoryUiState =
    HistoryUiState(loaded = true, entries = records.map { it.toHistoryEntry(zone, locale) })
