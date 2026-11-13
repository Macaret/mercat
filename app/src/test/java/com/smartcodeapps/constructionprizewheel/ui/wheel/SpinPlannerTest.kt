package com.smartcodeapps.constructionprizewheel.ui.wheel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class SpinPlannerTest {

    private val startingRotations = listOf(0f, 37.5f, 180f, 359.9f, 1234.5f)

    @Test
    fun `wheel always stops on the winning segment`() {
        for (count in 1..8) for (winner in 0 until count) for (start in startingRotations) for (seed in 0 until 50) {
            val target = SpinPlanner.targetRotation(start, winner, count, Random(seed))
            assertEquals(
                "count=$count winner=$winner start=$start seed=$seed",
                winner,
                SpinPlanner.segmentUnderPointer(target, count)
            )
        }
    }

    @Test
    fun `landing point stays within the middle 60 percent of the segment`() {
        for (count in 1..8) for (winner in 0 until count) for (seed in 0 until 100) {
            val target = SpinPlanner.targetRotation(0f, winner, count, Random(seed))
            val position = SpinPlanner.positionInSegment(target, count)
            assertTrue("count=$count winner=$winner seed=$seed position=$position", position in 0.199f..0.801f)
        }
    }

    @Test
    fun `wheel spins at least five full rotations and less than six`() {
        for (start in startingRotations) for (seed in 0 until 50) {
            val turned = SpinPlanner.targetRotation(start, 2, 6, Random(seed)) - start
            assertTrue("start=$start seed=$seed turned=$turned", turned >= 5 * 360f && turned < 6 * 360f)
        }
    }

    @Test
    fun `landing offset varies between spins`() {
        val positions = (0 until 50).map {
            SpinPlanner.positionInSegment(SpinPlanner.targetRotation(0f, 1, 4, Random(it)), 4)
        }.toSet()
        assertTrue(positions.size > 10)
    }

    @Test
    fun `normalize maps any angle into zero to 360`() {
        assertEquals(0f, SpinPlanner.normalize(720f), 0.001f)
        assertEquals(350f, SpinPlanner.normalize(-10f), 0.001f)
        assertEquals(90f, SpinPlanner.normalize(450f), 0.001f)
    }
}
