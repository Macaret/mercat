package com.smartcodeapps.constructionprizewheel.ui.admin.history

import com.smartcodeapps.constructionprizewheel.domain.SpinRecord
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class HistoryEntry(
    val id: Long,
    val prizeName: String,
    val date: String,
    val time: String
)

/** How many times one prize was won. */
data class PrizeTotal(val prizeId: Long, val prizeName: String, val count: Int)

data class HistoryUiState(
    val loaded: Boolean = false,
    val entries: List<HistoryEntry> = emptyList(),
    val prizeTotals: List<PrizeTotal> = emptyList()
) {
    val totalCount: Int get() = entries.size
}

fun SpinRecord.toHistoryEntry(zone: ZoneId, locale: Locale): HistoryEntry {
    val local = spunAt.atZone(zone)
    return HistoryEntry(
        id = id,
        prizeName = prizeName,
        date = DateTimeFormatter.ofPattern("d MMM yyyy", locale).format(local),
        time = DateTimeFormatter.ofPattern("HH:mm:ss", locale).format(local)
    )
}

fun buildHistoryUiState(records: List<SpinRecord>, zone: ZoneId, locale: Locale): HistoryUiState =
    HistoryUiState(
        loaded = true,
        entries = records.map { it.toHistoryEntry(zone, locale) },
        prizeTotals = buildPrizeTotals(records)
    )

/** Groups by prize id (so renames and deleted prizes still add up), showing the latest saved name. */
fun buildPrizeTotals(records: List<SpinRecord>): List<PrizeTotal> =
    records.groupBy { it.prizeId }
        .map { (prizeId, group) ->
            PrizeTotal(prizeId, group.maxBy { it.spunAt }.prizeName, group.size)
        }
        .sortedWith(compareByDescending<PrizeTotal> { it.count }.thenBy { it.prizeName })
