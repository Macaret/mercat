package com.smartcodeapps.constructionprizewheel.ui.common

import androidx.compose.runtime.mutableFloatStateOf
import org.junit.Assert.assertEquals
import org.junit.Test

class ShineScheduleTest {

    private fun schedule(phase: Float) = ShineSchedule(mutableFloatStateOf(phase))

    @Test
    fun `the shine enters from the left, crosses the button and leaves on the right`() {
        assertEquals(-0.4f, shinePosition(0f), 0.001f)
        assertEquals(0.5f, shinePosition(0.1f), 0.001f)
        assertEquals(1.4f, shinePosition(0.2f), 0.001f)
    }

    @Test
    fun `the button shine is on at the start of a cycle`() {
        assertEquals(0.5f, schedule(0.1f).button, 0.001f)
    }

    @Test
    fun `the shine stays off for the rest of the cycle`() {
        assertEquals(SHINE_OFF, schedule(0.21f).button, 0f)
        assertEquals(SHINE_OFF, schedule(0.6f).button, 0f)
        assertEquals(SHINE_OFF, schedule(1f).button, 0f)
    }
}
