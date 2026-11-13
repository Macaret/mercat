package com.smartcodeapps.constructionprizewheel.ui.wheel

import com.smartcodeapps.constructionprizewheel.data.Branding
import com.smartcodeapps.constructionprizewheel.domain.Prize
import com.smartcodeapps.constructionprizewheel.domain.WheelSlot
import com.smartcodeapps.constructionprizewheel.domain.wheelSlots

/** A bust segment has no prize: [prizeId], [name] and [imagePath] are all null. */
data class WheelSegment(val prizeId: Long?, val name: String?, val imagePath: String?) {
    val isBust: Boolean get() = prizeId == null

    companion object {
        val Bust = WheelSegment(null, null, null)
    }
}

/**
 * [busy] stays true from the tap until the customer dismisses the result screen, or until a bust lands.
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
}

fun buildWheelUiState(
    prizes: List<Prize>,
    branding: Branding,
    spin: SpinState = SpinState()
): WheelUiState = WheelUiState(
    loaded = true,
    branding = branding,
    segments = prizes.wheelSlots().map { slot ->
        when (slot) {
            is WheelSlot.PrizeSlot -> WheelSegment(slot.prize.id, slot.prize.name, slot.prize.imagePath)
            WheelSlot.Bust -> WheelSegment.Bust
        }
    },
    restingRotation = spin.restingRotation,
    spinPlan = spin.plan,
    resultPrizeId = spin.resultPrizeId,
    spinning = spin.busy
)
