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
    fun `initial state cannot spin and shows no message`() {
        val state = WheelUiState()
        assertFalse(state.canSpin)
        assertFalse(state.showNoPrizesMessage)
    }

    @Test
    fun `segments are the active prizes in wheel order`() {
        val prizes = listOf(prize(1, order = 3), prize(2, active = false), prize(3, order = 1), prize(4, order = 2))
        val state = buildWheelUiState(prizes, Branding())
        assertEquals(listOf(3L, 4L, 1L), state.segments.map { it.prizeId })
    }

    @Test
    fun `wheel with active prizes can spin`() {
        val state = buildWheelUiState(listOf(prize(1)), Branding())
        assertTrue(state.canSpin)
        assertFalse(state.showNoPrizesMessage)
    }

    @Test
    fun `no active prizes disables spin and shows the message`() {
        val state = buildWheelUiState(listOf(prize(1, active = false)), Branding())
        assertTrue(state.segments.isEmpty())
        assertFalse(state.canSpin)
        assertTrue(state.showNoPrizesMessage)
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
