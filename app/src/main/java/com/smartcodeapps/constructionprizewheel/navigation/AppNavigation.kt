package com.smartcodeapps.constructionprizewheel.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.smartcodeapps.constructionprizewheel.ui.admin.AdminAccessScreen
import com.smartcodeapps.constructionprizewheel.ui.admin.AdminLayout
import com.smartcodeapps.constructionprizewheel.ui.admin.AdminSection
import com.smartcodeapps.constructionprizewheel.ui.admin.FORCE_SETUP_ARG
import com.smartcodeapps.constructionprizewheel.ui.admin.prizes.EditPrizeScreen
import com.smartcodeapps.constructionprizewheel.ui.admin.prizes.NEW_PRIZE_ID
import com.smartcodeapps.constructionprizewheel.ui.admin.prizes.PrizeEditorViewModel
import com.smartcodeapps.constructionprizewheel.ui.admin.prizes.PrizeListScreen
import com.smartcodeapps.constructionprizewheel.ui.admin.UnsavedChangesDialog
import com.smartcodeapps.constructionprizewheel.ui.admin.history.HistoryScreen
import com.smartcodeapps.constructionprizewheel.ui.admin.settings.SettingsScreen
import com.smartcodeapps.constructionprizewheel.ui.result.PRIZE_ID_ARG
import com.smartcodeapps.constructionprizewheel.ui.result.PrizeResultScreen
import com.smartcodeapps.constructionprizewheel.ui.wheel.WheelScreen
import com.smartcodeapps.constructionprizewheel.ui.wheel.WheelViewModel

object Routes {
    const val WHEEL = "wheel"
    const val RESULT = "result/{$PRIZE_ID_ARG}"
    const val ADMIN_ACCESS = "admin_access"
    const val ADMIN_GRAPH = "admin_graph"
    /** Admin home: the menu layout showing the prize list. */
    const val ADMIN_DASHBOARD = "admin_dashboard"
    const val HISTORY = "admin_history"
    const val SETTINGS = "admin_settings"
    const val CHANGE_PIN = "admin_change_pin?$FORCE_SETUP_ARG={$FORCE_SETUP_ARG}"
    const val PRIZE_EDIT = "admin_prize_edit/{$PRIZE_ID_ARG}"

    fun result(prizeId: Long) = "result/$prizeId"
    fun prizeEdit(prizeId: Long) = "admin_prize_edit/$prizeId"
    fun changePin() = "admin_change_pin?$FORCE_SETUP_ARG=true"
}

private fun NavBackStackEntry.isResumed() = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)

@Composable
private fun adminEditor(navController: NavController, entry: NavBackStackEntry): PrizeEditorViewModel {
    val graphEntry = remember(entry) { navController.getBackStackEntry(Routes.ADMIN_GRAPH) }
    return hiltViewModel(graphEntry)
}

/**
 * Wraps an admin section in the two-pane layout. Switching sections or exiting asks for
 * confirmation first when [hasUnsavedChanges] is true.
 */
@Composable
private fun AdminScreen(
    section: AdminSection,
    hasUnsavedChanges: Boolean,
    onDiscardChanges: () -> Unit,
    onSelect: (AdminSection) -> Unit,
    onExit: () -> Unit,
    content: @Composable () -> Unit,
) {
    var confirmExit by remember { mutableStateOf(false) }
    var pendingSection by remember { mutableStateOf<AdminSection?>(null) }

    AdminLayout(
        selected = section,
        onSelect = { target ->
            when {
                target == section -> Unit
                hasUnsavedChanges -> pendingSection = target
                else -> onSelect(target)
            }
        },
        onExit = { if (hasUnsavedChanges) confirmExit = true else onExit() },
        content = content
    )

    if (confirmExit || pendingSection != null) {
        UnsavedChangesDialog(
            onDiscard = {
                val exit = confirmExit
                val target = pendingSection
                confirmExit = false
                pendingSection = null
                onDiscardChanges()
                if (exit) onExit() else target?.let(onSelect)
            },
            onKeepEditing = {
                confirmExit = false
                pendingSection = null
            }
        )
    }
}

