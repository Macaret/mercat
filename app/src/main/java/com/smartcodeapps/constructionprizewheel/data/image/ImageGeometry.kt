package com.smartcodeapps.constructionprizewheel.data.image

data class CropSquare(val x: Int, val y: Int, val size: Int)

object ImageGeometry {

    fun centerSquare(width: Int, height: Int): CropSquare {
        val size = minOf(width, height)
        return CropSquare(x = (width - size) / 2, y = (height - size) / 2, size = size)
    }

    fun sampleSize(width: Int, height: Int, maxEdge: Int): Int {
        var sample = 1
        while (minOf(width, height) / (sample * 2) >= maxEdge) sample *= 2
        return sample
    }

    fun fitWithin(width: Int, height: Int, maxEdge: Int): Pair<Int, Int> {
        val longest = maxOf(width, height)
        if (longest <= maxEdge) return width to height
        val scale = maxEdge.toDouble() / longest
        return maxOf(1, (width * scale).toInt()) to maxOf(1, (height * scale).toInt())
    }
}
