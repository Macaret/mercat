package com.smartcodeapps.constructionprizewheel.ui.admin

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.smartcodeapps.constructionprizewheel.ui.theme.ConstructionPrizeWheelTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AdminAccessScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val digits = mutableListOf<Int>()
    private var backspaces = 0
    private var submits = 0
    private var cancels = 0

    private fun show(state: AdminAccessUiState) {
        composeRule.setContent {
            ConstructionPrizeWheelTheme {
                AdminAccessContent(
                    state = state,
                    onDigit = { digits += it },
                    onBackspace = { backspaces++ },
                    onSubmit = { submits++ },
                    onCancel = { cancels++ }
                )
            }
        }
    }

    @Test
    fun keypadReportsEveryDigit() {
        show(AdminAccessUiState(mode = AdminAccessMode.LOGIN))
        (0..9).forEach { composeRule.onNodeWithTag(AdminAccessTestTags.key(it)).performClick() }
        assertEquals((0..9).toList(), digits)
    }

    @Test
    fun deleteSubmitAndCancelInvokeTheirCallbacks() {
        show(AdminAccessUiState(mode = AdminAccessMode.LOGIN, pin = "1234"))
        composeRule.onNodeWithTag(AdminAccessTestTags.KEY_DELETE).performClick()
        composeRule.onNodeWithTag(AdminAccessTestTags.SUBMIT).performClick()
        composeRule.onNodeWithTag(AdminAccessTestTags.CANCEL).performClick()

        assertEquals(1, backspaces)
        assertEquals(1, submits)
        assertEquals(1, cancels)
    }

    @Test
    fun pinIsShownAsFourSlotsAndNeverAsDigits() {
        show(AdminAccessUiState(mode = AdminAccessMode.LOGIN, pin = "48"))
        composeRule.onNodeWithTag(AdminAccessTestTags.PIN_DISPLAY).assertTextEquals("●●○○")
    }

    @Test
    fun emptyPinShowsFourEmptySlots() {
        show(AdminAccessUiState(mode = AdminAccessMode.LOGIN, pin = ""))
        composeRule.onNodeWithTag(AdminAccessTestTags.PIN_DISPLAY).assertTextEquals("○○○○")
    }

    @Test
    fun submitIsDisabledUntilFourDigitsAreEntered() {
        show(AdminAccessUiState(mode = AdminAccessMode.LOGIN, pin = "123"))
        composeRule.onNodeWithTag(AdminAccessTestTags.SUBMIT).assertIsNotEnabled()
    }

    @Test
    fun submitIsEnabledWithFourDigits() {
        show(AdminAccessUiState(mode = AdminAccessMode.LOGIN, pin = "1234"))
        composeRule.onNodeWithTag(AdminAccessTestTags.SUBMIT).assertIsEnabled()
    }

    @Test
    fun errorMessageIsShown() {
        show(AdminAccessUiState(mode = AdminAccessMode.LOGIN, error = AdminAccessError.INCORRECT_PIN))
        composeRule.onNodeWithTag(AdminAccessTestTags.ERROR).assertTextEquals("Incorrect PIN")
    }

    @Test
    fun titleAndButtonFollowTheMode() {
        show(AdminAccessUiState(mode = AdminAccessMode.SETUP_ENTER, pin = "1"))
        composeRule.onNodeWithTag(AdminAccessTestTags.TITLE).assertTextEquals("SET ADMINISTRATOR PIN")
        composeRule.onNodeWithTag(AdminAccessTestTags.SUBMIT).assertIsDisplayed()
    }
}
