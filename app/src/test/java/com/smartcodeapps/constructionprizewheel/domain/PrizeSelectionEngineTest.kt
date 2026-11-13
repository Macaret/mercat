package com.smartcodeapps.constructionprizewheel.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.random.Random

class PrizeSelectionEngineTest {

    /** Returns the given values in order from nextInt, checking each is within the requested bound. */
    private class SequenceRandom(vararg values: Int) : Random() {
        private val queue = ArrayDeque(values.toList())
        override fun nextBits(bitCount: Int) = 0
        override fun nextInt(until: Int): Int {
            val value = queue.removeFirst()
            require(value in 0 until until) { "draw $value is outside 0 until $until" }
            return value
        }
    }

    private fun prize(id: Long, probability: Int, active: Boolean = true) =
        Prize(id, "Prize $id", null, "img/$id.jpg", probability, active, if (active) id.toInt() else null)

    private fun select(prizes: List<Prize>, vararg draws: Int) =
        PrizeSelectionEngine(SequenceRandom(*draws)).select(prizes)

    // Three prizes sit in slots 0, 2 and 5; slots 1, 3, 4, 6 and 7 are busts.
    private val threePrizes = listOf(prize(1, 20), prize(2, 50), prize(3, 30))

    @Test
    fun `draws map to prizes in wheel order and land on their slot`() {
        assertEquals(SpinOutcome.Win(threePrizes[0], 0), select(threePrizes, 0))
        assertEquals(SpinOutcome.Win(threePrizes[0], 0), select(threePrizes, 19))
        assertEquals(SpinOutcome.Win(threePrizes[1], 2), select(threePrizes, 20))
        assertEquals(SpinOutcome.Win(threePrizes[1], 2), select(threePrizes, 69))
        assertEquals(SpinOutcome.Win(threePrizes[2], 5), select(threePrizes, 70))
        assertEquals(SpinOutcome.Win(threePrizes[2], 5), select(threePrizes, 99))
    }

    @Test
    fun `draws above the prize total are busts on a randomly chosen bust slot`() {
        // Two prizes at 20% + 30% sit in slots 0 and 4; bust slots are 1, 2, 3, 5, 6, 7.
        val prizes = listOf(prize(1, 20), prize(2, 30))
        assertEquals(SpinOutcome.Win(prizes[1], 4), select(prizes, 49))
        assertEquals(SpinOutcome.Bust(1), select(prizes, 50, 0))
        assertEquals(SpinOutcome.Bust(5), select(prizes, 99, 3))
        assertEquals(SpinOutcome.Bust(7), select(prizes, 75, 5))
    }

    @Test
    fun `no prizes always busts`() {
        val engine = PrizeSelectionEngine(Random(1))
        repeat(1000) { assertTrue(engine.select(emptyList()) is SpinOutcome.Bust) }
    }

    @Test
    fun `a full wheel totalling 100 never busts`() {
        val prizes = (1L..8L).map { prize(it, if (it <= 4) 20 else 5) }
        val engine = PrizeSelectionEngine(Random(5))
        repeat(10_000) { assertTrue(engine.select(prizes) is SpinOutcome.Win) }
    }

    @Test
    fun `inactive prizes are never selected`() {
        val mixed = listOf(prize(1, 100), prize(2, 100, active = false))
        val engine = PrizeSelectionEngine(Random(9))
        repeat(1000) { assertEquals(SpinOutcome.Win(mixed[0], 0), engine.select(mixed)) }
    }

    @Test
    fun `zero percent prize is never selected`() {
        val withZero = listOf(prize(1, 0), prize(2, 40))
        val engine = PrizeSelectionEngine(Random(7))
        repeat(10_000) {
            val outcome = engine.select(withZero)
            assertTrue(outcome !is SpinOutcome.Win || outcome.prize.id == 2L)
        }
    }

    @Test
    fun `an invalid full wheel of zero percent prizes still lands on a prize`() {
        val prizes = (1L..8L).map { prize(it, 0) }
        assertEquals(SpinOutcome.Win(prizes[3], 3), select(prizes, 3))
    }

    @Test
    fun `frequencies match prize probabilities and busts share the rest equally`() {
        val prizes = listOf(prize(1, 10), prize(2, 20)) // slots 0 and 4, bust 70% over 6 slots
        val engine = PrizeSelectionEngine(Random(42))
        val samples = 120_000
        val bySlot = IntArray(WHEEL_SLOTS)
        repeat(samples) { bySlot[engine.select(prizes).slotIndex]++ }

        fun assertPercent(expected: Double, slot: Int) {
            val observed = bySlot[slot] * 100.0 / samples
            assertTrue("slot $slot: expected $expected% but observed $observed%", abs(observed - expected) < 1.0)
        }
        assertPercent(10.0, 0)
        assertPercent(20.0, 4)
        listOf(1, 2, 3, 5, 6, 7).forEach { assertPercent(70.0 / 6, it) }
    }
}
