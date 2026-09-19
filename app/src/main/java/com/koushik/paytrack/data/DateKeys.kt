package com.koushik.paytrack.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** dateKey sorts lexicographically by day; displayDate matches the sheet's "19th September" style. */
object DateKeys {
    private val keyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val monthFormat = SimpleDateFormat("MMMM", Locale.US)

    fun keyFor(date: Date): String = keyFormat.format(date)

    fun displayFor(date: Date): String {
        val calendar = java.util.Calendar.getInstance().apply { time = date }
        val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)
        val suffix = when {
            day in 11..13 -> "th"
            day % 10 == 1 -> "st"
            day % 10 == 2 -> "nd"
            day % 10 == 3 -> "rd"
            else -> "th"
        }
        return "$day$suffix ${monthFormat.format(date)}"
    }
}
