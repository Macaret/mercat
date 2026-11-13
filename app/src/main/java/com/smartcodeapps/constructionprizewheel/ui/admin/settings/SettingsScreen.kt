package com.smartcodeapps.constructionprizewheel.ui.admin.settings

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartcodeapps.constructionprizewheel.R
import com.smartcodeapps.constructionprizewheel.ui.common.BluePill
import com.smartcodeapps.constructionprizewheel.ui.common.GlossyPillButton
import com.smartcodeapps.constructionprizewheel.ui.common.RedPill
import com.smartcodeapps.constructionprizewheel.ui.theme.ConstructionPrizeWheelTheme

object SettingsTestTags {
    const val CHANGE_PIN = "settings_change_pin"
    const val RESET_HISTORY = "settings_reset_history"
    const val RESTORE = "settings_restore"
    const val CONFIRM = "settings_confirm"
    const val CANCEL_CONFIRM = "settings_cancel_confirm"
    const val CONFIRM_TITLE = "settings_confirm_title"
}

private enum class ConfirmAction(
    @StringRes val title: Int,
    @StringRes val message: Int,
    @StringRes val confirmLabel: Int
) {
    RESET_HISTORY(R.string.reset_history_title, R.string.reset_history_message, R.string.confirm_reset_button),
    RESTORE(R.string.restore_title, R.string.restore_message, R.string.confirm_restore_button)
}

private fun SettingsMessage.stringRes(): Int = when (this) {
    SettingsMessage.HISTORY_RESET -> R.string.msg_history_reset
}

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onChangePin: () -> Unit,
    onRestored: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.restored) {
        if (state.restored) onRestored()
    }
    BackHandler(onBack = onBack)

    SettingsContent(
        state = state,
        onChangePin = onChangePin,
        onResetHistory = viewModel::resetSpinHistory,
        onRestore = viewModel::restoreConfiguration,
        onMessageShown = viewModel::onMessageShown
    )
}

@Composable
fun SettingsContent(
    state: SettingsUiState,
    onChangePin: () -> Unit,
    onResetHistory: () -> Unit,
    onRestore: () -> Unit,
    onMessageShown: () -> Unit,
    modifier: Modifier = Modifier
) {
    var pending by remember { mutableStateOf<ConfirmAction?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val messageText = state.message?.let { stringResource(it.stringRes()) }
    LaunchedEffect(messageText) {
        if (messageText != null) {
            snackbarHostState.showSnackbar(messageText)
            onMessageShown()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 12.dp, top = 12.dp, end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            SectionTitle(R.string.settings_security_header)
            GlossyPillButton(
                text = stringResource(R.string.change_pin_button),
                onClick = onChangePin,
                palette = BluePill,
                textSize = 16.sp,
                modifier = Modifier.height(56.dp).testTag(SettingsTestTags.CHANGE_PIN)
            )

            HorizontalDivider()
            SectionTitle(R.string.settings_data_header)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                GlossyPillButton(
                    text = stringResource(R.string.reset_history_button),
                    onClick = { pending = ConfirmAction.RESET_HISTORY },
                    palette = RedPill,
                    textSize = 16.sp,
                    modifier = Modifier.height(56.dp).testTag(SettingsTestTags.RESET_HISTORY)
                )
                GlossyPillButton(
                    text = stringResource(R.string.restore_button),
                    onClick = { pending = ConfirmAction.RESTORE },
                    palette = RedPill,
                    textSize = 16.sp,
                    modifier = Modifier.height(56.dp).testTag(SettingsTestTags.RESTORE)
                )
            }
            Spacer(Modifier.height(12.dp))
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp)
        )
    }

    pending?.let { action ->
        AlertDialog(
            onDismissRequest = { pending = null },
            title = {
                Text(stringResource(action.title), modifier = Modifier.testTag(SettingsTestTags.CONFIRM_TITLE))
            },
            text = { Text(stringResource(action.message)) },
            confirmButton = {
                GlossyPillButton(
                    text = stringResource(action.confirmLabel),
                    onClick = {
                        pending = null
                        when (action) {
                            ConfirmAction.RESET_HISTORY -> onResetHistory()
                            ConfirmAction.RESTORE -> onRestore()
                        }
                    },
                    palette = RedPill,
                    textSize = 16.sp,
                    modifier = Modifier.height(48.dp).testTag(SettingsTestTags.CONFIRM)
                )
            },
            dismissButton = {
                TextButton(onClick = { pending = null }, modifier = Modifier.testTag(SettingsTestTags.CANCEL_CONFIRM)) {
                    Text(stringResource(R.string.cancel_button))
                }
            }
        )
    }
}

@Composable
private fun SectionTitle(@StringRes res: Int) {
    Text(
        text = stringResource(res),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
}

@Preview(widthDp = 1024, heightDp = 700, showBackground = true)
@Composable
private fun SettingsPreview() {
    ConstructionPrizeWheelTheme {
        SettingsContent(
            state = SettingsUiState(),
            onChangePin = {}, onResetHistory = {}, onRestore = {}, onMessageShown = {}
        )
    }
}
