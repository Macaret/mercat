package com.smartcodeapps.constructionprizewheel.ui.admin.prizes

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.smartcodeapps.constructionprizewheel.ui.theme.ConstructionPrizeWheelTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class EditPrizeScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var done = 0
    private var cancelled = 0
    private var chooseImage = 0

    private fun show(
        isNew: Boolean = true,
        name: String = "",
        imagePath: String? = null,
        imageError: Boolean = false,
        active: Boolean = false,
        canActivate: Boolean = true
    ) {
        composeRule.setContent {
            var nameState by remember { mutableStateOf(name) }
            var activeState by remember { mutableStateOf(active) }
            var probability by remember { mutableStateOf("") }
            ConstructionPrizeWheelTheme {
                EditPrizeContent(
                    isNew = isNew,
                    name = nameState, onNameChange = { nameState = it },
                    description = "", onDescriptionChange = {},
                    imagePath = imagePath, imageError = imageError, onChooseImage = { chooseImage++ },
                    probability = probability, onProbabilityChange = { probability = sanitizeProbability(it) },
                    active = activeState, onActiveChange = { activeState = it },
                    canActivate = canActivate,
                    onDone = { done++ }, onCancel = { cancelled++ }
                )
            }
        }
    }

    @Test
    fun titleDependsOnWhetherThePrizeIsNew() {
        show(isNew = true)
        composeRule.onNodeWithTag(EditPrizeTestTags.TITLE).assertTextEquals("NEW PRIZE")
    }

    @Test
    fun titleForAnExistingPrize() {
        show(isNew = false, name = "Hammer")
        composeRule.onNodeWithTag(EditPrizeTestTags.TITLE).assertTextEquals("EDIT PRIZE")
    }

    @Test
    fun doneIsDisabledUntilTheNameIsFilled() {
        show(name = "")
        composeRule.onNodeWithTag(EditPrizeTestTags.DONE).assertIsNotEnabled()

        composeRule.onNodeWithTag(EditPrizeTestTags.NAME).performTextInput("Hammer")
        composeRule.onNodeWithTag(EditPrizeTestTags.DONE).assertIsEnabled().performClick()
        assertEquals(1, done)
    }

    @Test
    fun cancelAndChooseImageInvokeTheirCallbacks() {
        show(name = "Hammer")
        composeRule.onNodeWithTag(EditPrizeTestTags.CANCEL).performClick()
        composeRule.onNodeWithTag(EditPrizeTestTags.CHOOSE_IMAGE).performClick()

        assertEquals(1, cancelled)
        assertEquals(1, chooseImage)
    }

    @Test
    fun probabilityFieldAcceptsDigitsOnly() {
        show(name = "Hammer")
        composeRule.onNodeWithTag(EditPrizeTestTags.PROBABILITY).performTextInput("2a5b")
        composeRule.onNodeWithTag(EditPrizeTestTags.PROBABILITY).assertTextContains("25")
    }

    @Test
    fun imageErrorIsShownWhenLoadingFails() {
        show(imageError = true)
        composeRule.onNodeWithTag(EditPrizeTestTags.IMAGE_ERROR).assertIsDisplayed()
    }

    @Test
    fun activeSwitchIsDisabledWithAHintWhenTheLimitIsReached() {
        show(active = false, canActivate = false)
        composeRule.onNodeWithTag(EditPrizeTestTags.ACTIVE).assertIsNotEnabled()
        composeRule.onNodeWithTag(EditPrizeTestTags.ACTIVE_HINT).assertIsDisplayed()
    }

    @Test
    fun activeSwitchIsEnabledWhenAnotherPrizeCanBeActivated() {
        show(canActivate = true)
        composeRule.onNodeWithTag(EditPrizeTestTags.ACTIVE).assertIsEnabled()
        composeRule.onNodeWithTag(EditPrizeTestTags.ACTIVE_HINT).assertDoesNotExist()
    }
}
