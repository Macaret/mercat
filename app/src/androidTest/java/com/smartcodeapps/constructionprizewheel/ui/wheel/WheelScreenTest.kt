package com.smartcodeapps.constructionprizewheel.ui.wheel

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.down
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.up
import com.smartcodeapps.constructionprizewheel.data.Branding
import com.smartcodeapps.constructionprizewheel.ui.theme.ConstructionPrizeWheelTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class WheelScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun show(state: WheelUiState, onSpin: () -> Unit = {}) {
        composeRule.setContent {
            ConstructionPrizeWheelTheme {
                WheelContent(state = state, rotationDegrees = { 0f }, onSpin = onSpin)
            }
        }
    }

    private fun segments(count: Int) = List(count) { WheelSegment(it.toLong(), "Prize $it", null) }

    @Test
    fun spinButtonIsEnabledAndClickableWithActivePrizes() {
        var spins = 0
        show(WheelUiState(loaded = true, segments = segments(4)), onSpin = { spins++ })

        composeRule.onNodeWithTag(WheelTestTags.SPIN_BUTTON).assertIsEnabled().performClick()
        assertEquals(1, spins)
    }

    @Test
    fun spinButtonIsDisabledAndMessageShownWithNoActivePrizes() {
        show(WheelUiState(loaded = true, segments = emptyList()))

        composeRule.onNodeWithTag(WheelTestTags.SPIN_BUTTON).assertIsNotEnabled()
        composeRule.onNodeWithTag(WheelTestTags.NO_PRIZES_MESSAGE).assertIsDisplayed()
    }

    @Test
    fun holdingTheLogoForThreeSecondsRequestsAdminAccess() {
        var requested = 0
        composeRule.setContent {
            ConstructionPrizeWheelTheme {
                WheelContent(
                    state = WheelUiState(loaded = true, segments = segments(3)),
                    rotationDegrees = { 0f },
                    onSpin = {},
                    onAdminRequested = { requested++ }
                )
            }
        }

        composeRule.onNodeWithTag(WheelTestTags.LOGO).performTouchInput { down(center) }
        composeRule.mainClock.advanceTimeBy(3_100)
        composeRule.onNodeWithTag(WheelTestTags.LOGO).performTouchInput { up() }

        assertEquals(1, requested)
    }

    @Test
    fun tappingTheLogoBrieflyDoesNotRequestAdminAccess() {
        var requested = 0
        composeRule.setContent {
            ConstructionPrizeWheelTheme {
                WheelContent(
                    state = WheelUiState(loaded = true, segments = segments(3)),
                    rotationDegrees = { 0f },
                    onSpin = {},
                    onAdminRequested = { requested++ }
                )
            }
        }

        composeRule.onNodeWithTag(WheelTestTags.LOGO).performClick()
        composeRule.mainClock.advanceTimeBy(5_000)

        assertEquals(0, requested)
    }

    @Test
    fun adminGestureIsIgnoredWhileTheWheelIsSpinning() {
        var requested = 0
        composeRule.setContent {
            ConstructionPrizeWheelTheme {
                WheelContent(
                    state = WheelUiState(loaded = true, segments = segments(3), spinning = true),
                    rotationDegrees = { 0f },
                    onSpin = {},
                    onAdminRequested = { requested++ }
                )
            }
        }

        composeRule.onNodeWithTag(WheelTestTags.LOGO).performTouchInput { down(center) }
        composeRule.mainClock.advanceTimeBy(3_100)
        composeRule.onNodeWithTag(WheelTestTags.LOGO).performTouchInput { up() }

        assertEquals(0, requested)
    }

    @Test
    fun noMessageIsShownBeforeDataLoads() {
        show(WheelUiState(loaded = false))

        composeRule.onNodeWithTag(WheelTestTags.NO_PRIZES_MESSAGE).assertDoesNotExist()
    }
}
