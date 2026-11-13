package com.smartcodeapps.constructionprizewheel.ui.result

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.smartcodeapps.constructionprizewheel.ui.theme.ConstructionPrizeWheelTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PrizeResultScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun show(state: ResultUiState, onContinue: () -> Unit = {}) {
        composeRule.setContent {
            ConstructionPrizeWheelTheme { ResultContent(state = state, onContinue = onContinue) }
        }
    }

    @Test
    fun showsCongratulationsTextAndPrizeDetails() {
        show(ResultUiState(loaded = true, prize = ResultPrize("Hammer", "Heavy duty", null)))

        composeRule.onNodeWithContentDescription("Felicitări, ai câștigat un premiu!").assertIsDisplayed()
        composeRule.onNodeWithTag(ResultTestTags.PRIZE_NAME).assertIsDisplayed()
        composeRule.onNodeWithTag(ResultTestTags.PRIZE_DESCRIPTION).assertIsDisplayed()
    }

    @Test
    fun descriptionIsHiddenWhenThePrizeHasNone() {
        show(ResultUiState(loaded = true, prize = ResultPrize("Gloves", null, null)))

        composeRule.onNodeWithTag(ResultTestTags.PRIZE_NAME).assertIsDisplayed()
        composeRule.onNodeWithTag(ResultTestTags.PRIZE_DESCRIPTION).assertDoesNotExist()
    }

    @Test
    fun continueButtonInvokesTheCallback() {
        var continued = 0
        show(ResultUiState(loaded = true, prize = ResultPrize("Gloves", null, null)), onContinue = { continued++ })

        composeRule.onNodeWithTag(ResultTestTags.CONTINUE_BUTTON).performClick()
        assertEquals(1, continued)
    }

    @Test
    fun continueIgnoresTapsUntilTheEntryAnimationHasPlayed() {
        var continued = 0
        composeRule.mainClock.autoAdvance = false
        show(ResultUiState(loaded = true, prize = ResultPrize("Gloves", null, null)), onContinue = { continued++ })

        composeRule.mainClock.advanceTimeBy(500)
        composeRule.onNodeWithTag(ResultTestTags.CONTINUE_BUTTON).performClick()
        assertEquals(0, continued)

        composeRule.mainClock.advanceTimeBy(2_000)
        composeRule.onNodeWithTag(ResultTestTags.CONTINUE_BUTTON).performClick()
        assertEquals(1, continued)
    }

    @Test
    fun customTitlesFromBrandingAreShown() {
        show(ResultUiState(loaded = true, congratsTitle = "WELL DONE", congratsMessage = "ENJOY", prize = null))

        composeRule.onNodeWithText("WELL DONE").assertIsDisplayed()
        composeRule.onNodeWithText("ENJOY").assertIsDisplayed()
    }
}
