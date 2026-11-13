package com.smartcodeapps.constructionprizewheel.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class PrizeSelectionEngineTest {

    private class FixedRandom(private val value: Int) : Random() {
        override fun nextBits(bitCount: Int) = 0
        override fun nextInt(until: Int) = value
    }

    private fun prize(id: Long, probability: Int, active: Boolean = true) =
        Prize(id, "Prize $id", null, "img/$id.jpg", probability, active, if (active) id.toInt() else null)

    private val prizes = listOf(prize(1, 20), prize(2, 50), prize(3, 30))

    @Test
    fun `draw at the start of the range picks the first prize`() {
        assertEquals(1L, PrizeSelectionEngine(FixedRandom(0)).select(prizes)?.id)
    }

    @Test
    fun `draw at a boundary moves to the next prize`() {
        assertEquals(1L, PrizeSelectionEngine(FixedRandom(19)).select(prizes)?.id)
        assertEquals(2L, PrizeSelectionEngine(FixedRandom(20)).select(prizes)?.id)
        assertEquals(2L, PrizeSelectionEngine(FixedRandom(69)).select(prizes)?.id)
        assertEquals(3L, PrizeSelectionEngine(FixedRandom(70)).select(prizes)?.id)
    }

    @Test
    fun `draw at the end of the range picks the last prize`() {
        assertEquals(3L, PrizeSelectionEngine(FixedRandom(99)).select(prizes)?.id)
    }

    @Test
    fun `no prizes returns null`() {
        assertNull(PrizeSelectionEngine(Random(1)).select(emptyList()))
    }

    @Test
    fun `inactive prizes are never selected`() {
        val mixed = listOf(prize(1, 100), prize(2, 100, active = false))
        repeat(1000) {
            assertEquals(1L, PrizeSelectionEngine(Random(it)).select(mixed)?.id)
        }
    }

    @Test
    fun `zero percent prize is never selected`() {
        val withZero = listOf(prize(1, 0), prize(2, 100))
        val engine = PrizeSelectionEngine(Random(7))
        repeat(10_000) { assertEquals(2L, engine.select(withZero)?.id) }
    }

    @Test
    fun `all zero probabilities returns null`() {
        assertNull(PrizeSelectionEngine(Random(1)).select(listOf(prize(1, 0), prize(2, 0))))
    }

    @Test
    fun `single prize always wins`() {
        val engine = PrizeSelectionEngine(Random(3))
        repeat(1000) { assertEquals(1L, engine.select(listOf(prize(1, 100)))?.id) }
    }

    @Test
    fun `selection frequencies match configured probabilities`() {
        val engine = PrizeSelectionEngine(Random(42))
        val samples = 100_000
        val counts = mutableMapOf<Long, Int>()
        repeat(samples) {
            val id = engine.select(prizes)!!.id
            counts[id] = (counts[id] ?: 0) + 1
        }
        prizes.forEach { prize ->
            val observed = counts.getValue(prize.id) * 100.0 / samples
            assertTrue(
                "Prize ${prize.id}: expected ${prize.probability}% but observed $observed%",
                kotlin.math.abs(observed - prize.probability) < 1.0
            )
        }
    }
}
