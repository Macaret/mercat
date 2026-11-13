package com.smartcodeapps.constructionprizewheel.ui.admin.prizes

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.smartcodeapps.constructionprizewheel.domain.ConfigError
import com.smartcodeapps.constructionprizewheel.domain.Prize
import com.smartcodeapps.constructionprizewheel.ui.admin.UnsavedChangesDialog
import com.smartcodeapps.constructionprizewheel.ui.admin.UnsavedDialogTestTags
import com.smartcodeapps.constructionprizewheel.ui.theme.ConstructionPrizeWheelTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PrizeListScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val prizes = listOf(
        Prize(1, "Hammer", null, null, 60, true, 1),
        Prize(2, "Gloves", null, null, 40, true, 2),
        Prize(3, "Drill", null, null, 0, false, null)
    )

    private var added = 0
    private val edited = mutableListOf<Long>()
    private val deleted = mutableListOf<Long>()
    private var saves = 0
    private var discards = 0
    private var noticesShown = 0

    private fun show(state: PrizeEditorUiState) {
        composeRule.setContent {
            ConstructionPrizeWheelTheme {
                PrizeListContent(
                    state = state,
                    onAddPrize = { added++ },
                    onEditPrize = { edited += it },
                    onDeletePrize = { deleted += it },
                    onRearrange = {},
                    onSave = { saves++ },
                    onDiscard = { discards++ },
                    onBack = {},
                    onNoticeShown = { noticesShown++ }
                )
            }
        }
    }

    private fun state(
        prizes: List<Prize> = this.prizes,
        dirty: Boolean = false,
        errors: List<ConfigError> = emptyList(),
        pushedOut: String? = null
    ) = PrizeEditorUiState(
        loaded = true,
        prizes = prizes,
        dirty = dirty,
        errors = errors,
        activeCount = prizes.count { it.active },
        activeProbabilityTotal = prizes.filter { it.active }.sumOf { it.probability },
        pushedOutPrizeName = pushedOut
    )

    @Test
    fun listsEveryPrizeUnderItsSection() {
        show(state())
        composeRule.onNodeWithText("Hammer").assertIsDisplayed()
        composeRule.onNodeWithText("Gloves").assertIsDisplayed()
        composeRule.onNodeWithText("Drill").assertIsDisplayed()
    }

    @Test
    fun bothSectionHeadersAreShown() {
        show(state())
        composeRule.onNodeWithTag(PrizeListTestTags.ACTIVE_HEADER).assertTextEquals("ON THE WHEEL (2 of 8)")
        composeRule.onNodeWithTag(PrizeListTestTags.INACTIVE_HEADER).assertTextEquals("NOT ON THE WHEEL")
    }

    @Test
    fun inactiveRowsShowTheirStoredProbability() {
        show(state())
        composeRule.onNodeWithText("Inactive  •  0%").assertIsDisplayed()
        composeRule.onNodeWithText("Active  •  60%").assertIsDisplayed()
    }

    @Test
    fun everyPrizeHasADragHandle() {
        show(state())
        prizes.forEach { composeRule.onNodeWithTag(PrizeListTestTags.handle(it.id)).assertIsDisplayed() }
    }

    @Test
    fun emptyActiveSectionShowsADropHint() {
        show(state(prizes = listOf(prizes[2])))
        composeRule.onNodeWithTag(PrizeListTestTags.ACTIVE_HINT).assertIsDisplayed()
        composeRule.onNodeWithTag(PrizeListTestTags.INACTIVE_HINT).assertDoesNotExist()
    }

    @Test
    fun emptyInactiveSectionShowsADropHint() {
        show(state(prizes = prizes.take(2)))
        composeRule.onNodeWithTag(PrizeListTestTags.INACTIVE_HINT).assertIsDisplayed()
        composeRule.onNodeWithTag(PrizeListTestTags.ACTIVE_HINT).assertDoesNotExist()
    }

    @Test
    fun noHintsWhenBothSectionsHavePrizes() {
        show(state())
        composeRule.onNodeWithTag(PrizeListTestTags.ACTIVE_HINT).assertDoesNotExist()
        composeRule.onNodeWithTag(PrizeListTestTags.INACTIVE_HINT).assertDoesNotExist()
    }

    @Test
    fun summaryShowsActiveCountAndTotal() {
        show(state())
        composeRule.onNodeWithText("Active: 2 of 8", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Total probability: 100%", substring = true).assertIsDisplayed()
    }

    @Test
    fun editAndDeleteButtonsReportThePrizeId() {
        show(state())
        composeRule.onNodeWithTag(PrizeListTestTags.edit(2)).performClick()
        composeRule.onNodeWithTag(PrizeListTestTags.delete(3)).performClick()

        assertEquals(listOf(2L), edited)
        assertEquals(listOf(3L), deleted)
    }

    @Test
    fun tappingARowDoesNotOpenTheEditor() {
        show(state())
        composeRule.onNodeWithTag(PrizeListTestTags.row(2)).performClick()
        assertEquals(emptyList<Long>(), edited)
    }

    @Test
    fun addButtonInvokesItsCallback() {
        show(state())
        composeRule.onNodeWithTag(PrizeListTestTags.ADD).performClick()
        assertEquals(1, added)
    }

    @Test
    fun saveAndDiscardAreDisabledWhenNothingChanged() {
        show(state(dirty = false))
        composeRule.onNodeWithTag(PrizeListTestTags.SAVE).assertIsNotEnabled()
        composeRule.onNodeWithTag(PrizeListTestTags.DISCARD).assertIsNotEnabled()
    }

    @Test
    fun saveAndDiscardWorkWhenDirty() {
        show(state(dirty = true))
        composeRule.onNodeWithTag(PrizeListTestTags.SAVE).assertIsEnabled().performClick()
        composeRule.onNodeWithTag(PrizeListTestTags.DISCARD).assertIsEnabled().performClick()

        assertEquals(1, saves)
        assertEquals(1, discards)
    }

    @Test
    fun validationErrorsAreShownTogether() {
        show(
            state(
                dirty = true,
                errors = listOf(ConfigError.ProbabilityTotalInvalid(90), ConfigError.ActiveMissingImage(1))
            )
        )
        composeRule.onNodeWithTag(PrizeListTestTags.ERRORS).assertIsDisplayed()
        composeRule.onNodeWithText("Active prize probabilities must total 100% (currently 90%).").assertIsDisplayed()
        composeRule.onNodeWithText("Hammer is active but has no image.").assertIsDisplayed()
    }

    @Test
    fun noErrorsCardWhenThereAreNoErrors() {
        show(state(dirty = true))
        composeRule.onNodeWithTag(PrizeListTestTags.ERRORS).assertDoesNotExist()
    }

    @Test
    fun pushedOutNoticeIsShownAndThenAcknowledged() {
        show(state(dirty = true, pushedOut = "Hammer"))
        composeRule.onNodeWithText("Hammer moved to Not on the wheel (maximum 8 prizes).").assertIsDisplayed()
        composeRule.waitForIdle()
        assertEquals(1, noticesShown)
    }

    @Test
    fun noNoticeWhenNothingWasPushedOut() {
        show(state(dirty = true))
        assertEquals(0, noticesShown)
    }

    @Test
    fun emptyListShowsAHint() {
        show(PrizeEditorUiState(loaded = true))
        composeRule.onNodeWithTag(PrizeListTestTags.EMPTY).assertIsDisplayed()
    }

    @Test
    fun unsavedChangesDialogOffersDiscardAndKeepEditing() {
        var discarded = 0
        var kept = 0
        composeRule.setContent {
            ConstructionPrizeWheelTheme {
                UnsavedChangesDialog(onDiscard = { discarded++ }, onKeepEditing = { kept++ })
            }
        }
        composeRule.onNodeWithTag(UnsavedDialogTestTags.KEEP_EDITING).performClick()
        composeRule.onNodeWithTag(UnsavedDialogTestTags.DISCARD).performClick()

        assertEquals(1, kept)
        assertEquals(1, discarded)
    }
}
