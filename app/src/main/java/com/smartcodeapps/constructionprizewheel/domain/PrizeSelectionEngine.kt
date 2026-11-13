package com.smartcodeapps.constructionprizewheel.domain

import javax.inject.Inject
import kotlin.random.Random

class PrizeSelectionEngine @Inject constructor(private val random: Random) {

    fun select(prizes: List<Prize>): Prize? {
        val candidates = prizes.filter { it.active && it.probability > 0 }
        val total = candidates.sumOf { it.probability }
        if (total <= 0) return null

        var remaining = random.nextInt(total)
        for (prize in candidates) {
            if (remaining < prize.probability) return prize
            remaining -= prize.probability
        }
        return candidates.last()
    }
}
