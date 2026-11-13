package com.smartcodeapps.constructionprizewheel.ui.admin.history

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.smartcodeapps.constructionprizewheel.ui.theme.ConstructionPrizeWheelTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class HistoryScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val changes = mutableListOf<Pair<Long, Boolean>>()
    private var backs = 0

    private val entries = listOf(
        HistoryEntry(2, "Hammer", "13 Nov 2026", "14:05:09", redeemed = false),
        HistoryEntry(1, "Gloves", "13 Nov 2026", "10:00:00", redeemed = true)
    )

    private fun show(state: HistoryUiState) {
        composeRule.setContent {
            ConstructionPrizeWheelTheme {
                HistoryContent(
                    state = state,
                    onRedeemedChange = { id, redeemed -> changes += id to redeemed },
                    onBack = { backs++ }
                )
            }
        }
    }

    @Test
    fun showsEveryEntryWithPrizeDateAndTime() {
        show(HistoryUiState(loaded = true, entries = entries))
        composeRule.onNodeWithText("Hammer").assertIsDisplayed()
        composeRule.onNodeWithText("13 Nov 2026  •  14:05:09").assertIsDisplayed()
        composeRule.onNodeWithText("Gloves").assertIsDisplayed()
        composeRule.onNodeWithText("13 Nov 2026  •  10:00:00").assertIsDisplayed()
    }

    @Test
    fun summaryShowsTotalAndNotRedeemed() {
        show(HistoryUiState(loaded = true, entries = entries))
        composeRule.onNodeWithTag(HistoryTestTags.SUMMARY).assertTextEquals("Total spins: 2  •  Not redeemed: 1")
    }

    @Test
    fun statusTextAndSwitchMatchTheRedemptionState() {
        show(HistoryUiState(loaded = true, entries = entries))
        composeRule.onNodeWithTag(HistoryTestTags.status(2)).assertTextEquals("Not redeemed")
        composeRule.onNodeWithTag(HistoryTestTags.redeemedSwitch(2)).assertIsOff()
        composeRule.onNodeWithTag(HistoryTestTags.status(1)).assertTextEquals("Redeemed")
        composeRule.onNodeWithTag(HistoryTestTags.redeemedSwitch(1)).assertIsOn()
    }

    @Test
    fun togglingTheSwitchReportsTheNewValueBothWays() {
        show(HistoryUiState(loaded = true, entries = entries))
        composeRule.onNodeWithTag(HistoryTestTags.redeemedSwitch(2)).performClick()
        composeRule.onNodeWithTag(HistoryTestTags.redeemedSwitch(1)).performClick()

        assertEquals(listOf(2L to true, 1L to false), changes)
    }

    @Test
    fun emptyHistoryShowsAMessage() {
        show(HistoryUiState(loaded = true))
        composeRule.onNodeWithTag(HistoryTestTags.EMPTY).assertIsDisplayed()
        composeRule.onNodeWithTag(HistoryTestTags.SUMMARY).assertTextEquals("Total spins: 0  •  Not redeemed: 0")
    }

    @Test
    fun noEmptyMessageBeforeDataLoads() {
        show(HistoryUiState(loaded = false))
        composeRule.onNodeWithTag(HistoryTestTags.EMPTY).assertDoesNotExist()
    }

    @Test
    fun backButtonInvokesItsCallback() {
        show(HistoryUiState(loaded = true, entries = entries))
        composeRule.onNodeWithTag(HistoryTestTags.BACK).performClick()
        assertEquals(1, backs)
    }
}
