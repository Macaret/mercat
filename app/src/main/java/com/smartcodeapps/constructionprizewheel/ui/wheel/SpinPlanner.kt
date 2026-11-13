package com.smartcodeapps.constructionprizewheel.ui.wheel

import kotlin.random.Random

/** [winnerPrizeId] is null when the spin lands on a bust slot. [winnerIndex] is the slot index. */
data class SpinPlan(val winnerPrizeId: Long?, val winnerIndex: Int, val targetRotation: Float)

/** Rotation is in degrees clockwise. The pointer is fixed at the top, at wheel angle 0. */
object SpinPlanner {
    const val FULL_ROTATIONS = 5
    const val DURATION_MS = 4_000
    const val LANDING_FRACTION = 0.6f

    fun targetRotation(currentRotation: Float, winnerIndex: Int, count: Int, random: Random): Float {
        val segmentAngle = WheelGeometry.segmentAngle(count)
        val offsetInSegment = (random.nextFloat() - 0.5f) * LANDING_FRACTION * segmentAngle
        val landingAngle = WheelGeometry.segmentCenter(winnerIndex, count) + offsetInSegment

        val requiredRotation = normalize(-landingAngle)
        var extra = requiredRotation - normalize(currentRotation)
        if (extra < 0f) extra += 360f
        return currentRotation + FULL_ROTATIONS * 360f + extra
    }

    fun segmentUnderPointer(rotation: Float, count: Int): Int {
        val wheelAngleAtPointer = normalize(-rotation)
        return (wheelAngleAtPointer / WheelGeometry.segmentAngle(count)).toInt().coerceIn(0, count - 1)
    }

    fun positionInSegment(rotation: Float, count: Int): Float {
        val segmentAngle = WheelGeometry.segmentAngle(count)
        val wheelAngleAtPointer = normalize(-rotation)
        return (wheelAngleAtPointer % segmentAngle) / segmentAngle
    }

    fun normalize(degrees: Float): Float = ((degrees % 360f) + 360f) % 360f
}
