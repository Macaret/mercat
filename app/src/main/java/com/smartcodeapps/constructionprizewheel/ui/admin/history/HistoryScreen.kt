package com.smartcodeapps.constructionprizewheel.ui.admin.history

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartcodeapps.constructionprizewheel.R
import com.smartcodeapps.constructionprizewheel.ui.theme.ConstructionPrizeWheelTheme

object HistoryTestTags {
    const val SUMMARY = "history_summary"
    const val EMPTY = "history_empty"
    fun row(id: Long) = "history_row_$id"
    fun status(id: Long) = "history_status_$id"
    fun redeemedSwitch(id: Long) = "history_switch_$id"
}

@Composable
fun HistoryScreen(onBack: () -> Unit, viewModel: HistoryViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    BackHandler(onBack = onBack)
    HistoryContent(state = state, onRedeemedChange = viewModel::onRedeemedChange)
}

@Composable
fun HistoryContent(
    state: HistoryUiState,
    onRedeemedChange: (recordId: Long, redeemed: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize().padding(start = 12.dp, top = 12.dp, end = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.history_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = stringResource(R.string.history_summary, state.totalCount, state.notRedeemedCount),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.testTag(HistoryTestTags.SUMMARY)
        )

        // The list takes the rest of the height and scrolls down to the bottom edge of the panel.
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (state.loaded && state.entries.isEmpty()) {
                Text(
                    text = stringResource(R.string.history_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.align(Alignment.Center).testTag(HistoryTestTags.EMPTY)
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(state.entries, key = { it.id }) { entry ->
                        HistoryRow(entry = entry, onRedeemedChange = { onRedeemedChange(entry.id, it) })
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(entry: HistoryEntry, onRedeemedChange: (Boolean) -> Unit) {
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
            Text(
                text = stringResource(if (entry.redeemed) R.string.status_redeemed else R.string.status_not_redeemed),
                style = MaterialTheme.typography.titleMedium,
                color = if (entry.redeemed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.testTag(HistoryTestTags.status(entry.id))
            )
            Switch(
                checked = entry.redeemed,
                onCheckedChange = onRedeemedChange,
                modifier = Modifier.testTag(HistoryTestTags.redeemedSwitch(entry.id))
            )
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
                    HistoryEntry(3, "Professional Hammer", "13 Nov 2026", "14:05:12", false),
                    HistoryEntry(2, "€50 Voucher", "13 Nov 2026", "13:58:40", true),
                    HistoryEntry(1, "Paint Brush", "12 Nov 2026", "17:20:03", false)
                )
            ),
            onRedeemedChange = { _, _ -> }
        )
    }
}
