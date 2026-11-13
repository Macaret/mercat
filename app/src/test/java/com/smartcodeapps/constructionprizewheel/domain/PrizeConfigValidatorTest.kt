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
    fun `total below 100 is valid because busts take the rest`() {
        assertTrue(PrizeConfigValidator.validate(listOf(prize(1, 60), prize(2, 30))).isEmpty())
    }

    @Test
    fun `seven prizes totalling 99 are valid`() {
        val prizes = (1L..7L).map { prize(it, if (it == 1L) 93 else 1) }
        assertTrue(PrizeConfigValidator.validate(prizes).isEmpty())
    }

    @Test
    fun `total above 100 is rejected`() {
        val errors = PrizeConfigValidator.validate(listOf(prize(1, 70), prize(2, 50)))
        assertEquals(listOf(ConfigError.ProbabilityTotalOver100(120)), errors)
    }

    @Test
    fun `a full wheel below 100 is rejected`() {
        val prizes = (1L..8L).map { prize(it, if (it == 1L) 92 else 1) }
        assertEquals(listOf(ConfigError.FullWheelTotalNot100(99)), PrizeConfigValidator.validate(prizes))
    }

    @Test
    fun `a full wheel above 100 reports only the over 100 error`() {
        val prizes = (1L..8L).map { prize(it, if (it == 1L) 94 else 1) }
        assertEquals(listOf(ConfigError.ProbabilityTotalOver100(101)), PrizeConfigValidator.validate(prizes))
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
        val prizes = (1L..9L).map { prize(it, if (it == 1L) 91 else 1) }
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
        val prizes = listOf(prize(1, 80, imagePath = null), prize(2, 80, name = ""))
        val errors = PrizeConfigValidator.validate(prizes)
        assertEquals(
            setOf(
                ConfigError.BlankName(2),
                ConfigError.ActiveMissingImage(1),
                ConfigError.ProbabilityTotalOver100(160)
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

    private fun List<Prize>.slotLayout(): String =
        wheelSlots().joinToString("") { if (it is WheelSlot.PrizeSlot) it.prize.id.toString() else "-" }

    @Test
    fun `no prizes means eight bust slots`() {
        assertEquals("--------", emptyList<Prize>().slotLayout())
    }

    @Test
    fun `prizes are spread evenly around the eight slots in wheel order`() {
        assertEquals("1-------", listOf(prize(1)).slotLayout())
        assertEquals("1---2---", (1L..2L).map { prize(it) }.slotLayout())
        assertEquals("1-2--3--", (1L..3L).map { prize(it) }.slotLayout())
        assertEquals("1-2-3-4-", (1L..4L).map { prize(it) }.slotLayout())
        assertEquals("12-34-5-", (1L..5L).map { prize(it) }.slotLayout())
        assertEquals("1234567-", (1L..7L).map { prize(it) }.slotLayout())
        assertEquals("12345678", (1L..8L).map { prize(it) }.slotLayout())
    }

    @Test
    fun `inactive prizes do not take a slot`() {
        assertEquals("3---1---", listOf(prize(1, order = 2), prize(2, active = false), prize(3, order = 1)).slotLayout())
    }
}
