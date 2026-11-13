package com.smartcodeapps.constructionprizewheel.ui.result

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartcodeapps.constructionprizewheel.R
import com.smartcodeapps.constructionprizewheel.ui.common.MainButton
import com.smartcodeapps.constructionprizewheel.ui.common.rememberImageBitmap
import com.smartcodeapps.constructionprizewheel.ui.theme.ConstructionPrizeWheelTheme
import nl.dionsegijn.konfetti.compose.KonfettiView

object ResultTestTags {
    const val CONTINUE_BUTTON = "continue_button"
    const val PRIZE_NAME = "prize_name"
    const val PRIZE_DESCRIPTION = "prize_description"
    const val PRIZE_IMAGE = "prize_image"
    const val LOGO = "result_logo"
}

private val PRIZE_CIRCLE_SIZE = 255.dp
/** transparent_logo.png is 626x342; at this height it is about 146dp wide. */
private val LOGO_HEIGHT = 80.dp
private val BANNER_MAX_WIDTH = 460.dp
private val BANNER_TOP_GAP = 14.dp
/**
 * Fraction of the banner's height that moves down behind the prize circle. The ribbon's middle
 * curves up to about 64% of its height, so about a third closes the gap above the circle.
 */
private const val BANNER_OVERLAP = 0.34f
private val GAP = 8.dp
private val TEXT_COLOR = Color(0xFF1B2A1B)
private val PRIZE_NAME_SIZE = 34.sp
private val PRIZE_DESCRIPTION_SIZE = 20.sp

/** How far the logo drops and the texts rise while appearing. */
private val RISE_DISTANCE = 24.dp
private const val RAYS_SCALE = 2.2f
private const val RAY_COUNT = 14
private const val GLOW_SCALE = 1.4f
private val GlowColor = Color(0xFFFFE08A)

@Composable
fun PrizeResultScreen(onContinue: () -> Unit, viewModel: ResultViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    BackHandler(onBack = onContinue)
    ResultContent(state = state, onContinue = onContinue)
}

