package com.smartcodeapps.constructionprizewheel.ui.admin.prizes

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextReplacement
import com.smartcodeapps.constructionprizewheel.domain.ConfigError
import com.smartcodeapps.constructionprizewheel.domain.Prize
import com.smartcodeapps.constructionprizewheel.ui.admin.UnsavedChangesDialog
import com.smartcodeapps.constructionprizewheel.ui.admin.UnsavedDialogTestTags
import com.smartcodeapps.constructionprizewheel.ui.theme.ConstructionPrizeWheelTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
    private var noticesShown = 0
    private val probabilityChanges = mutableListOf<Pair<Long, Int>>()

    private fun show(state: PrizeEditorUiState) {
        composeRule.setContent {
            ConstructionPrizeWheelTheme {
                PrizeListContent(
                    state = state,
                    onAddPrize = { added++ },
                    onEditPrize = { edited += it },
                    onDeletePrize = { deleted += it },
                    onRearrange = {},
                    onBack = {},
                    onNoticeShown = { noticesShown++ },
                    onProbabilityChange = { id, value -> probabilityChanges += id to value }
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
        composeRule.onNodeWithTag(PrizeListTestTags.ACTIVE_HEADER).assertTextEquals("PE ROATĂ (2 din 8)")
        composeRule.onNodeWithTag(PrizeListTestTags.INACTIVE_HEADER).assertTextEquals("NU SUNT PE ROATĂ")
    }

    @Test
    fun everyRowShowsItsStoredProbabilityInAnEditableField() {
        show(state())
        composeRule.onNodeWithTag(PrizeListTestTags.probability(1)).assertTextContains("60")
        composeRule.onNodeWithTag(PrizeListTestTags.probability(2)).assertTextContains("40")
        composeRule.onNodeWithTag(PrizeListTestTags.probability(3)).assertTextContains("0")
        composeRule.onNodeWithText("Inactiv").assertIsDisplayed()
    }

    @Test
    fun donePressedOnTheKeyboardCommitsTheTypedProbability() {
        show(state())
        composeRule.onNodeWithTag(PrizeListTestTags.probability(1))
            .performTextReplacement("35")
            .performImeAction()
        assertEquals(listOf(1L to 35), probabilityChanges)
    }

    @Test
    fun typedProbabilityIsClampedTo100() {
        show(state())
        composeRule.onNodeWithTag(PrizeListTestTags.probability(2))
            .performTextReplacement("250")
            .performImeAction()
        assertEquals(listOf(2L to 100), probabilityChanges)
    }

    @Test
    fun anEmptyProbabilityCommitsAsZero() {
        show(state())
        composeRule.onNodeWithTag(PrizeListTestTags.probability(1))
            .performTextReplacement("")
            .performImeAction()
        assertEquals(listOf(1L to 0), probabilityChanges)
    }

    @Test
    fun anUnchangedProbabilityIsNotReported() {
        show(state())
        composeRule.onNodeWithTag(PrizeListTestTags.probability(1))
            .performTextReplacement("60")
            .performImeAction()
        assertTrue(probabilityChanges.isEmpty())
    }

    @Test
    fun nonDigitsAreIgnoredWhileTyping() {
        show(state())
        composeRule.onNodeWithTag(PrizeListTestTags.probability(1))
            .performTextReplacement("4a5")
            .performImeAction()
        assertEquals(listOf(1L to 45), probabilityChanges)
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
    fun summaryShowsActiveCountPrizeTotalAndBustChance() {
        show(state().copy(activeProbabilityTotal = 40))
        composeRule.onNodeWithTag(PrizeListTestTags.SUMMARY)
            .assertTextEquals("Active: 2 din 8  •  Premii: 40%  •  Fără câștig: 60%")
        composeRule.onNodeWithTag(PrizeListTestTags.BUST_WARNING).assertDoesNotExist()
    }

    @Test
    fun warnsWhenBustSlotsCanNeverBeLandedOn() {
        show(state())
        composeRule.onNodeWithTag(PrizeListTestTags.SUMMARY)
            .assertTextEquals("Active: 2 din 8  •  Premii: 100%  •  Fără câștig: 0%")
        composeRule.onNodeWithText("Premiile însumează 100%, deci cele 6 locuri fără câștig de pe roată nu vor fi niciodată alese.")
            .assertIsDisplayed()
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
    fun validationErrorsAreShownTogether() {
        show(
            state(
                dirty = true,
                errors = listOf(ConfigError.ProbabilityTotalOver100(120), ConfigError.ActiveMissingImage(1))
            )
        )
        composeRule.onNodeWithTag(PrizeListTestTags.ERRORS).assertIsDisplayed()
        composeRule.onNodeWithText("Probabilitățile premiilor active nu pot depăși 100% în total (acum 120%).")
            .assertIsDisplayed()
        composeRule.onNodeWithText("Hammer este activ, dar nu are imagine.").assertIsDisplayed()
    }

    @Test
    fun noErrorsCardWhenThereAreNoErrors() {
        show(state(dirty = true))
        composeRule.onNodeWithTag(PrizeListTestTags.ERRORS).assertDoesNotExist()
    }

    @Test
    fun pushedOutNoticeIsShownAndThenAcknowledged() {
        show(state(dirty = true, pushedOut = "Hammer"))
        composeRule.onNodeWithText("Hammer a fost mutat în secțiunea „Nu sunt pe roată” (maximum 8 premii).").assertIsDisplayed()
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
