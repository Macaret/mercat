package com.smartcodeapps.constructionprizewheel.data.image

import org.junit.Assert.assertEquals
import org.junit.Test

class ImageGeometryTest {

    @Test
    fun `landscape image is cropped to a centered square`() {
        assertEquals(CropSquare(x = 200, y = 0, size = 600), ImageGeometry.centerSquare(1000, 600))
    }

    @Test
    fun `portrait image is cropped to a centered square`() {
        assertEquals(CropSquare(x = 0, y = 350, size = 300), ImageGeometry.centerSquare(300, 1000))
    }

    @Test
    fun `square image is unchanged`() {
        assertEquals(CropSquare(0, 0, 500), ImageGeometry.centerSquare(500, 500))
    }

    @Test
    fun `sample size keeps the short edge at or above the target`() {
        assertEquals(1, ImageGeometry.sampleSize(1000, 900, 800))
        assertEquals(4, ImageGeometry.sampleSize(4000, 3200, 800))
        assertEquals(1, ImageGeometry.sampleSize(500, 400, 800))
    }

    @Test
    fun `fit within scales the longest edge`() {
        assertEquals(800 to 400, ImageGeometry.fitWithin(1600, 800, 800))
        assertEquals(300 to 200, ImageGeometry.fitWithin(300, 200, 800))
    }
}
