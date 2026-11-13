package com.smartcodeapps.constructionprizewheel.ui.wheel

import com.smartcodeapps.constructionprizewheel.data.Branding
import com.smartcodeapps.constructionprizewheel.domain.Prize
import com.smartcodeapps.constructionprizewheel.domain.wheelPrizes

data class WheelSegment(val prizeId: Long, val name: String, val imagePath: String?)

/**
 * [busy] stays true from the tap until the customer dismisses the result screen.
 * [resultPrizeId] is set once the spin record is saved, and cleared when navigation to the result happens.
 */
data class SpinState(
    val restingRotation: Float = 0f,
    val busy: Boolean = false,
    val plan: SpinPlan? = null,
    val resultPrizeId: Long? = null
)

data class WheelUiState(
    val loaded: Boolean = false,
    val branding: Branding = Branding(),
    val segments: List<WheelSegment> = emptyList(),
    val restingRotation: Float = 0f,
    val spinPlan: SpinPlan? = null,
    val resultPrizeId: Long? = null,
    val spinning: Boolean = false
) {
    val canSpin: Boolean get() = loaded && segments.isNotEmpty() && !spinning
    val showNoPrizesMessage: Boolean get() = loaded && segments.isEmpty()
}

fun buildWheelUiState(
    prizes: List<Prize>,
    branding: Branding,
    spin: SpinState = SpinState()
): WheelUiState = WheelUiState(
    loaded = true,
    branding = branding,
    segments = prizes.wheelPrizes().map { WheelSegment(it.id, it.name, it.imagePath) },
    restingRotation = spin.restingRotation,
    spinPlan = spin.plan,
    resultPrizeId = spin.resultPrizeId,
    spinning = spin.busy
)
