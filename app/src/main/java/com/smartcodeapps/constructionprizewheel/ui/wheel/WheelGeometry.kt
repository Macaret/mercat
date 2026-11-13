package com.smartcodeapps.constructionprizewheel.ui.wheel

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** Angles are in degrees, measured clockwise from the top (the pointer position). */
object WheelGeometry {

    fun segmentAngle(count: Int): Float = 360f / count

    fun segmentStart(index: Int, count: Int): Float = index * segmentAngle(count)

    fun segmentCenter(index: Int, count: Int): Float = segmentStart(index, count) + segmentAngle(count) / 2f

    fun pointAt(centerX: Float, centerY: Float, radius: Float, angleFromTop: Float): Pair<Float, Float> {
        val radians = angleFromTop * PI.toFloat() / 180f
        return (centerX + radius * sin(radians)) to (centerY - radius * cos(radians))
    }

    fun imageSize(radius: Float, count: Int): Float {
        if (count == 1) return radius * 0.5f
        val halfAngle = segmentAngle(count) / 2f * PI.toFloat() / 180f
        val chordAtImage = 2f * radius * IMAGE_DISTANCE * sin(halfAngle)
        return min(radius * 0.50f, chordAtImage * 0.95f)
    }

    fun colorIndex(index: Int, count: Int): Int =
        if (count > 1 && count % 2 == 1 && index == count - 1) 2 else index % 2

    const val IMAGE_DISTANCE = 0.62f
}
