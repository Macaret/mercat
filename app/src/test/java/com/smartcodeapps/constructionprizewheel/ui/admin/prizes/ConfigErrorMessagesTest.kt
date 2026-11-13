package com.smartcodeapps.constructionprizewheel.ui.admin.prizes

import com.smartcodeapps.constructionprizewheel.R
import com.smartcodeapps.constructionprizewheel.domain.ConfigError
import com.smartcodeapps.constructionprizewheel.domain.Prize
import org.junit.Assert.assertEquals
import org.junit.Test

class ConfigErrorMessagesTest {

    private val prizes = listOf(
        Prize(1, "Hammer", null, null, 10, true, 1),
        Prize(2, "  ", null, null, 10, true, 2)
    )

    @Test
    fun `over 100 error carries the actual total`() {
        assertEquals(
            ErrorText(R.string.err_total_over_100, listOf(120)),
            ConfigError.ProbabilityTotalOver100(120).toErrorText(prizes)
        )
    }

    @Test
    fun `full wheel error carries the slot count and the actual total`() {
        assertEquals(
            ErrorText(R.string.err_full_wheel_total, listOf(8, 90)),
            ConfigError.FullWheelTotalNot100(90).toErrorText(prizes)
        )
    }

    @Test
    fun `too many active carries the limit and the count`() {
        assertEquals(
            ErrorText(R.string.err_too_many_active, listOf(8, 9)),
            ConfigError.TooManyActive(9).toErrorText(prizes)
        )
    }

    @Test
    fun `missing image names the prize`() {
        assertEquals(
            ErrorText(R.string.err_missing_image, listOf("Hammer")),
            ConfigError.ActiveMissingImage(1).toErrorText(prizes)
        )
    }

    @Test
    fun `out of range probability names the prize`() {
        assertEquals(
            ErrorText(R.string.err_probability_range, listOf("Hammer")),
            ConfigError.ProbabilityOutOfRange(1, 150).toErrorText(prizes)
        )
    }

    @Test
    fun `a prize without a name falls back to a placeholder`() {
        assertEquals(
            ErrorText(R.string.err_missing_image, listOf("(fără nume)")),
            ConfigError.ActiveMissingImage(2).toErrorText(prizes)
        )
    }

    @Test
    fun `blank name error has no arguments`() {
        assertEquals(ErrorText(R.string.err_blank_name), ConfigError.BlankName(2).toErrorText(prizes))
    }

    @Test
    fun `probability input keeps only up to three digits`() {
        assertEquals("123", sanitizeProbability("12a3b4"))
        assertEquals("", sanitizeProbability("abc"))
        assertEquals("50", sanitizeProbability("50"))
    }
}
