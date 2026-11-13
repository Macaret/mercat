package com.smartcodeapps.constructionprizewheel.ui.admin.prizes

import androidx.annotation.StringRes
import com.smartcodeapps.constructionprizewheel.R
import com.smartcodeapps.constructionprizewheel.domain.ConfigError
import com.smartcodeapps.constructionprizewheel.domain.MAX_ACTIVE_PRIZES
import com.smartcodeapps.constructionprizewheel.domain.Prize

data class ErrorText(@StringRes val resId: Int, val args: List<Any> = emptyList())

private const val UNNAMED = "(no name)"

fun ConfigError.toErrorText(prizes: List<Prize>): ErrorText {
    fun nameOf(id: Long) = prizes.firstOrNull { it.id == id }?.name?.takeIf { it.isNotBlank() } ?: UNNAMED
    return when (this) {
        is ConfigError.BlankName -> ErrorText(R.string.err_blank_name)
        is ConfigError.ProbabilityOutOfRange -> ErrorText(R.string.err_probability_range, listOf(nameOf(prizeId)))
        is ConfigError.TooManyActive -> ErrorText(R.string.err_too_many_active, listOf(MAX_ACTIVE_PRIZES, count))
        is ConfigError.ActiveMissingImage -> ErrorText(R.string.err_missing_image, listOf(nameOf(prizeId)))
        is ConfigError.ProbabilityTotalInvalid -> ErrorText(R.string.err_total_not_100, listOf(total))
    }
}
