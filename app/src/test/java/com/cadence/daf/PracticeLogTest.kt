package com.cadence.daf

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class PracticeLogTest {
    private val today = LocalDate.of(2026, 11, 8)
    private val lines = listOf(
        "2026-11-08,600", "2026-11-08,59", "2026-11-07,1200", "2026-11-02,1200",
        "2026-11-01,6000", "2026-10-01,3600", "2026-11-09,600", "bad line"
    )

    @Test
    fun dailyMinutesLastSeven() {
        val days = dailyMinutes(lines, today, 7)
        assertEquals(7, days.size)
        assertEquals(DayTotal(LocalDate.of(2026, 11, 2), 20), days.first())
        assertEquals(20, days.first { it.date == LocalDate.of(2026, 11, 7) }.minutes)
        assertEquals(DayTotal(today, 10), days.last())
        assertEquals(4, days.count { it.minutes == 0 })
    }

    @Test
    fun stats() {
        assertEquals(PracticeStats(10, 50, 3, 150, 4, 210), practiceStats(lines, today))
    }

    @Test
    fun emptyLog() {
        assertEquals(PracticeStats(0, 0, 0, 0, 0, 0), practiceStats(emptyList(), today))
    }

    @Test
    fun goalReachedNeedsGoalOnAndEnoughMinutes() {
        assertEquals(true, goalReached(10, 10))
        assertEquals(false, goalReached(9, 10))
        assertEquals(false, goalReached(30, 0))
    }

    @Test
    fun heatmapRowsAlignToMondayAndPadWeeks() {
        val rows = heatmapRows(dailyMinutes(emptyList(), today, 30))
        assertEquals(5, rows.size)
        assertEquals(null, rows[0][0])
        assertEquals(LocalDate.of(2026, 10, 10), rows[0][5]?.date)
        assertEquals(today, rows[4][6]?.date)
    }
}
