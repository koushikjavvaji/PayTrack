package com.koushik.paytrack.data

import android.content.Context

/**
 * One-time seed for "Total Spent Till Date": the running total from before this app started
 * tracking (₹89,545.70 as of 13th July in your sheet). Everything after is computed as
 * previousDay.totalSpentTillDate + today.total.
 */
class AppPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("paytrack_prefs", Context.MODE_PRIVATE)

    var baselineTotal: Double
        get() = java.lang.Double.longBitsToDouble(prefs.getLong(KEY_BASELINE, 0L))
        set(value) = prefs.edit().putLong(KEY_BASELINE, java.lang.Double.doubleToLongBits(value)).apply()

    companion object {
        private const val KEY_BASELINE = "baseline_total"
    }
}
