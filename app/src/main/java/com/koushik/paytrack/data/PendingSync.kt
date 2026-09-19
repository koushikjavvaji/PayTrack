package com.koushik.paytrack.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A sheet delta that failed to send (no network, endpoint down, etc.), waiting to be retried. */
@Entity(tableName = "pending_syncs")
data class PendingSync(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val displayDate: String,
    val category: Category,
    val amount: Double,
    val createdAt: Long,
)
