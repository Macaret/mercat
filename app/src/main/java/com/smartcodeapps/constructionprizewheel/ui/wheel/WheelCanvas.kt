package com.smartcodeapps.constructionprizewheel.ui.wheel

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import com.smartcodeapps.constructionprizewheel.R
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/** Inner (near hub) and outer (near rim) colors per slice, echoing the glossy design wheel. */
private val SegmentGradients = listOf(
    listOf(Color(0xFF1E88F0), Color(0xFF0B5BC4), Color(0xFF0A4CA8)), // blue
    listOf(Color(0xFFF2352B), Color(0xFFD81D17), Color(0xFFB30F0B)), // red
    listOf(Color(0xFF22B53A), Color(0xFF13972B), Color(0xFF0B7A20)), // green
)
private val BustGradient = listOf(Color(0xFF9E9E9E), Color(0xFF7A7A7A), Color(0xFF5E5E5E))
private val SegmentColors = SegmentGradients.map { it[1] }
private val RimColor = Color(0xFF3A3A3A)
private val PointerColor = Color(0xFFC62828)
private const val WHEEL_RADIUS_FRACTION = 0.94f
private const val HUB_ICON_FRACTION = 0.28f

@Composable
fun WheelCanvas(
    segments: List<WheelSegment>,
    images: Map<Long, ImageBitmap>,
    rotationDegrees: () -> Float,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.aspectRatio(1f)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = size.minDimension / 2f * WHEEL_RADIUS_FRACTION
            val center = Offset(size.width / 2f, size.height / 2f)
            val count = segments.size

            rotate(degrees = rotationDegrees(), pivot = center) {
                if (count == 0) {
                    drawCircle(color = SegmentColors[0].copy(alpha = 0.3f), radius = radius, center = center)
                }
                segments.forEachIndexed { index, segment ->
                    drawArc(
                        brush = Brush.radialGradient(
                            colors = if (segment.isBust) BustGradient
                            else SegmentGradients[WheelGeometry.colorIndex(index, count)],
                            center = center,
                            radius = radius
                        ),
                        startAngle = WheelGeometry.segmentStart(index, count) - 90f,
                        sweepAngle = WheelGeometry.segmentAngle(count),
                        useCenter = true,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2f, radius * 2f)
                    )
                }
                if (count > 1) {
                    repeat(count) { index ->
                        val (x, y) = WheelGeometry.pointAt(
                            center.x, center.y, radius, WheelGeometry.segmentStart(index, count)
                        )
                        drawLine(Color.White, center, Offset(x, y), strokeWidth = 4f)
                    }
                }
                segments.forEachIndexed { index, segment ->
                    val image = segment.prizeId?.let { images[it] } ?: return@forEachIndexed
                    val side = WheelGeometry.imageSize(radius, count)
                    val distance = radius * WheelGeometry.IMAGE_DISTANCE
                    rotate(degrees = WheelGeometry.segmentCenter(index, count), pivot = center) {
                        val topLeft = Offset(center.x - side / 2f, center.y - distance - side / 2f)
                        val clip = Path().apply { addOval(Rect(topLeft, Size(side, side))) }
                        clipPath(clip) {
                            drawImage(
                                image = image,
                                dstOffset = IntOffset(topLeft.x.roundToInt(), topLeft.y.roundToInt()),
                                dstSize = IntSize(side.roundToInt(), side.roundToInt())
                            )
                        }
                        drawCircle(
                            color = Color.White,
                            radius = side / 2f,
                            center = Offset(center.x, center.y - distance),
                            style = Stroke(width = 4f)
                        )
                    }
                }
                drawCircle(color = RimColor, radius = radius, center = center, style = Stroke(width = 10f))
                drawCircle(color = RimColor, radius = radius * 0.08f, center = center)
            }
        }
        Image(
            painter = painterResource(R.drawable.wheel_hub_icon),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.align(Alignment.Center).fillMaxSize(HUB_ICON_FRACTION)
        )
        Pointer(Modifier.align(Alignment.TopCenter))
    }
}

@Composable
private fun Pointer(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(width = 44.dp, height = 52.dp)) {
        val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(size.width, 0f)
            lineTo(size.width / 2f, size.height)
            close()
        }
        drawPath(path, PointerColor)
        drawPath(path, Color.White, style = Stroke(width = 4f))
    }
}
