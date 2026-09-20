package com.koushik.paytrack.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A recurring monthly spending limit for one category. Local-only — never synced to the sheet. */
@Entity(tableName = "budgets")
data class Budget(
    @PrimaryKey val category: Category,
    val monthlyLimit: Double,
)
