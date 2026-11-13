package com.smartcodeapps.constructionprizewheel.domain

sealed interface ConfigError {
    data class BlankName(val prizeId: Long) : ConfigError
    data class ProbabilityOutOfRange(val prizeId: Long, val value: Int) : ConfigError
    data class TooManyActive(val count: Int) : ConfigError
    data class ActiveMissingImage(val prizeId: Long) : ConfigError
    data class ProbabilityTotalInvalid(val total: Int) : ConfigError
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
        if (active.isNotEmpty()) {
            val total = active.sumOf { it.probability }
            if (total != 100) errors += ConfigError.ProbabilityTotalInvalid(total)
        }
        return errors
    }
}
