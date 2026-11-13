package com.smartcodeapps.constructionprizewheel.ui.wheel

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.pointerInput

const val ADMIN_LONG_PRESS_MS = 3_000L

/** Fires [onTriggered] once if the pointer stays down for the full duration. Releasing earlier does nothing. */
fun Modifier.adminLongPress(
    enabled: Boolean,
    durationMs: Long = ADMIN_LONG_PRESS_MS,
    onTriggered: () -> Unit
): Modifier = if (!enabled) this else pointerInput(durationMs) {
    awaitEachGesture {
        awaitFirstDown()
        try {
            withTimeout(durationMs) { waitForUpOrCancellation() }
        } catch (_: PointerEventTimeoutCancellationException) {
            onTriggered()
            waitForUpOrCancellation()
        }
    }
}
