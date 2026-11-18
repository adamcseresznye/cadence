package com.cadence.daf

/** Never goes below [targetMs] (or [startMs] when that is lower); drops [stepMs] every [intervalDays] days. */
fun programDelayMs(startMs: Int, stepMs: Int, startDay: Long, today: Long, targetMs: Int = 40, intervalDays: Int = 7): Int =
    maxOf(minOf(targetMs, startMs), startMs - stepMs * Math.floorDiv(today - startDay, intervalDays.toLong()).toInt())

/** The start day that puts the program one step back (a higher delay); unchanged when no step has been taken yet. */
fun stepBackStartDay(startMs: Int, stepMs: Int, startDay: Long, day: Long, targetMs: Int, intervalDays: Int): Long {
    val steps = Math.floorDiv(day - startDay, intervalDays.toLong())
    val reachable = if (stepMs > 0) Math.max(0, (startMs - targetMs + stepMs - 1) / stepMs).toLong() else 0L
    val taken = minOf(steps, reachable)
    return if (taken <= 0) startDay else day - (taken - 1) * intervalDays
}