@Composable
fun AppNavigation(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = Routes.WHEEL,
        modifier = modifier
    ) {
        composable(Routes.WHEEL) {
            WheelScreen(
                onNavigateToResult = { prizeId -> navController.navigate(Routes.result(prizeId)) },
                onNavigateToAdmin = { navController.navigate(Routes.ADMIN_ACCESS) }
            )
        }
        composable(
            route = Routes.RESULT,
            arguments = listOf(navArgument(PRIZE_ID_ARG) { type = NavType.LongType })
        ) { entry ->
            val wheelEntry = remember(entry) { navController.getBackStackEntry(Routes.WHEEL) }
            val wheelViewModel: WheelViewModel = hiltViewModel(wheelEntry)
            PrizeResultScreen(
                onContinue = {
                    if (entry.isResumed()) {
                        wheelViewModel.onResultDismissed()
                        navController.popBackStack()
                    }
                }
            )
        }
        composable(Routes.ADMIN_ACCESS) { entry ->
            AdminAccessScreen(
                onAuthenticated = {
                    navController.navigate(Routes.ADMIN_GRAPH) {
                        popUpTo(Routes.ADMIN_ACCESS) { inclusive = true }
                    }
                },
                onCancel = { if (entry.isResumed()) navController.popBackStack() }
            )
        }
        navigation(startDestination = Routes.ADMIN_DASHBOARD, route = Routes.ADMIN_GRAPH) {

            fun openSection(section: AdminSection, entry: NavBackStackEntry) {
                if (!entry.isResumed()) return
                val route = when (section) {
                    AdminSection.PRIZES -> Routes.ADMIN_DASHBOARD
                    AdminSection.HISTORY -> Routes.HISTORY
                    AdminSection.SETTINGS -> Routes.SETTINGS
                }
                navController.navigate(route) {
                    popUpTo(Routes.ADMIN_DASHBOARD) { inclusive = false }
                    launchSingleTop = true
                }
            }

            fun exitAdmin(entry: NavBackStackEntry) {
                if (entry.isResumed()) navController.popBackStack(Routes.WHEEL, inclusive = false)
            }

            composable(Routes.ADMIN_DASHBOARD) { entry ->
                val editor = adminEditor(navController, entry)
                val editorState by editor.uiState.collectAsStateWithLifecycle()
                AdminScreen(
                    section = AdminSection.PRIZES,
                    hasUnsavedChanges = editorState.dirty,
                    onDiscardChanges = editor::discard,
                    onSelect = { openSection(it, entry) },
                    onExit = { exitAdmin(entry) }
                ) {
                    PrizeListScreen(
                        editor = editor,
                        onAddPrize = {
                            if (entry.isResumed()) navController.navigate(Routes.prizeEdit(NEW_PRIZE_ID))
                        },
                        onEditPrize = { id -> if (entry.isResumed()) navController.navigate(Routes.prizeEdit(id)) },
                        onBack = { exitAdmin(entry) }
                    )
                }
            }
            composable(Routes.HISTORY) { entry ->
                AdminScreen(
                    section = AdminSection.HISTORY,
                    hasUnsavedChanges = false,
                    onDiscardChanges = {},
                    onSelect = { openSection(it, entry) },
                    onExit = { exitAdmin(entry) }
                ) {
                    HistoryScreen(onBack = { openSection(AdminSection.PRIZES, entry) })
                }
            }
            composable(Routes.SETTINGS) { entry ->
                AdminScreen(
                    section = AdminSection.SETTINGS,
                    hasUnsavedChanges = false,
                    onDiscardChanges = {},
                    onSelect = { openSection(it, entry) },
                    onExit = { exitAdmin(entry) }
                ) {
                    SettingsScreen(
                        onBack = { openSection(AdminSection.PRIZES, entry) },
                        onChangePin = { if (entry.isResumed()) navController.navigate(Routes.changePin()) },
                        onRestored = { navController.popBackStack(Routes.WHEEL, inclusive = false) }
                    )
                }
            }
            composable(
                route = Routes.CHANGE_PIN,
                arguments = listOf(navArgument(FORCE_SETUP_ARG) {
                    type = NavType.BoolType
                    defaultValue = false
                })
            ) { entry ->
                AdminAccessScreen(
                    onAuthenticated = { navController.popBackStack() },
                    onCancel = { if (entry.isResumed()) navController.popBackStack() }
                )
            }
            composable(
                route = Routes.PRIZE_EDIT,
                arguments = listOf(navArgument(PRIZE_ID_ARG) { type = NavType.LongType })
            ) { entry ->
                EditPrizeScreen(
                    editor = adminEditor(navController, entry),
                    prizeId = entry.arguments?.getLong(PRIZE_ID_ARG) ?: NEW_PRIZE_ID,
                    onFinished = { if (entry.isResumed()) navController.popBackStack() }
                )
            }
        }
    }
}
