package com.smartcodeapps.constructionprizewheel.ui.admin.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.smartcodeapps.constructionprizewheel.ui.theme.ConstructionPrizeWheelTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SettingsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var changePins = 0
    private var resets = 0
    private var restores = 0
    private var messagesShown = 0

    private fun show(state: SettingsUiState = SettingsUiState()) {
        composeRule.setContent {
            ConstructionPrizeWheelTheme {
                SettingsContent(
                    state = state,
                    onChangePin = { changePins++ },
                    onResetHistory = { resets++ },
                    onRestore = { restores++ },
                    onMessageShown = { messagesShown++ }
                )
            }
        }
    }

    @Test
    fun changePinButtonInvokesItsCallback() {
        show()
        composeRule.onNodeWithTag(SettingsTestTags.CHANGE_PIN).performScrollTo().performClick()
        assertEquals(1, changePins)
    }

    @Test
    fun resetHistoryAsksForConfirmationBeforeDoingAnything() {
        show()
        composeRule.onNodeWithTag(SettingsTestTags.RESET_HISTORY).performScrollTo().performClick()

        composeRule.onNodeWithTag(SettingsTestTags.CONFIRM_TITLE).assertIsDisplayed()
        assertEquals(0, resets)

        composeRule.onNodeWithTag(SettingsTestTags.CONFIRM).performClick()
        assertEquals(1, resets)
    }

    @Test
    fun cancellingTheResetConfirmationDoesNothing() {
        show()
        composeRule.onNodeWithTag(SettingsTestTags.RESET_HISTORY).performScrollTo().performClick()
        composeRule.onNodeWithTag(SettingsTestTags.CANCEL_CONFIRM).performClick()

        assertEquals(0, resets)
        composeRule.onNodeWithTag(SettingsTestTags.CONFIRM_TITLE).assertDoesNotExist()
    }

    @Test
    fun restoreAsksForConfirmationAndExplainsWhatIsDeleted() {
        show()
        composeRule.onNodeWithTag(SettingsTestTags.RESTORE).performScrollTo().performClick()

        composeRule.onNodeWithText("Restaurezi configurarea?").assertIsDisplayed()
        composeRule.onNodeWithText("Istoricul rotirilor este păstrat.", substring = true).assertIsDisplayed()
        assertEquals(0, restores)

        composeRule.onNodeWithTag(SettingsTestTags.CONFIRM).performClick()
        assertEquals(1, restores)
        assertEquals(0, resets)
    }

    @Test
    fun cancellingTheRestoreConfirmationDoesNothing() {
        show()
        composeRule.onNodeWithTag(SettingsTestTags.RESTORE).performScrollTo().performClick()
        composeRule.onNodeWithTag(SettingsTestTags.CANCEL_CONFIRM).performClick()
        assertEquals(0, restores)
    }

    @Test
    fun historyResetMessageIsShownAndAcknowledged() {
        show(SettingsUiState(message = SettingsMessage.HISTORY_RESET))
        composeRule.onNodeWithText("Istoricul rotirilor a fost șters.").assertIsDisplayed()
        composeRule.waitForIdle()
        assertEquals(1, messagesShown)
    }
}
