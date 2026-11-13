package com.smartcodeapps.constructionprizewheel.ui.admin.prizes

import com.smartcodeapps.constructionprizewheel.domain.Prize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PrizeArrangementTest {

    private val headerActive = buildArrangement(emptyList())[0]
    private val headerInactive = buildArrangement(emptyList())[1]

    private fun prize(id: Long, name: String, active: Boolean, order: Int? = null) =
        Prize(id, name, null, "/img/$id.jpg", 10, active, if (active) order else null)

    private val prizes = listOf(
        prize(1, "Hammer", true, 1),
        prize(2, "Gloves", true, 2),
        prize(3, "Drill", false),
        prize(4, "Apple", false)
    )

    private val arrangement = listOf(headerActive, 1L, 2L, headerInactive, 3L, 4L)

    @Test
    fun `arrangement lists active prizes then inactive prizes under their headers`() {
        assertEquals(arrangement, buildArrangement(prizes))
    }

    @Test
    fun `arrangement with no prizes is just the two headers`() {
        assertEquals(2, buildArrangement(emptyList()).size)
    }

    @Test
    fun `active ids are the items between the two headers`() {
        assertEquals(listOf(1L, 2L), activeIdsOf(arrangement))
    }

    @Test
    fun `active ids are empty when the active section is empty`() {
        assertEquals(emptyList<Long>(), activeIdsOf(listOf(headerActive, headerInactive, 3L)))
    }

    @Test
    fun `reordering inside the active section changes the active order`() {
        val moved = moveInArrangement(arrangement, fromKey = 2L, toKey = 1L)!!
        assertEquals(listOf(2L, 1L), activeIdsOf(moved))
    }

    @Test
    fun `dragging an active prize down past the inactive header deactivates it`() {
        val moved = moveInArrangement(arrangement, fromKey = 2L, toKey = headerInactive)!!
        assertEquals(listOf(1L), activeIdsOf(moved))
        assertEquals(listOf(headerActive, 1L, headerInactive, 2L, 3L, 4L), moved)
    }

    @Test
    fun `dragging an inactive prize up past the inactive header activates it at the end`() {
        val moved = moveInArrangement(arrangement, fromKey = 3L, toKey = headerInactive)!!
        assertEquals(listOf(1L, 2L, 3L), activeIdsOf(moved))
    }

    @Test
    fun `dragging an inactive prize onto an active one inserts it at that position`() {
        val moved = moveInArrangement(arrangement, fromKey = 3L, toKey = 1L)!!
        assertEquals(listOf(3L, 1L, 2L), activeIdsOf(moved))
    }

    @Test
    fun `an active prize can be dragged into an empty inactive section`() {
        val onlyActive = listOf(headerActive, 1L, 2L, headerInactive)
        val moved = moveInArrangement(onlyActive, fromKey = 2L, toKey = headerInactive)!!
        assertEquals(listOf(1L), activeIdsOf(moved))
    }

    @Test
    fun `an inactive prize can be dragged into an empty active section`() {
        val onlyInactive = listOf(headerActive, headerInactive, 3L)
        val moved = moveInArrangement(onlyInactive, fromKey = 3L, toKey = headerInactive)!!
        assertEquals(listOf(3L), activeIdsOf(moved))
    }

    @Test
    fun `nothing can be dragged above the active header`() {
        assertNull(moveInArrangement(arrangement, fromKey = 1L, toKey = headerActive))
    }

    @Test
    fun `headers themselves cannot be dragged`() {
        assertNull(moveInArrangement(arrangement, fromKey = headerInactive, toKey = 2L))
        assertNull(moveInArrangement(arrangement, fromKey = headerActive, toKey = 1L))
    }

    @Test
    fun `moving onto itself or an unknown key is ignored`() {
        assertNull(moveInArrangement(arrangement, fromKey = 1L, toKey = 1L))
        assertNull(moveInArrangement(arrangement, fromKey = 1L, toKey = 99L))
    }
}
