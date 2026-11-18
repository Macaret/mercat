package com.smartcodeapps.constructionprizewheel.ui.wheel

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import com.smartcodeapps.constructionprizewheel.R
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
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
private val BustIconBackdrop = Color(0xFF4A4A4A)
/** Size of every circle (prize photos and bust icon) relative to its slice, so it sits with padding inside it. */
private const val IMAGE_CIRCLE_SCALE = 0.8f
private val SegmentColors = SegmentGradients.map { it[1] }
private const val WHEEL_RADIUS_FRACTION = 0.92f
private const val HUB_ICON_FRACTION = 0.28f
private const val PIN_FRACTION = 0.2f
private val PIN_RISE = 14.dp

@Composable
fun WheelCanvas(
    segments: List<WheelSegment>,
    images: Map<Long, ImageBitmap>,
    rotationDegrees: () -> Float,
    modifier: Modifier = Modifier
) {
    val bustImage = ImageBitmap.imageResource(R.drawable.bust_icon)
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
                    val image = (if (segment.isBust) bustImage else segment.prizeId?.let { images[it] })
                        ?: return@forEachIndexed
                    // Circles are scaled down, leaving a gap between them and the edges of their slice.
                    val side = WheelGeometry.imageSize(radius, count) * IMAGE_CIRCLE_SCALE
                    val distance = radius * WheelGeometry.IMAGE_DISTANCE
                    rotate(degrees = WheelGeometry.segmentCenter(index, count), pivot = center) {
                        val topLeft = Offset(center.x - side / 2f, center.y - distance - side / 2f)
                        val clip = Path().apply { addOval(Rect(topLeft, Size(side, side))) }
                        clipPath(clip) {
                            if (segment.isBust) {
                                // The icon is transparent, so give it a round backdrop like a prize photo.
                                drawCircle(
                                    color = BustIconBackdrop,
                                    radius = side / 2f,
                                    center = Offset(center.x, center.y - distance)
                                )
                            }
                            // Crop to a centered square so non-square images are not stretched.
                            val srcSide = minOf(image.width, image.height)
                            drawImage(
                                image = image,
                                srcOffset = IntOffset((image.width - srcSide) / 2, (image.height - srcSide) / 2),
                                srcSize = IntSize(srcSide, srcSide),
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
                drawCircle(color = Color(0xFF3A3A3A), radius = radius * 0.08f, center = center)
            }
        }
        Image(
            painter = painterResource(R.drawable.wheel_hub_icon),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.align(Alignment.Center).fillMaxSize(HUB_ICON_FRACTION)
        )
        Image(
            painter = painterResource(R.drawable.wheel_border),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )
        Image(
            painter = painterResource(R.drawable.wheel_pin),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.align(Alignment.TopCenter).offset(y = (-PIN_RISE)).fillMaxSize(PIN_FRACTION)
        )
    }
}
