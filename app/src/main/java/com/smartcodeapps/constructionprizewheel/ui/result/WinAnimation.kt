package com.smartcodeapps.constructionprizewheel.ui.result

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalInspectionMode
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import nl.dionsegijn.konfetti.core.models.Shape
import java.util.concurrent.TimeUnit

/**
 * Entry timeline for the win screen. Each value goes from 0 (hidden) to 1 (in place); springs may
 * overshoot slightly above 1, which gives the bounce.
 *
 *  0 ms     logo slides down and fades in
 *  150 ms   prize circle pops in with a small bounce and turn
 *  400 ms   banner drops in with a small wobble
 *  700 ms   confetti from both bottom corners
 *  900 ms   prize name rises in, description 100 ms later
 *  1200 ms  continue button fades in; it accepts taps from 1500 ms
 */
class WinAnimation(startFinished: Boolean) {
    private val initial = if (startFinished) 1f else 0f

    val logo = Animatable(initial)
    val prize = Animatable(initial)
    val banner = Animatable(initial)
    val name = Animatable(initial)
    val description = Animatable(initial)
    val button = Animatable(initial)

    /** Konfetti redraws every frame while shown, so it is removed once the pieces have fallen. */
    var showConfetti by mutableStateOf(false)
        private set

    /** Taps on CONTINUE are ignored until the customer has had a moment to see the prize. */
    var continueEnabled by mutableStateOf(startFinished)
        private set

    suspend fun play() = coroutineScope {
        launch { logo.animateTo(1f, tween(300, easing = FastOutSlowInEasing)) }
        launch {
            delay(150)
            prize.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow))
        }
        launch {
            delay(400)
            banner.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMediumLow))
        }
        launch {
            delay(700)
            showConfetti = true
            delay(CONFETTI_VISIBLE_MS)
            showConfetti = false
        }
        launch {
            delay(900)
            name.animateTo(1f, tween(400, easing = FastOutSlowInEasing))
        }
        launch {
            delay(1000)
            description.animateTo(1f, tween(400, easing = FastOutSlowInEasing))
        }
        launch {
            delay(1200)
            button.animateTo(1f, tween(300))
            continueEnabled = true
        }
    }

    companion object {
        private const val CONFETTI_VISIBLE_MS = 4_500L
    }
}

/** In previews everything is shown in its final place, with no confetti. */
@Composable
fun rememberWinAnimation(): WinAnimation {
    val inPreview = LocalInspectionMode.current
    val animation = remember { WinAnimation(startFinished = inPreview) }
    if (!inPreview) LaunchedEffect(animation) { animation.play() }
    return animation
}

/** Slow loops that keep the screen alive after the entry: rays, glow and banner shine. */
class WinLoops(rays: State<Float>, glow: State<Float>, shine: State<Float>) {
    /** Rotation of the light rays behind the prize, in degrees. */
    val raysRotation by rays

    /** 0..1, the glow behind the prize breathing in and out. */
    val glow by glow

    /** Horizontal position of the shine across the banner, as a fraction of its width. */
    val shine by shine
}

@Composable
fun rememberWinLoops(): WinLoops {
    val transition = rememberInfiniteTransition(label = "winLoops")
    val rays = transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(40_000, easing = LinearEasing)),
        label = "rays"
    )
    val glow = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2_000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow"
    )
    val shine = transition.animateFloat(
        initialValue = -0.4f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(tween(1_400, delayMillis = 2_600, easing = LinearEasing)),
        label = "shine"
    )
    return remember(transition) { WinLoops(rays, glow, shine) }
}

private val ConfettiColors = listOf(0x1E88F0, 0xF2352B, 0x22B53A, 0xFFC83D, 0xFFFFFF)

/** Two cannons in the bottom corners, aimed up towards the middle of the screen. */
val winConfetti: List<Party> = listOf(
    confettiCannon(x = 0.0, angle = 300),
    confettiCannon(x = 1.0, angle = 240)
)

private fun confettiCannon(x: Double, angle: Int) = Party(
    angle = angle,
    spread = 35,
    speed = 30f,
    maxSpeed = 55f,
    damping = 0.9f,
    colors = ConfettiColors,
    shapes = listOf(Shape.Square, Shape.Circle),
    timeToLive = 3_500L,
    fadeOutEnabled = true,
    position = Position.Relative(x, 1.0),
    emitter = Emitter(duration = 250, TimeUnit.MILLISECONDS).max(70)
)
