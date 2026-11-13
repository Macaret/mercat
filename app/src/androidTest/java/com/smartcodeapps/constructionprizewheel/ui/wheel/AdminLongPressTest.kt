package com.smartcodeapps.constructionprizewheel.ui.wheel

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.down
import androidx.compose.ui.test.up
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AdminLongPressTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var triggered = 0

    private fun show(enabled: Boolean = true) {
        triggered = 0
        composeRule.setContent {
            Box(
                Modifier
                    .size(100.dp)
                    .testTag("target")
                    .adminLongPress(enabled = enabled) { triggered++ }
            )
        }
    }

    @Test
    fun holdingForThreeSecondsTriggersOnce() {
        show()
        composeRule.onNodeWithTag("target").performTouchInput { down(center) }
        composeRule.mainClock.advanceTimeBy(3_100)
        composeRule.onNodeWithTag("target").performTouchInput { up() }

        assertEquals(1, triggered)
    }

    @Test
    fun releasingAfterOneSecondDoesNothing() {
        show()
        composeRule.onNodeWithTag("target").performTouchInput { down(center) }
        composeRule.mainClock.advanceTimeBy(1_000)
        composeRule.onNodeWithTag("target").performTouchInput { up() }
        composeRule.mainClock.advanceTimeBy(5_000)

        assertEquals(0, triggered)
    }

    @Test
    fun holdingJustUnderThreeSecondsDoesNothing() {
        show()
        composeRule.onNodeWithTag("target").performTouchInput { down(center) }
        composeRule.mainClock.advanceTimeBy(2_800)
        composeRule.onNodeWithTag("target").performTouchInput { up() }

        assertEquals(0, triggered)
    }

    @Test
    fun disabledGestureNeverTriggers() {
        show(enabled = false)
        composeRule.onNodeWithTag("target").performTouchInput { down(center) }
        composeRule.mainClock.advanceTimeBy(4_000)
        composeRule.onNodeWithTag("target").performTouchInput { up() }

        assertEquals(0, triggered)
    }
}
