package com.smartcodeapps.constructionprizewheel.ui.admin.history

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.smartcodeapps.constructionprizewheel.ui.theme.ConstructionPrizeWheelTheme
import org.junit.Rule
import org.junit.Test

class HistoryScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val entries = listOf(
        HistoryEntry(2, "Hammer", "13 Nov 2026", "14:05:09"),
        HistoryEntry(1, "Gloves", "13 Nov 2026", "10:00:00")
    )

    private val totals = listOf(PrizeTotal(10, "Hammer", 2), PrizeTotal(20, "Gloves", 1))

    private fun show(state: HistoryUiState) {
        composeRule.setContent {
            ConstructionPrizeWheelTheme {
                HistoryContent(state = state)
            }
        }
    }

    @Test
    fun showsEveryEntryWithPrizeDateAndTime() {
        show(HistoryUiState(loaded = true, entries = entries, prizeTotals = totals))
        composeRule.onNodeWithText("Hammer").assertIsDisplayed()
        composeRule.onNodeWithText("13 Nov 2026  •  14:05:09").assertIsDisplayed()
        composeRule.onNodeWithText("Gloves").assertIsDisplayed()
        composeRule.onNodeWithText("13 Nov 2026  •  10:00:00").assertIsDisplayed()
    }

    @Test
    fun summaryShowsTotal() {
        show(HistoryUiState(loaded = true, entries = entries, prizeTotals = totals))
        composeRule.onNodeWithTag(HistoryTestTags.SUMMARY).assertTextEquals("Total rotiri: 2")
    }

    @Test
    fun emptyHistoryShowsAMessage() {
        show(HistoryUiState(loaded = true))
        composeRule.onNodeWithTag(HistoryTestTags.EMPTY).assertIsDisplayed()
        composeRule.onNodeWithTag(HistoryTestTags.SUMMARY).assertTextEquals("Total rotiri: 0")
    }

    @Test
    fun noEmptyMessageBeforeDataLoads() {
        show(HistoryUiState(loaded = false))
        composeRule.onNodeWithTag(HistoryTestTags.EMPTY).assertDoesNotExist()
    }

    @Test
    fun byPrizeTabShowsTheCountForEachPrize() {
        show(HistoryUiState(loaded = true, entries = entries, prizeTotals = totals))
        composeRule.onNodeWithTag(HistoryTestTags.prizeRow(10)).assertDoesNotExist()

        composeRule.onNodeWithTag(HistoryTestTags.TAB_BY_PRIZE).performClick()

        composeRule.onNodeWithTag(HistoryTestTags.prizeCount(10)).assertTextEquals("2")
        composeRule.onNodeWithTag(HistoryTestTags.prizeCount(20)).assertTextEquals("1")
        composeRule.onNodeWithTag(HistoryTestTags.row(2)).assertDoesNotExist()
    }

    @Test
    fun switchingBackToTimelineShowsTheSpinsAgain() {
        show(HistoryUiState(loaded = true, entries = entries, prizeTotals = totals))
        composeRule.onNodeWithTag(HistoryTestTags.TAB_BY_PRIZE).performClick()
        composeRule.onNodeWithTag(HistoryTestTags.TAB_TIMELINE).performClick()

        composeRule.onNodeWithTag(HistoryTestTags.row(2)).assertIsDisplayed()
        composeRule.onNodeWithTag(HistoryTestTags.prizeRow(10)).assertDoesNotExist()
    }
}