@Composable
fun ResultContent(state: ResultUiState, onContinue: () -> Unit, modifier: Modifier = Modifier) {
    val prize = state.prize
    val image = rememberImageBitmap(prize?.imagePath)
    val anim = rememberWinAnimation()
    val loops = rememberWinLoops()
    val inPreview = LocalInspectionMode.current

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val riseDistancePx = with(density) { RISE_DISTANCE.toPx() }
        // Space above the centered prize circle. The logo takes the top of it and the banner sits
        // between the logo and the prize, so the banner's width is limited by what is left.
        val spaceAbovePrize: Dp = maxHeight / 2 - PRIZE_CIRCLE_SIZE / 2
        // The banner tucks BANNER_OVERLAP of its height down behind the prize circle, so it can be
        // taller than the free space: only (1 - BANNER_OVERLAP) of it has to fit above the circle.
        val bannerHeightLimit: Dp =
            ((spaceAbovePrize - LOGO_HEIGHT - BANNER_TOP_GAP) / (1f - BANNER_OVERLAP)).coerceAtLeast(0.dp)

        Image(
            painter = painterResource(R.drawable.winner_background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Slowly turning light rays behind the prize. They appear together with the prize.
        Canvas(
            modifier = Modifier
                .align(Alignment.Center)
                .size(PRIZE_CIRCLE_SIZE * RAYS_SCALE)
                .graphicsLayer {
                    rotationZ = loops.raysRotation
                    alpha = anim.prize.value.coerceIn(0f, 1f)
                }
        ) {
            val radius = size.minDimension / 2f
            val brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.5f), Color.White.copy(alpha = 0f)),
                center = center,
                radius = radius
            )
            val step = 360f / RAY_COUNT
            repeat(RAY_COUNT) { index ->
                drawArc(brush = brush, startAngle = index * step, sweepAngle = step / 2f, useCenter = true)
            }
        }

        // Soft warm glow right around the circle, breathing in and out.
        Canvas(
            modifier = Modifier
                .align(Alignment.Center)
                .size(PRIZE_CIRCLE_SIZE * GLOW_SCALE)
                .graphicsLayer {
                    val breath = loops.glow
                    scaleX = 1f + breath * 0.06f
                    scaleY = scaleX
                    alpha = anim.prize.value.coerceIn(0f, 1f) * (0.55f + breath * 0.45f)
                }
        ) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(GlowColor.copy(alpha = 0.9f), GlowColor.copy(alpha = 0f)),
                    center = center,
                    radius = size.minDimension / 2f
                )
            )
        }

        // Logo flush against the top edge of the screen, with no padding. Slides down into place.
        Image(
            painter = painterResource(R.drawable.transparent_logo),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .height(LOGO_HEIGHT)
                .graphicsLayer {
                    val p = anim.logo.value
                    alpha = p
                    translationY = (1f - p) * -riseDistancePx
                }
                .testTag(ResultTestTags.LOGO)
        )

        // Banner sits between the logo and the prize. It is drawn before the prize, so the circle
        // covers the part that tucks into the ribbon's upward curve.
        Box(
            contentAlignment = Alignment.BottomCenter,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(spaceAbovePrize)
        ) {
            Image(
                painter = painterResource(R.drawable.congrats_banner),
                contentDescription = stringResource(R.string.congrats_banner_description),
                contentScale = ContentScale.Fit,
                // Fit keeps the image proportions inside both limits, so no aspect ratio is needed.
                modifier = Modifier
                    .heightIn(max = bannerHeightLimit)
                    .widthIn(max = BANNER_MAX_WIDTH)
                    .graphicsLayer {
                        // Drops in from above with a small wobble; the spring overshoot swings it back.
                        val p = anim.banner.value
                        alpha = p.coerceIn(0f, 1f)
                        translationY = size.height * BANNER_OVERLAP - (1f - p) * size.height * 2f
                        rotationZ = (1f - p) * 8f
                        // Offscreen so the shine below only lights the ribbon, not the transparent corners.
                        compositingStrategy = CompositingStrategy.Offscreen
                    }
                    .drawWithContent {
                        drawContent()
                        val x = size.width * loops.shine
                        val band = size.width * 0.18f
                        drawRect(
                            brush = Brush.linearGradient(
                                colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.45f), Color.Transparent),
                                start = Offset(x - band, 0f),
                                end = Offset(x + band, size.height)
                            ),
                            blendMode = BlendMode.SrcAtop
                        )
                    }
            )
        }

        // Prize, centered on the whole screen.
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = prize?.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(PRIZE_CIRCLE_SIZE)
                    .graphicsLayer {
                        // Pops in with a small bounce (the spring overshoots past full size) and a turn.
                        val p = anim.prize.value
                        scaleX = p
                        scaleY = p
                        alpha = p.coerceIn(0f, 1f)
                        rotationZ = (1f - p) * -20f
                    }
                    .clip(CircleShape)
                    .border(4.dp, Color.White, CircleShape)
                    .testTag(ResultTestTags.PRIZE_IMAGE)
            )
        }

        // Name and description below the prize, in the space left above the button.
        if (prize != null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = maxHeight / 2 + PRIZE_CIRCLE_SIZE / 2 + GAP)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Text(
                    text = prize.name,
                    style = MaterialTheme.typography.headlineMedium,
                    fontSize = PRIZE_NAME_SIZE,
                    lineHeight = PRIZE_NAME_SIZE * 1.2f,
                    fontWeight = FontWeight.Bold,
                    color = TEXT_COLOR,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .riseIn(anim.name, riseDistancePx)
                        .testTag(ResultTestTags.PRIZE_NAME)
                )
                if (!prize.description.isNullOrBlank()) {
                    Text(
                        text = prize.description,
                        style = MaterialTheme.typography.bodyLarge,
                        fontSize = PRIZE_DESCRIPTION_SIZE,
                        lineHeight = PRIZE_DESCRIPTION_SIZE * 1.25f,
                        fontWeight = FontWeight.SemiBold,
                        color = TEXT_COLOR,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .riseIn(anim.description, riseDistancePx)
                            .testTag(ResultTestTags.PRIZE_DESCRIPTION)
                    )
                }
            }
        }

        // Confetti over the prize and texts, under the button. Konfetti only draws, so taps pass through.
        if (anim.showConfetti && !inPreview) {
            KonfettiView(parties = winConfetti, modifier = Modifier.fillMaxSize())
        }

        // Continue button pinned to the bottom. It fades in last and ignores taps until then.
        MainButton(
            text = stringResource(R.string.continue_button),
            onClick = { if (anim.continueEnabled) onContinue() },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
                .height(64.dp)
                .graphicsLayer { alpha = anim.button.value }
                .testTag(ResultTestTags.CONTINUE_BUTTON)
        )
    }
}

/** Fades in while rising [distancePx] into place, driven by [progress] (0..1). */
private fun Modifier.riseIn(progress: Animatable<Float, *>, distancePx: Float): Modifier =
    graphicsLayer {
        val p = progress.value
        alpha = p
        translationY = (1f - p) * distancePx
    }

@Preview(widthDp = 1024, heightDp = 600, showBackground = true)
@Composable
private fun ResultContentPreview() {
    ConstructionPrizeWheelTheme {
        ResultContent(
            state = ResultUiState(
                loaded = true,
                prize = ResultPrize("Professional Hammer", "Heavy duty, 500g head", null)
            ),
            onContinue = {}
        )
    }
}
