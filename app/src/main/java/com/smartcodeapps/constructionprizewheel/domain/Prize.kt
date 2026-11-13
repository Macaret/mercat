package com.smartcodeapps.constructionprizewheel.domain

/** The wheel always has this many slots. Slots not taken by an active prize are busts. */
const val WHEEL_SLOTS = 8

const val MAX_ACTIVE_PRIZES = WHEEL_SLOTS

data class Prize(
    val id: Long,
    val name: String,
    val description: String?,
    val imagePath: String?,
    val probability: Int,
    val active: Boolean,
    val wheelOrder: Int?
)

fun List<Prize>.wheelPrizes(): List<Prize> =
    filter { it.active }.sortedBy { it.wheelOrder ?: Int.MAX_VALUE }

sealed interface WheelSlot {
    data class PrizeSlot(val prize: Prize) : WheelSlot
    data object Bust : WheelSlot
}

/**
 * Always [WHEEL_SLOTS] slots. The active prizes keep their wheel order and are spread as evenly as
 * possible around the wheel; the remaining slots are busts.
 */
fun List<Prize>.wheelSlots(): List<WheelSlot> {
    val prizes = wheelPrizes().take(WHEEL_SLOTS)
    val slots = MutableList<WheelSlot>(WHEEL_SLOTS) { WheelSlot.Bust }
    prizes.forEachIndexed { index, prize ->
        slots[index * WHEEL_SLOTS / prizes.size] = WheelSlot.PrizeSlot(prize)
    }
    return slots
}
