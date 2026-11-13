package com.smartcodeapps.constructionprizewheel.ui.admin.settings

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartcodeapps.constructionprizewheel.R
import com.smartcodeapps.constructionprizewheel.ui.admin.UnsavedChangesDialog
import com.smartcodeapps.constructionprizewheel.ui.common.rememberImageBitmap
import com.smartcodeapps.constructionprizewheel.ui.theme.ConstructionPrizeWheelTheme
import kotlinx.coroutines.launch
import java.io.IOException

object SettingsTestTags {
    const val BACK = "settings_back"
    const val MAIN_TITLE = "settings_main_title"
    const val CONGRATS_TITLE = "settings_congrats_title"
    const val CONGRATS_MESSAGE = "settings_congrats_message"
    const val CHOOSE_LOGO = "settings_choose_logo"
    const val DEFAULT_LOGO = "settings_default_logo"
    const val LOGO_ERROR = "settings_logo_error"
    const val SAVE_BRANDING = "settings_save_branding"
    const val BLANK_HINT = "settings_blank_hint"
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
    SettingsMessage.BRANDING_SAVED -> R.string.msg_branding_saved
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
    var logoError by remember { mutableStateOf(false) }
    var confirmLeave by remember { mutableStateOf(false) }
    val requestBack = { if (state.dirty) confirmLeave = true else onBack() }
    val scope = rememberCoroutineScope()

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    viewModel.onLogoPicked(viewModel.importLogo(uri))
                    logoError = false
                } catch (_: IOException) {
                    logoError = true
                }
            }
        }
    }

    LaunchedEffect(state.restored) {
        if (state.restored) onRestored()
    }
    BackHandler(onBack = requestBack)

    SettingsContent(
        state = state,
        logoError = logoError,
        onMainTitleChange = viewModel::onMainTitleChange,
        onCongratsTitleChange = viewModel::onCongratsTitleChange,
        onCongratsMessageChange = viewModel::onCongratsMessageChange,
        onChooseLogo = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
        onUseDefaultLogo = {
            logoError = false
            viewModel.onUseDefaultLogo()
        },
        onSaveBranding = viewModel::saveBranding,
        onChangePin = onChangePin,
        onResetHistory = viewModel::resetSpinHistory,
        onRestore = viewModel::restoreConfiguration,
        onMessageShown = viewModel::onMessageShown,
        onBack = requestBack
    )

    if (confirmLeave) {
        UnsavedChangesDialog(
            onDiscard = {
                confirmLeave = false
                onBack()
            },
            onKeepEditing = { confirmLeave = false }
        )
    }
}

