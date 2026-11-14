package com.smartcodeapps.constructionprizewheel.ui.admin.history

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartcodeapps.constructionprizewheel.R
import com.smartcodeapps.constructionprizewheel.ui.theme.ConstructionPrizeWheelTheme

object HistoryTestTags {
    const val SUMMARY = "history_summary"
    const val EMPTY = "history_empty"
    const val TAB_TIMELINE = "history_tab_timeline"
    const val TAB_BY_PRIZE = "history_tab_by_prize"
    fun row(id: Long) = "history_row_$id"
    fun prizeRow(prizeId: Long) = "history_prize_row_$prizeId"
    fun prizeCount(prizeId: Long) = "history_prize_count_$prizeId"
}

@Composable
fun HistoryScreen(onBack: () -> Unit, viewModel: HistoryViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    BackHandler(onBack = onBack)
    HistoryContent(state = state)
}

private enum class HistoryTab { TIMELINE, BY_PRIZE }

@Composable
fun HistoryContent(
    state: HistoryUiState,
    modifier: Modifier = Modifier
) {
    var tab by rememberSaveable { mutableStateOf(HistoryTab.TIMELINE) }

    Column(
        modifier = modifier.fillMaxSize().padding(start = 12.dp, top = 12.dp, end = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.history_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.history_summary, state.totalCount),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f).testTag(HistoryTestTags.SUMMARY)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = tab == HistoryTab.TIMELINE,
                    onClick = { tab = HistoryTab.TIMELINE },
                    label = { Text(stringResource(R.string.history_tab_timeline)) },
                    modifier = Modifier.testTag(HistoryTestTags.TAB_TIMELINE)
                )
                FilterChip(
                    selected = tab == HistoryTab.BY_PRIZE,
                    onClick = { tab = HistoryTab.BY_PRIZE },
                    label = { Text(stringResource(R.string.history_tab_by_prize)) },
                    modifier = Modifier.testTag(HistoryTestTags.TAB_BY_PRIZE)
                )
            }
        }

        // The list takes the rest of the height and scrolls down to the bottom edge of the panel.
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (state.loaded && state.entries.isEmpty()) {
                Text(
                    text = stringResource(R.string.history_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.align(Alignment.Center).testTag(HistoryTestTags.EMPTY)
                )
            } else if (tab == HistoryTab.TIMELINE) {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(state.entries, key = { it.id }) { entry ->
                        HistoryRow(entry = entry)
                    }
                }
            } else {
                val maxCount = state.prizeTotals.maxOfOrNull { it.count } ?: 1
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(state.prizeTotals, key = { it.prizeId }) { total ->
                        PrizeTotalRow(total = total, maxCount = maxCount)
                    }
                }
            }
        }
    }
}

@Composable
private fun PrizeTotalRow(total: PrizeTotal, maxCount: Int) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth().testTag(HistoryTestTags.prizeRow(total.prizeId))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = total.prizeName.ifBlank { "—" },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            // The bar is proportional to the most-won prize.
            Box(modifier = Modifier.weight(1f).height(12.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(total.count / maxCount.toFloat())
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp))
                )
            }
            Text(
                text = total.count.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.End,
                modifier = Modifier.width(48.dp).testTag(HistoryTestTags.prizeCount(total.prizeId))
            )
        }
    }
}

@Composable
private fun HistoryRow(entry: HistoryEntry) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth().testTag(HistoryTestTags.row(entry.id))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.prizeName.ifBlank { "—" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.history_date_time, entry.date, entry.time),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Preview(widthDp = 1024, heightDp = 600, showBackground = true)
@Composable
private fun HistoryPreview() {
    ConstructionPrizeWheelTheme {
        HistoryContent(
            state = HistoryUiState(
                loaded = true,
                entries = listOf(
                    HistoryEntry(3, "Professional Hammer", "13 Nov 2026", "14:05:12"),
                    HistoryEntry(2, "€50 Voucher", "13 Nov 2026", "13:58:40"),
                    HistoryEntry(1, "Paint Brush", "12 Nov 2026", "17:20:03")
                ),
                prizeTotals = listOf(
                    PrizeTotal(1, "Paint Brush", 20),
                    PrizeTotal(2, "€50 Voucher", 10),
                    PrizeTotal(3, "Professional Hammer", 3)
                )
            )
        )
    }
}
