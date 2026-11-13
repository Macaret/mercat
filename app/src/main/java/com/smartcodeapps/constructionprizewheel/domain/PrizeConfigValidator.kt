package com.smartcodeapps.constructionprizewheel.domain

sealed interface ConfigError {
    data class BlankName(val prizeId: Long) : ConfigError
    data class ProbabilityOutOfRange(val prizeId: Long, val value: Int) : ConfigError
    data class TooManyActive(val count: Int) : ConfigError
    data class ActiveMissingImage(val prizeId: Long) : ConfigError
    data class ProbabilityTotalOver100(val total: Int) : ConfigError

    /** Every slot holds a prize, so there are no busts to take the rest of the chance. */
    data class FullWheelTotalNot100(val total: Int) : ConfigError
}

object PrizeConfigValidator {

    fun validate(prizes: List<Prize>): List<ConfigError> {
        val errors = mutableListOf<ConfigError>()
        val active = prizes.filter { it.active }

        prizes.filter { it.name.isBlank() }
            .forEach { errors += ConfigError.BlankName(it.id) }
        prizes.filter { it.probability !in 0..100 }
            .forEach { errors += ConfigError.ProbabilityOutOfRange(it.id, it.probability) }
        if (active.size > MAX_ACTIVE_PRIZES) {
            errors += ConfigError.TooManyActive(active.size)
        }
        active.filter { it.imagePath.isNullOrBlank() }
            .forEach { errors += ConfigError.ActiveMissingImage(it.id) }
        val total = active.sumOf { it.probability }
        when {
            total > 100 -> errors += ConfigError.ProbabilityTotalOver100(total)
            active.size == WHEEL_SLOTS && total != 100 -> errors += ConfigError.FullWheelTotalNot100(total)
        }
        return errors
    }
}
