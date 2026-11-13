package com.smartcodeapps.constructionprizewheel.ui.admin.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.smartcodeapps.constructionprizewheel.ui.theme.ConstructionPrizeWheelTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SettingsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var saves = 0
    private var changePins = 0
    private var resets = 0
    private var restores = 0
    private var messagesShown = 0
    private var defaultLogos = 0
    private val mainTitles = mutableListOf<String>()

    private val base = SettingsUiState(
        loaded = true,
        mainTitle = "SPIN THE WHEEL AND WIN!",
        congratsTitle = "CONGRATULATIONS!",
        congratsMessage = "YOU WON!"
    )

    private fun show(state: SettingsUiState = base, logoError: Boolean = false) {
        composeRule.setContent {
            ConstructionPrizeWheelTheme {
                SettingsContent(
                    state = state,
                    logoError = logoError,
                    onMainTitleChange = { mainTitles += it },
                    onCongratsTitleChange = {},
                    onCongratsMessageChange = {},
                    onChooseLogo = {},
                    onUseDefaultLogo = { defaultLogos++ },
                    onSaveBranding = { saves++ },
                    onChangePin = { changePins++ },
                    onResetHistory = { resets++ },
                    onRestore = { restores++ },
                    onMessageShown = { messagesShown++ },
                    onBack = {}
                )
            }
        }
    }

    @Test
    fun fieldsShowTheCurrentBranding() {
        show()
        composeRule.onNodeWithTag(SettingsTestTags.MAIN_TITLE).assertTextContains("SPIN THE WHEEL AND WIN!")
        composeRule.onNodeWithTag(SettingsTestTags.CONGRATS_TITLE).assertTextContains("CONGRATULATIONS!")
        composeRule.onNodeWithTag(SettingsTestTags.CONGRATS_MESSAGE).assertTextContains("YOU WON!")
    }

    @Test
    fun typingInAFieldReportsTheChange() {
        show()
        composeRule.onNodeWithTag(SettingsTestTags.MAIN_TITLE).performTextClearance()
        composeRule.onNodeWithTag(SettingsTestTags.MAIN_TITLE).performTextInput("HELLO")
        assertEquals("HELLO", mainTitles.last())
    }

    @Test
    fun saveBrandingIsDisabledUntilSomethingChanges() {
        show(base.copy(dirty = false, canSave = false))
        composeRule.onNodeWithTag(SettingsTestTags.SAVE_BRANDING).assertIsNotEnabled()
    }

    @Test
    fun saveBrandingWorksWhenThereAreValidChanges() {
        show(base.copy(dirty = true, canSave = true))
        composeRule.onNodeWithTag(SettingsTestTags.SAVE_BRANDING).assertIsEnabled().performClick()
        assertEquals(1, saves)
    }

    @Test
    fun blankFieldsShowAHintAndBlockSaving() {
        show(base.copy(mainTitle = "", dirty = true, canSave = false))
        composeRule.onNodeWithTag(SettingsTestTags.BLANK_HINT).assertIsDisplayed()
        composeRule.onNodeWithTag(SettingsTestTags.SAVE_BRANDING).assertIsNotEnabled()
    }

    @Test
    fun defaultLogoButtonIsDisabledWhenNoLogoIsSet() {
        show(base.copy(logoPath = null))
        composeRule.onNodeWithTag(SettingsTestTags.DEFAULT_LOGO).assertIsNotEnabled()
    }

    @Test
    fun defaultLogoButtonIsEnabledWhenALogoIsSet() {
        show(base.copy(logoPath = "/logo/a.png"))
        composeRule.onNodeWithTag(SettingsTestTags.DEFAULT_LOGO).assertIsEnabled().performClick()
        assertEquals(1, defaultLogos)
    }

    @Test
    fun logoErrorIsShown() {
        show(logoError = true)
        composeRule.onNodeWithTag(SettingsTestTags.LOGO_ERROR).assertIsDisplayed()
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

        composeRule.onNodeWithText("Restore configuration?").assertIsDisplayed()
        composeRule.onNodeWithText("The spin history is kept.", substring = true).assertIsDisplayed()
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
    fun savedMessageIsShownAndAcknowledged() {
        show(base.copy(message = SettingsMessage.BRANDING_SAVED))
        composeRule.onNodeWithText("Branding saved.").assertIsDisplayed()
        composeRule.waitForIdle()
        assertEquals(1, messagesShown)
    }

    @Test
    fun historyResetMessageIsShown() {
        show(base.copy(message = SettingsMessage.HISTORY_RESET))
        composeRule.onNodeWithText("Spin history deleted.").assertIsDisplayed()
    }
}
