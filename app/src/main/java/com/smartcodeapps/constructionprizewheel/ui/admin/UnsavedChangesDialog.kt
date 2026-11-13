package com.smartcodeapps.constructionprizewheel.ui.admin

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.smartcodeapps.constructionprizewheel.R

object UnsavedDialogTestTags {
    const val DISCARD = "unsaved_discard"
    const val KEEP_EDITING = "unsaved_keep_editing"
}

@Composable
fun UnsavedChangesDialog(onDiscard: () -> Unit, onKeepEditing: () -> Unit) {
    AlertDialog(
        onDismissRequest = onKeepEditing,
        title = { Text(stringResource(R.string.unsaved_title)) },
        text = { Text(stringResource(R.string.unsaved_message)) },
        confirmButton = {
            TextButton(onClick = onDiscard, modifier = Modifier.testTag(UnsavedDialogTestTags.DISCARD)) {
                Text(stringResource(R.string.discard_changes_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onKeepEditing, modifier = Modifier.testTag(UnsavedDialogTestTags.KEEP_EDITING)) {
                Text(stringResource(R.string.keep_editing_button))
            }
        }
    )
}
