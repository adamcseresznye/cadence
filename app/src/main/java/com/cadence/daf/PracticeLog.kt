package com.cadence.daf

import java.time.LocalDate
import java.time.YearMonth

data class DayTotal(val date: LocalDate, val minutes: Int)

data class PracticeStats(
    val todayMin: Int,
    val weekMin: Int,
    val weekDays: Int,
    val monthMin: Int,
    val monthDays: Int,
    val allTimeMin: Int,
)

// Seconds per day. Malformed lines and lines dated after today are skipped.
private fun secondsByDay(lines: List<String>, today: LocalDate): Map<LocalDate, Long> {
    val sums = HashMap<LocalDate, Long>()
    for (line in lines) {
        val parts = line.split(",")
        val date = runCatching { LocalDate.parse(parts[0]) }.getOrNull() ?: continue
        val sec = parts.getOrNull(1)?.toLongOrNull() ?: continue
        if (!date.isAfter(today)) sums.merge(date, sec, Long::plus)
    }
    return sums
}

private fun Map<LocalDate, Long>.lastDays(today: LocalDate, days: Int): List<Long> =
    (days - 1 downTo 0).map { this[today.minusDays(it.toLong())] ?: 0L }

/** Minutes for every day that has a log entry. */
fun minutesByDay(lines: List<String>, today: LocalDate): Map<LocalDate, Int> =
    secondsByDay(lines, today).mapValues { (it.value / 60).toInt() }

/** Every day of [month] up to today, oldest first. */
fun monthDays(minutes: Map<LocalDate, Int>, month: YearMonth, today: LocalDate): List<DayTotal> =
    (1..month.lengthOfMonth()).map { month.atDay(it) }.filter { !it.isAfter(today) }.map { DayTotal(it, minutes[it] ?: 0) }

fun practiceStats(lines: List<String>, today: LocalDate): PracticeStats {
    val sums = secondsByDay(lines, today)
    val week = sums.lastDays(today, 7)
    val month = sums.lastDays(today, 30)
    return PracticeStats(
        todayMin = ((sums[today] ?: 0L) / 60).toInt(),
        weekMin = (week.sum() / 60).toInt(),
        weekDays = week.count { it > 0 },
        monthMin = (month.sum() / 60).toInt(),
        monthDays = month.count { it > 0 },
        allTimeMin = (sums.values.sum() / 60).toInt(),
    )
}
