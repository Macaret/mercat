package com.smartcodeapps.constructionprizewheel.ui.admin.history

import com.smartcodeapps.constructionprizewheel.domain.SpinRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

class HistoryUiStateTest {

    private val utc = ZoneId.of("UTC")

    private fun record(
        id: Long,
        at: String,
        name: String = "Hammer",
        prizeId: Long = 10
    ) = SpinRecord(id, Instant.parse(at), prizeId, name)

    @Test
    fun `date and time are formatted separately`() {
        val entry = record(1, "2026-11-13T14:05:09Z").toHistoryEntry(utc, Locale.ENGLISH)
        assertEquals("13 Nov 2026", entry.date)
        assertEquals("14:05:09", entry.time)
    }

    @Test
    fun `the time zone shifts the displayed date and time`() {
        val entry = record(1, "2026-11-13T23:30:00Z").toHistoryEntry(ZoneId.of("Europe/Bucharest"), Locale.ENGLISH)
        assertEquals("14 Nov 2026", entry.date)
        assertEquals("01:30:00", entry.time)
    }

    @Test
    fun `entry keeps the prize name snapshot and record id`() {
        val entry = record(7, "2026-11-13T10:00:00Z", name = "Deleted Prize").toHistoryEntry(utc, Locale.ENGLISH)
        assertEquals(7L, entry.id)
        assertEquals("Deleted Prize", entry.prizeName)
    }

    @Test
    fun `entries keep the order of the records`() {
        val state = buildHistoryUiState(
            listOf(record(3, "2026-11-13T12:00:00Z"), record(2, "2026-11-13T11:00:00Z"), record(1, "2026-11-13T10:00:00Z")),
            utc,
            Locale.ENGLISH
        )
        assertEquals(listOf(3L, 2L, 1L), state.entries.map { it.id })
    }

    @Test
    fun `count is the total number of records`() {
        val state = buildHistoryUiState(
            listOf(
                record(1, "2026-11-13T10:00:00Z"),
                record(2, "2026-11-13T11:00:00Z"),
                record(3, "2026-11-13T12:00:00Z")
            ),
            utc,
            Locale.ENGLISH
        )
        assertEquals(3, state.totalCount)
    }

    @Test
    fun `an empty history is loaded with zero count`() {
        val state = buildHistoryUiState(emptyList(), utc, Locale.ENGLISH)
        assertTrue(state.loaded)
        assertEquals(0, state.totalCount)
    }

    @Test
    fun `initial state is not loaded`() {
        assertFalse(HistoryUiState().loaded)
    }

    @Test
    fun `prize totals count wins per prize, most won first`() {
        val totals = buildPrizeTotals(
            listOf(
                record(1, "2026-11-13T10:00:00Z", name = "Gloves", prizeId = 20),
                record(2, "2026-11-13T11:00:00Z", name = "Hammer", prizeId = 10),
                record(3, "2026-11-13T12:00:00Z", name = "Hammer", prizeId = 10),
                record(4, "2026-11-13T13:00:00Z", name = "Hammer", prizeId = 10)
            )
        )
        assertEquals(listOf(PrizeTotal(10, "Hammer", 3), PrizeTotal(20, "Gloves", 1)), totals)
    }

    @Test
    fun `prize totals break ties by name`() {
        val totals = buildPrizeTotals(
            listOf(
                record(1, "2026-11-13T10:00:00Z", name = "Zebra", prizeId = 1),
                record(2, "2026-11-13T11:00:00Z", name = "Apple", prizeId = 2)
            )
        )
        assertEquals(listOf("Apple", "Zebra"), totals.map { it.prizeName })
    }

    @Test
    fun `a renamed prize is grouped by id and shows its latest name`() {
        val totals = buildPrizeTotals(
            listOf(
                record(1, "2026-11-13T10:00:00Z", name = "Old name", prizeId = 5),
                record(2, "2026-11-13T12:00:00Z", name = "New name", prizeId = 5)
            )
        )
        assertEquals(listOf(PrizeTotal(5, "New name", 2)), totals)
    }

    @Test
    fun `an empty history has no prize totals`() {
        assertTrue(buildHistoryUiState(emptyList(), utc, Locale.ENGLISH).prizeTotals.isEmpty())
    }
}
