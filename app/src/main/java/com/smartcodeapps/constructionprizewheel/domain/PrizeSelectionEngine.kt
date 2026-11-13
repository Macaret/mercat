package com.smartcodeapps.constructionprizewheel.domain

import javax.inject.Inject
import kotlin.random.Random

sealed interface SpinOutcome {
    /** Index of the wheel slot the wheel must land on. */
    val slotIndex: Int

    data class Win(val prize: Prize, override val slotIndex: Int) : SpinOutcome
    data class Bust(override val slotIndex: Int) : SpinOutcome
}

/**
 * Each active prize wins with its own percentage. Whatever is left up to 100% is a bust,
 * split equally across the bust slots.
 */
class PrizeSelectionEngine @Inject constructor(private val random: Random) {

    fun select(prizes: List<Prize>): SpinOutcome {
        val slots = prizes.wheelSlots()
        val prizeSlots = slots.withIndex().filter { it.value is WheelSlot.PrizeSlot }
            .map { (index, slot) -> index to (slot as WheelSlot.PrizeSlot).prize }
        val bustSlots = slots.indices.filter { slots[it] is WheelSlot.Bust }
        val prizeTotal = prizeSlots.sumOf { (_, prize) -> prize.probability.coerceAtLeast(0) }

        // A valid configuration draws from 100. The fallbacks only matter for invalid data
        // (a full wheel not totalling 100, or a total above 100) and keep the spin working.
        val range = if (bustSlots.isEmpty()) prizeTotal else maxOf(PERCENT, prizeTotal)
        if (range <= 0) {
            val (index, prize) = prizeSlots[random.nextInt(prizeSlots.size)]
            return SpinOutcome.Win(prize, index)
        }

        var remaining = random.nextInt(range)
        for ((index, prize) in prizeSlots) {
            val chance = prize.probability.coerceAtLeast(0)
            if (remaining < chance) return SpinOutcome.Win(prize, index)
            remaining -= chance
        }
        return SpinOutcome.Bust(bustSlots[random.nextInt(bustSlots.size)])
    }

    private companion object {
        const val PERCENT = 100
    }
}
