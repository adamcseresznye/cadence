package com.cadence.daf

fun programDelayMs(startMs: Int, stepMs: Int, startDay: Long, today: Long): Int =
    maxOf(40, startMs - stepMs * Math.floorDiv(today - startDay, 7L).toInt())
