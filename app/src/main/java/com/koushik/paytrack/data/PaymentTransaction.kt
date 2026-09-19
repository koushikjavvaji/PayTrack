package com.koushik.paytrack.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** One categorized payment, kept as an audit log even though the sheet only sees daily totals. */
@Entity(tableName = "payment_transactions")
data class PaymentTransaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateKey: String,
    val amount: Double,
    val merchant: String?,
    val app: String,
    val category: Category,
    val postedAt: Long,
)
