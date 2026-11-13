package com.smartcodeapps.constructionprizewheel.ui.wheel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WheelGeometryTest {

    @Test
    fun `segment angle is 360 divided by the segment count`() {
        mapOf(1 to 360f, 2 to 180f, 3 to 120f, 4 to 90f, 6 to 60f, 8 to 45f).forEach { (count, angle) ->
            assertEquals("count $count", angle, WheelGeometry.segmentAngle(count), 0.001f)
        }
    }

    @Test
    fun `segments tile the full circle without gaps`() {
        (1..8).forEach { count ->
            val angle = WheelGeometry.segmentAngle(count)
            (0 until count).forEach { index ->
                assertEquals(index * angle, WheelGeometry.segmentStart(index, count), 0.001f)
                assertEquals(
                    WheelGeometry.segmentStart(index, count) + angle / 2f,
                    WheelGeometry.segmentCenter(index, count),
                    0.001f
                )
            }
            assertEquals(360f, WheelGeometry.segmentStart(count - 1, count) + angle, 0.001f)
        }
    }

    @Test
    fun `angle zero is the top of the wheel and angles go clockwise`() {
        val top = WheelGeometry.pointAt(100f, 100f, 50f, 0f)
        assertEquals(100f, top.first, 0.01f)
        assertEquals(50f, top.second, 0.01f)

        val right = WheelGeometry.pointAt(100f, 100f, 50f, 90f)
        assertEquals(150f, right.first, 0.01f)
        assertEquals(100f, right.second, 0.01f)

        val bottom = WheelGeometry.pointAt(100f, 100f, 50f, 180f)
        assertEquals(100f, bottom.first, 0.01f)
        assertEquals(150f, bottom.second, 0.01f)
    }

    @Test
    fun `adjacent segments never share a color including the wrap around`() {
        (2..8).forEach { count ->
            (0 until count).forEach { index ->
                val next = (index + 1) % count
                assertNotEquals(
                    "count $count, segments $index and $next",
                    WheelGeometry.colorIndex(index, count),
                    WheelGeometry.colorIndex(next, count)
                )
            }
        }
    }

    @Test
    fun `prize images fit inside their segment`() {
        (1..8).forEach { count ->
            val radius = 400f
            val size = WheelGeometry.imageSize(radius, count)
            assertTrue("count $count: size $size", size > 0f && size <= radius * 0.5f)
        }
    }
}