@Composable
fun SettingsContent(
    state: SettingsUiState,
    logoError: Boolean,
    onMainTitleChange: (String) -> Unit,
    onCongratsTitleChange: (String) -> Unit,
    onCongratsMessageChange: (String) -> Unit,
    onChooseLogo: () -> Unit,
    onUseDefaultLogo: () -> Unit,
    onSaveBranding: () -> Unit,
    onChangePin: () -> Unit,
    onResetHistory: () -> Unit,
    onRestore: () -> Unit,
    onMessageShown: () -> Unit,
    onBack: () -> Unit,
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
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack, modifier = Modifier.testTag(SettingsTestTags.BACK)) {
                    Text(stringResource(R.string.back_button), style = MaterialTheme.typography.titleMedium)
                }
                Text(
                    text = stringResource(R.string.settings_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            SectionTitle(R.string.settings_branding_header)
            BrandingSection(
                state = state,
                logoError = logoError,
                onMainTitleChange = onMainTitleChange,
                onCongratsTitleChange = onCongratsTitleChange,
                onCongratsMessageChange = onCongratsMessageChange,
                onChooseLogo = onChooseLogo,
                onUseDefaultLogo = onUseDefaultLogo,
                onSaveBranding = onSaveBranding
            )

            HorizontalDivider()
            SectionTitle(R.string.settings_security_header)
            OutlinedButton(
                onClick = onChangePin,
                modifier = Modifier.height(64.dp).testTag(SettingsTestTags.CHANGE_PIN)
            ) { Text(stringResource(R.string.change_pin_button), style = MaterialTheme.typography.titleMedium) }

            HorizontalDivider()
            SectionTitle(R.string.settings_data_header)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedButton(
                    onClick = { pending = ConfirmAction.RESET_HISTORY },
                    modifier = Modifier.height(64.dp).testTag(SettingsTestTags.RESET_HISTORY)
                ) {
                    Text(
                        stringResource(R.string.reset_history_button),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                OutlinedButton(
                    onClick = { pending = ConfirmAction.RESTORE },
                    modifier = Modifier.height(64.dp).testTag(SettingsTestTags.RESTORE)
                ) {
                    Text(
                        stringResource(R.string.restore_button),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
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
                Button(
                    onClick = {
                        pending = null
                        when (action) {
                            ConfirmAction.RESET_HISTORY -> onResetHistory()
                            ConfirmAction.RESTORE -> onRestore()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag(SettingsTestTags.CONFIRM)
                ) { Text(stringResource(action.confirmLabel)) }
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

@Composable
private fun BrandingSection(
    state: SettingsUiState,
    logoError: Boolean,
    onMainTitleChange: (String) -> Unit,
    onCongratsTitleChange: (String) -> Unit,
    onCongratsMessageChange: (String) -> Unit,
    onChooseLogo: () -> Unit,
    onUseDefaultLogo: () -> Unit,
    onSaveBranding: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(220.dp)) {
                val logo = rememberImageBitmap(state.logoPath)
                if (logo != null) {
                    Image(bitmap = logo, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.size(140.dp))
                } else {
                    Image(painter = painterResource(R.drawable.ic_logo_placeholder), contentDescription = null, modifier = Modifier.size(140.dp))
                }
                OutlinedButton(
                    onClick = onChooseLogo,
                    modifier = Modifier.padding(top = 12.dp).fillMaxWidth().testTag(SettingsTestTags.CHOOSE_LOGO)
                ) { Text(stringResource(R.string.choose_logo_button)) }
                TextButton(
                    onClick = onUseDefaultLogo,
                    enabled = state.logoPath != null,
                    modifier = Modifier.testTag(SettingsTestTags.DEFAULT_LOGO)
                ) { Text(stringResource(R.string.default_logo_button)) }
                if (logoError) {
                    Text(
                        text = stringResource(R.string.image_error),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.testTag(SettingsTestTags.LOGO_ERROR)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = state.mainTitle,
                    onValueChange = onMainTitleChange,
                    label = { Text(stringResource(R.string.field_main_title)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag(SettingsTestTags.MAIN_TITLE)
                )
                OutlinedTextField(
                    value = state.congratsTitle,
                    onValueChange = onCongratsTitleChange,
                    label = { Text(stringResource(R.string.field_congrats_title)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag(SettingsTestTags.CONGRATS_TITLE)
                )
                OutlinedTextField(
                    value = state.congratsMessage,
                    onValueChange = onCongratsMessageChange,
                    label = { Text(stringResource(R.string.field_congrats_message)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag(SettingsTestTags.CONGRATS_MESSAGE)
                )
                if (state.showBlankHint) {
                    Text(
                        text = stringResource(R.string.branding_blank_hint),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.testTag(SettingsTestTags.BLANK_HINT)
                    )
                }
                Button(
                    onClick = onSaveBranding,
                    enabled = state.canSave,
                    modifier = Modifier.height(56.dp).testTag(SettingsTestTags.SAVE_BRANDING)
                ) { Text(stringResource(R.string.save_branding_button), style = MaterialTheme.typography.titleMedium) }
            }
        }
    }
}

@Preview(widthDp = 1024, heightDp = 700, showBackground = true)
@Composable
private fun SettingsPreview() {
    ConstructionPrizeWheelTheme {
        SettingsContent(
            state = SettingsUiState(
                loaded = true,
                mainTitle = "SPIN THE WHEEL AND WIN!",
                congratsTitle = "CONGRATULATIONS!",
                congratsMessage = "YOU WON!",
                dirty = true,
                canSave = true
            ),
            logoError = false,
            onMainTitleChange = {}, onCongratsTitleChange = {}, onCongratsMessageChange = {},
            onChooseLogo = {}, onUseDefaultLogo = {}, onSaveBranding = {}, onChangePin = {},
            onResetHistory = {}, onRestore = {}, onMessageShown = {}, onBack = {}
        )
    }
}
