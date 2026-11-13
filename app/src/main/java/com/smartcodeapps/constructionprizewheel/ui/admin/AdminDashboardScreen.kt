package com.smartcodeapps.constructionprizewheel.ui.admin

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.smartcodeapps.constructionprizewheel.R

object AdminDashboardTestTags {
    const val PRIZES = "dashboard_prizes"
    const val HISTORY = "dashboard_history"
    const val SETTINGS = "dashboard_settings"
    const val EXIT = "dashboard_exit"
}

@Composable
fun AdminDashboardScreen(
    hasUnsavedChanges: Boolean,
    onDiscardChanges: () -> Unit,
    onOpenPrizes: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    onExit: () -> Unit
) {
    var confirmExit by remember { mutableStateOf(false) }
    val requestExit = { if (hasUnsavedChanges) confirmExit = true else onExit() }

    BackHandler(onBack = requestExit)
    AdminDashboardContent(
        onOpenPrizes = onOpenPrizes,
        onOpenHistory = onOpenHistory,
        onOpenSettings = onOpenSettings,
        onExit = requestExit
    )

    if (confirmExit) {
        UnsavedChangesDialog(
            onDiscard = {
                confirmExit = false
                onDiscardChanges()
                onExit()
            },
            onKeepEditing = { confirmExit = false }
        )
    }
}

@Composable
fun AdminDashboardContent(
    onOpenPrizes: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = stringResource(R.string.admin_panel_title), style = MaterialTheme.typography.displaySmall)
        Button(
            onClick = onOpenPrizes,
            modifier = Modifier.width(360.dp).height(80.dp).testTag(AdminDashboardTestTags.PRIZES)
        ) {
            Text(text = stringResource(R.string.prizes_button), style = MaterialTheme.typography.titleLarge)
        }
        Button(
            onClick = onOpenHistory,
            modifier = Modifier.width(360.dp).height(80.dp).testTag(AdminDashboardTestTags.HISTORY)
        ) {
            Text(text = stringResource(R.string.history_button), style = MaterialTheme.typography.titleLarge)
        }
        Button(
            onClick = onOpenSettings,
            modifier = Modifier.width(360.dp).height(80.dp).testTag(AdminDashboardTestTags.SETTINGS)
        ) {
            Text(text = stringResource(R.string.settings_button), style = MaterialTheme.typography.titleLarge)
        }
        OutlinedButton(
            onClick = onExit,
            modifier = Modifier.width(360.dp).height(80.dp).testTag(AdminDashboardTestTags.EXIT)
        ) {
            Text(text = stringResource(R.string.exit_to_wheel_button), style = MaterialTheme.typography.titleLarge)
        }
    }
}
