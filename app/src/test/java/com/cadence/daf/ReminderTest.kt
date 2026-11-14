package com.cadence.daf

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class ReminderTest {
    @Test
    fun laterTodayWhenTimeIsAhead() {
        val now = LocalDateTime.of(2026, 11, 13, 9, 30)
        assertEquals(LocalDateTime.of(2026, 11, 13, 18, 0), nextReminderAt(now, 18 * 60))
    }

    @Test
    fun tomorrowWhenTimeHasPassed() {
        val now = LocalDateTime.of(2026, 11, 13, 18, 0)
        assertEquals(LocalDateTime.of(2026, 11, 14, 18, 0), nextReminderAt(now, 18 * 60))
    }

    @Test
    fun crossesMonthEnd() {
        val now = LocalDateTime.of(2026, 11, 30, 23, 0)
        assertEquals(LocalDateTime.of(2026, 12, 1, 7, 15), nextReminderAt(now, 7 * 60 + 15))
    }

    @Test
    fun remindsUntilGoalIsReached() {
        assertTrue(shouldRemind(todayMin = 9, goalMin = 10))
        assertFalse(shouldRemind(todayMin = 10, goalMin = 10))
    }

    @Test
    fun goalOffRemindsOnlyWithoutPractice() {
        assertTrue(shouldRemind(todayMin = 0, goalMin = 0))
        assertFalse(shouldRemind(todayMin = 1, goalMin = 0))
    }
}
