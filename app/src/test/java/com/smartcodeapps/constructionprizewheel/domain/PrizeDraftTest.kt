package com.smartcodeapps.constructionprizewheel.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class PrizeDraftTest {

    private fun prize(id: Long, name: String = "Prize $id", probability: Int = 0, active: Boolean = false, order: Int? = null) =
        Prize(id, name, null, "/img/$id.jpg", probability, active, if (active) order else null)

    private fun fullWheel() = PrizeDraft((1L..8L).map { prize(it, active = true, order = it.toInt(), probability = 10) })

    @Test
    fun `added prizes get unique negative temporary ids`() {
        val draft = PrizeDraft(listOf(prize(5)))
            .add("A", null, null, 0, false)
            .add("B", null, null, 0, false)

        val ids = draft.prizes.map { it.id }
        assertEquals(listOf(5L, -1L, -2L), ids)
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `an added inactive prize has no wheel order`() {
        val added = PrizeDraft(emptyList()).add("A", "desc", "/img/a.jpg", 0, false).prizes.single()
        assertFalse(added.active)
        assertNull(added.wheelOrder)
        assertEquals("desc", added.description)
        assertEquals("/img/a.jpg", added.imagePath)
    }

    @Test
    fun `an added active prize goes to the end of the wheel order`() {
        val draft = PrizeDraft(listOf(prize(1, active = true, order = 1), prize(2, active = true, order = 4)))
            .add("New", null, null, 10, true)
        assertEquals(5, draft.prizes.last().wheelOrder)
    }

    @Test
    fun `activating a prize appends it to the end of the wheel order`() {
        val draft = PrizeDraft(listOf(prize(1, active = true, order = 1), prize(2, active = true, order = 2), prize(3)))
            .update(3, "Prize 3", null, "/img/3.jpg", 10, true)

        val updated = draft.prizes.first { it.id == 3L }
        assertTrue(updated.active)
        assertEquals(3, updated.wheelOrder)
    }

    @Test
    fun `deactivating a prize removes it from the wheel order`() {
        val draft = PrizeDraft(listOf(prize(1, active = true, order = 1), prize(2, active = true, order = 2)))
            .update(1, "Prize 1", null, "/img/1.jpg", 0, false)

        assertNull(draft.prizes.first { it.id == 1L }.wheelOrder)
        assertEquals(2, draft.prizes.first { it.id == 2L }.wheelOrder)
    }

    @Test
    fun `updating an active prize keeps its wheel position`() {
        val draft = PrizeDraft(listOf(prize(1, active = true, order = 1), prize(2, active = true, order = 2)))
            .update(1, "Renamed", "d", "/img/x.jpg", 30, true)

        val updated = draft.prizes.first { it.id == 1L }
        assertEquals("Renamed", updated.name)
        assertEquals(30, updated.probability)
        assertEquals(1, updated.wheelOrder)
    }

    @Test
    fun `update only changes the targeted prize`() {
        val original = listOf(prize(1, name = "One"), prize(2, name = "Two"))
        val draft = PrizeDraft(original).update(1, "Changed", null, null, 0, false)
        assertEquals(original[1], draft.prizes[1])
    }

    @Test
    fun `a ninth active prize cannot be added or activated`() {
        val full = fullWheel()
        assertFalse(full.canActivateMore)
        assertThrows(IllegalArgumentException::class.java) { full.add("Ninth", null, "/i.jpg", 0, true) }

        val withInactive = PrizeDraft(full.prizes + prize(9))
        assertThrows(IllegalArgumentException::class.java) {
            withInactive.update(9, "Prize 9", null, "/img/9.jpg", 0, true)
        }
    }

    @Test
    fun `an inactive prize can still be added when the wheel is full`() {
        assertEquals(9, fullWheel().add("Spare", null, null, 0, false).prizes.size)
    }

    @Test
    fun `deleting an active prize frees a slot`() {
        val draft = fullWheel().delete(3)
        assertEquals(7, draft.activeCount)
        assertTrue(draft.canActivateMore)
    }

    @Test
    fun `delete removes only that prize`() {
        val draft = PrizeDraft(listOf(prize(1), prize(2), prize(3))).delete(2)
        assertEquals(listOf(1L, 3L), draft.prizes.map { it.id })
    }

    @Test
    fun `a failed activation leaves the draft untouched`() {
        val full = fullWheel()
        runCatching { full.add("Ninth", null, "/i.jpg", 0, true) }
        assertEquals(8, full.prizes.size)
    }

    private fun threeActive() = PrizeDraft(
        listOf(
            prize(1, active = true, order = 1, probability = 30),
            prize(2, active = true, order = 2, probability = 30),
            prize(3, active = true, order = 3, probability = 40),
            prize(4)
        )
    )

    private fun PrizeDraft.activeIds() = prizes.wheelPrizes().map { it.id }

    @Test
    fun `rearranging inside the active section changes only the order`() {
        val result = threeActive().rearrange(listOf(3L, 1L, 2L))
        assertEquals(listOf(3L, 1L, 2L), result.draft.activeIds())
        assertEquals(emptyList<Prize>(), result.pushedOut)
        assertEquals(listOf(30, 30, 40), result.draft.prizes.filter { it.active }.sortedBy { it.id }.map { it.probability })
    }

    @Test
    fun `an inactive prize dropped into the active list becomes active at that position`() {
        val result = threeActive().rearrange(listOf(1L, 4L, 2L, 3L))
        assertEquals(listOf(1L, 4L, 2L, 3L), result.draft.activeIds())
        assertTrue(result.draft.prizes.first { it.id == 4L }.active)
        assertEquals(2, result.draft.prizes.first { it.id == 4L }.wheelOrder)
    }

    @Test
    fun `an active prize missing from the active list becomes inactive and keeps its probability`() {
        val result = threeActive().rearrange(listOf(1L, 3L))
        val deactivated = result.draft.prizes.first { it.id == 2L }
        assertFalse(deactivated.active)
        assertNull(deactivated.wheelOrder)
        assertEquals(30, deactivated.probability)
        assertEquals(listOf(1L, 3L), result.draft.activeIds())
        assertEquals(listOf(1, 2), result.draft.prizes.wheelPrizes().map { it.wheelOrder })
    }

    @Test
    fun `an unchanged arrangement returns the same draft`() {
        val draft = threeActive()
        val result = draft.rearrange(listOf(1L, 2L, 3L))
        assertTrue(result.draft === draft)
        assertTrue(result.pushedOut.isEmpty())
    }

    @Test
    fun `dropping a ninth prize in the middle pushes out the prize that was last`() {
        val full = PrizeDraft(
            (1L..8L).map { prize(it, active = true, order = it.toInt(), probability = 10) } + prize(9)
        )
        val result = full.rearrange(listOf(1L, 2L, 9L, 3L, 4L, 5L, 6L, 7L, 8L))

        assertEquals(listOf(1L, 2L, 9L, 3L, 4L, 5L, 6L, 7L), result.draft.activeIds())
        assertEquals(listOf(8L), result.pushedOut.map { it.id })
        assertFalse(result.draft.prizes.first { it.id == 8L }.active)
        assertNull(result.draft.prizes.first { it.id == 8L }.wheelOrder)
        assertEquals(8, result.draft.activeCount)
    }

    @Test
    fun `dropping a ninth prize at the very end takes the last spot and pushes out the old last`() {
        val full = PrizeDraft(
            (1L..8L).map { prize(it, active = true, order = it.toInt(), probability = 10) } + prize(9)
        )
        val result = full.rearrange(listOf(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L))

        assertEquals(listOf(1L, 2L, 3L, 4L, 5L, 6L, 7L, 9L), result.draft.activeIds())
        assertEquals(listOf(8L), result.pushedOut.map { it.id })
    }

    @Test
    fun `the pushed out prize is the last on the wheel even after a reorder`() {
        val full = PrizeDraft(
            (1L..8L).map { prize(it, active = true, order = (9 - it).toInt(), probability = 10) } + prize(9)
        )
        // wheel order is 8,7,6,5,4,3,2,1 so prize 1 is last
        val result = full.rearrange(listOf(9L, 8L, 7L, 6L, 5L, 4L, 3L, 2L, 1L))

        assertEquals(listOf(1L), result.pushedOut.map { it.id })
        assertEquals(listOf(9L, 8L, 7L, 6L, 5L, 4L, 3L, 2L), result.draft.activeIds())
    }

    @Test
    fun `ids that do not exist are ignored when rearranging`() {
        val result = threeActive().rearrange(listOf(1L, 99L, 2L, 3L))
        assertEquals(listOf(1L, 2L, 3L), result.draft.activeIds())
    }

    @Test
    fun `an empty active list deactivates everything`() {
        val result = threeActive().rearrange(emptyList())
        assertEquals(0, result.draft.activeCount)
        assertTrue(result.draft.prizes.all { it.wheelOrder == null })
    }

    @Test
    fun `admin list shows wheel order first then inactive prizes alphabetically`() {
        val prizes = listOf(
            prize(1, name = "Zebra"),
            prize(2, name = "Second", active = true, order = 2),
            prize(3, name = "apple"),
            prize(4, name = "First", active = true, order = 1)
        )
        assertEquals(listOf("First", "Second", "apple", "Zebra"), prizes.forAdminList().map { it.name })
    }
}
