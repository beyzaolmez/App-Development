package com.nhlstenden.momentum.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Thread-safe date formatting wrappers.
 *
 * SimpleDateFormat is NOT thread-safe — concurrent access from multiple coroutine
 * threads can corrupt output. We use ThreadLocal so each thread gets its own instance.
 */
object MomentumDateFormat {

    private val isoDateFormat = ThreadLocal.withInitial {
        SimpleDateFormat("yyyy-MM-dd", Locale.US)
    }

    private val isoDateTimeFormat = ThreadLocal.withInitial {
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    }

    private val historyDateFormat = ThreadLocal.withInitial {
        SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
    }

    private val weekdayFormat = ThreadLocal.withInitial {
        SimpleDateFormat("EEEE", Locale.getDefault())
    }

    /** Format a Date as "yyyy-MM-dd" (UTC/Locale.US). */
    fun formatIso(date: Date): String = isoDateFormat.get().format(date)

    /** Parse a string in "yyyy-MM-dd" format. */
    fun parseIso(string: String): Date? = isoDateFormat.get().parse(string)

    /** Format a Date as "yyyy-MM-dd HH:mm" (Locale default). */
    fun formatIsoDateTime(date: Date): String = isoDateTimeFormat.get().format(date)

    /** Format a timestamp as "MMM d, HH:mm" for history display. */
    fun formatHistory(timestamp: Long): String = historyDateFormat.get().format(Date(timestamp))

    /** Format current date as weekday name (e.g. "Monday"). */
    fun formatWeekday(): String = weekdayFormat.get().format(Date())
}
