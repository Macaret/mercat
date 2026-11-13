package com.smartcodeapps.constructionprizewheel.ui.admin.prizes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.smartcodeapps.constructionprizewheel.R

@Composable
internal fun ErrorsCard(state: PrizeEditorUiState, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = modifier.fillMaxWidth().testTag(PrizeListTestTags.ERRORS)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            state.errors.forEach { error ->
                val text = error.toErrorText(state.prizes)
                Text(
                    text = stringResource(text.resId, *text.args.toTypedArray()),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@Composable
internal fun SaveBar(
    state: PrizeEditorUiState,
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = modifier) {
        OutlinedButton(
            onClick = onDiscard,
            enabled = state.dirty && !state.saving,
            modifier = Modifier.height(56.dp).testTag(PrizeListTestTags.DISCARD)
        ) { Text(stringResource(R.string.discard_button), style = MaterialTheme.typography.titleMedium) }
        Button(
            onClick = onSave,
            enabled = state.dirty && !state.saving,
            modifier = Modifier.height(56.dp).testTag(PrizeListTestTags.SAVE)
        ) { Text(stringResource(R.string.save_button), style = MaterialTheme.typography.titleMedium) }
    }
}
