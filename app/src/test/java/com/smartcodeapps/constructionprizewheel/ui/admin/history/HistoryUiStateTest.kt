package com.smartcodeapps.constructionprizewheel.ui.admin.history

import com.smartcodeapps.constructionprizewheel.domain.RedemptionStatus
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
        status: RedemptionStatus = RedemptionStatus.NOT_REDEEMED
    ) = SpinRecord(id, Instant.parse(at), 10, name, status)

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
    fun `redemption status maps to a boolean`() {
        assertFalse(record(1, "2026-11-13T10:00:00Z").toHistoryEntry(utc, Locale.ENGLISH).redeemed)
        assertTrue(
            record(2, "2026-11-13T10:00:00Z", status = RedemptionStatus.REDEEMED)
                .toHistoryEntry(utc, Locale.ENGLISH).redeemed
        )
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
    fun `counts include total and not redeemed`() {
        val state = buildHistoryUiState(
            listOf(
                record(1, "2026-11-13T10:00:00Z"),
                record(2, "2026-11-13T11:00:00Z", status = RedemptionStatus.REDEEMED),
                record(3, "2026-11-13T12:00:00Z")
            ),
            utc,
            Locale.ENGLISH
        )
        assertEquals(3, state.totalCount)
        assertEquals(2, state.notRedeemedCount)
    }

    @Test
    fun `an empty history is loaded with zero counts`() {
        val state = buildHistoryUiState(emptyList(), utc, Locale.ENGLISH)
        assertTrue(state.loaded)
        assertEquals(0, state.totalCount)
        assertEquals(0, state.notRedeemedCount)
    }

    @Test
    fun `initial state is not loaded`() {
        assertFalse(HistoryUiState().loaded)
    }
}
