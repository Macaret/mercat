package com.smartcodeapps.constructionprizewheel.ui.common

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember

/** Returned by [ShineSchedule] while the shine is not sweeping. */
const val SHINE_OFF = -1f

/** Timing of the light streak that sweeps over the spin button once every [CYCLE_MS]. */
class ShineSchedule(private val phase: State<Float>) {
    /** Horizontal position of the button's shine as a fraction of its width, or [SHINE_OFF]. */
    val button: Float get() = shinePosition(phase.value)

    companion object {
        const val CYCLE_MS = 7_000

        /** Share of a cycle the sweep takes (1.4 s); the rest of the cycle is a pause. */
        const val SWEEP_FRACTION = 0.2f
    }
}

/**
 * Where the shine is for a cycle [phase] in 0..1. It travels from just left of the element (-0.4)
 * to just right of it (1.4), so it enters and leaves fully, then stays off until the next cycle.
 */
fun shinePosition(phase: Float): Float {
    val local = phase / ShineSchedule.SWEEP_FRACTION
    if (local > 1f) return SHINE_OFF
    return -0.4f + local * 1.8f
}

@Composable
fun rememberShineSchedule(): ShineSchedule {
    val phase = rememberInfiniteTransition(label = "shineSchedule").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(ShineSchedule.CYCLE_MS, easing = LinearEasing)),
        label = "phase"
    )
    return remember(phase) { ShineSchedule(phase) }
}
