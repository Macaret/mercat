package com.smartcodeapps.constructionprizewheel.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PrizeConfigValidatorTest {

    private fun prize(
        id: Long,
        probability: Int = 0,
        active: Boolean = true,
        name: String = "Prize $id",
        imagePath: String? = "img/$id.jpg",
        order: Int? = id.toInt()
    ) = Prize(id, name, null, imagePath, probability, active, if (active) order else null)

    @Test
    fun `no prizes is valid`() {
        assertTrue(PrizeConfigValidator.validate(emptyList()).isEmpty())
    }

    @Test
    fun `active prizes totalling 100 are valid`() {
        val prizes = listOf(prize(1, 60), prize(2, 40))
        assertTrue(PrizeConfigValidator.validate(prizes).isEmpty())
    }

    @Test
    fun `total below 100 is rejected with the actual total`() {
        val errors = PrizeConfigValidator.validate(listOf(prize(1, 60), prize(2, 30)))
        assertEquals(listOf(ConfigError.ProbabilityTotalInvalid(90)), errors)
    }

    @Test
    fun `total above 100 is rejected`() {
        val errors = PrizeConfigValidator.validate(listOf(prize(1, 70), prize(2, 50)))
        assertEquals(listOf(ConfigError.ProbabilityTotalInvalid(120)), errors)
    }

    @Test
    fun `inactive prizes do not count toward the total`() {
        val prizes = listOf(prize(1, 100), prize(2, 50, active = false))
        assertTrue(PrizeConfigValidator.validate(prizes).isEmpty())
    }

    @Test
    fun `a zero percent active prize is valid`() {
        val prizes = listOf(prize(1, 100), prize(2, 0))
        assertTrue(PrizeConfigValidator.validate(prizes).isEmpty())
    }

    @Test
    fun `nine active prizes exceed the limit`() {
        val prizes = (1L..9L).map { prize(it, if (it == 1L) 92 else 1) }
        val errors = PrizeConfigValidator.validate(prizes)
        assertEquals(listOf(ConfigError.TooManyActive(9)), errors)
    }

    @Test
    fun `eight active prizes are allowed`() {
        val prizes = (1L..8L).map { prize(it, if (it == 1L) 93 else 1) }
        assertTrue(PrizeConfigValidator.validate(prizes).isEmpty())
    }

    @Test
    fun `active prize without image is rejected`() {
        val errors = PrizeConfigValidator.validate(listOf(prize(1, 100, imagePath = null)))
        assertEquals(listOf(ConfigError.ActiveMissingImage(1)), errors)
    }

    @Test
    fun `inactive prize without image is allowed`() {
        val prizes = listOf(prize(1, 100), prize(2, 0, active = false, imagePath = null))
        assertTrue(PrizeConfigValidator.validate(prizes).isEmpty())
    }

    @Test
    fun `probability out of range is rejected even when inactive`() {
        val prizes = listOf(prize(1, 100), prize(2, 150, active = false), prize(3, -5, active = false))
        val errors = PrizeConfigValidator.validate(prizes)
        assertEquals(
            listOf(
                ConfigError.ProbabilityOutOfRange(2, 150),
                ConfigError.ProbabilityOutOfRange(3, -5)
            ),
            errors
        )
    }

    @Test
    fun `blank name is rejected`() {
        val errors = PrizeConfigValidator.validate(listOf(prize(1, 100, name = "  ")))
        assertEquals(listOf(ConfigError.BlankName(1)), errors)
    }

    @Test
    fun `all failing rules are reported together`() {
        val prizes = listOf(prize(1, 30, imagePath = null), prize(2, 30, name = ""))
        val errors = PrizeConfigValidator.validate(prizes)
        assertEquals(
            setOf(
                ConfigError.BlankName(2),
                ConfigError.ActiveMissingImage(1),
                ConfigError.ProbabilityTotalInvalid(60)
            ),
            errors.toSet()
        )
    }

    @Test
    fun `wheel prizes are the active ones in wheel order`() {
        val prizes = listOf(
            prize(1, 40, order = 3),
            prize(2, 0, active = false),
            prize(3, 30, order = 1),
            prize(4, 30, order = 2)
        )
        assertEquals(listOf(3L, 4L, 1L), prizes.wheelPrizes().map { it.id })
    }
}
