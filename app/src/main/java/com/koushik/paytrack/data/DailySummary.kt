package com.koushik.paytrack.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A day's spend broken down by category — a local, best-effort mirror of that day so far.
 * The month-to-date running total lives only in the Google Sheet, computed there from the full
 * month's history (which this local cache never has), not tracked here.
 */
@Entity(tableName = "daily_summaries")
data class DailySummary(
    @PrimaryKey val dateKey: String,
    val displayDate: String,
    val breakfast: Double = 0.0,
    val lunch: Double = 0.0,
    val dinner: Double = 0.0,
    val snacks: Double = 0.0,
    val travel: Double = 0.0,
    val otherExpenses: Double = 0.0,
    val total: Double = 0.0,
) {
    fun amountFor(category: Category): Double = when (category) {
        Category.BREAKFAST -> breakfast
        Category.LUNCH -> lunch
        Category.DINNER -> dinner
        Category.SNACKS -> snacks
        Category.TRAVEL -> travel
        Category.OTHER_EXPENSES -> otherExpenses
    }

    fun withAdded(category: Category, amount: Double): DailySummary {
        val updated = when (category) {
            Category.BREAKFAST -> copy(breakfast = breakfast + amount)
            Category.LUNCH -> copy(lunch = lunch + amount)
            Category.DINNER -> copy(dinner = dinner + amount)
            Category.SNACKS -> copy(snacks = snacks + amount)
            Category.TRAVEL -> copy(travel = travel + amount)
            Category.OTHER_EXPENSES -> copy(otherExpenses = otherExpenses + amount)
        }
        return updated.copy(
            total = updated.breakfast + updated.lunch + updated.dinner +
                updated.snacks + updated.travel + updated.otherExpenses,
        )
    }
}
