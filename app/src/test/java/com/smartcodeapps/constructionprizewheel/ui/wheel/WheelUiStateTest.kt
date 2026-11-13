package com.smartcodeapps.constructionprizewheel.ui.wheel

import com.smartcodeapps.constructionprizewheel.data.Branding
import com.smartcodeapps.constructionprizewheel.domain.Prize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WheelUiStateTest {

    private fun prize(id: Long, active: Boolean = true, order: Int? = id.toInt()) =
        Prize(id, "Prize $id", null, "/img/$id.jpg", 0, active, if (active) order else null)

    @Test
    fun `initial state cannot spin`() {
        assertFalse(WheelUiState().canSpin)
    }

    @Test
    fun `segments are always eight with active prizes spread in wheel order and busts between`() {
        val prizes = listOf(prize(1, order = 3), prize(2, active = false), prize(3, order = 1), prize(4, order = 2))
        val state = buildWheelUiState(prizes, Branding())
        assertEquals(listOf(3L, null, 4L, null, null, 1L, null, null), state.segments.map { it.prizeId })
        assertEquals(5, state.segments.count { it.isBust })
    }

    @Test
    fun `wheel with active prizes can spin`() {
        assertTrue(buildWheelUiState(listOf(prize(1)), Branding()).canSpin)
    }

    @Test
    fun `no active prizes gives eight bust segments and can still spin`() {
        val state = buildWheelUiState(listOf(prize(1, active = false)), Branding())
        assertEquals(List(8) { WheelSegment.Bust }, state.segments)
        assertTrue(state.canSpin)
    }

    @Test
    fun `spinning disables the spin button`() {
        val state = buildWheelUiState(listOf(prize(1)), Branding(), SpinState(busy = true))
        assertTrue(state.spinning)
        assertFalse(state.canSpin)
    }

    @Test
    fun `resting rotation and plan are passed through`() {
        val plan = SpinPlan(winnerPrizeId = 1, winnerIndex = 0, targetRotation = 1900f)
        val state = buildWheelUiState(listOf(prize(1)), Branding(), SpinState(restingRotation = 40f, plan = plan))
        assertEquals(40f, state.restingRotation, 0.001f)
        assertEquals(plan, state.spinPlan)
    }

    @Test
    fun `branding is passed through`() {
        val branding = Branding(mainTitle = "HELLO")
        assertEquals("HELLO", buildWheelUiState(emptyList(), branding).branding.mainTitle)
    }
}
