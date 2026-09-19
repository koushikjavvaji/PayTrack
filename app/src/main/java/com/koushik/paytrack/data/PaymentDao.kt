package com.koushik.paytrack.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentDao {
    @Insert
    suspend fun insertTransaction(transaction: PaymentTransaction)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSummary(summary: DailySummary)

    @Query("SELECT * FROM daily_summaries WHERE dateKey = :dateKey LIMIT 1")
    suspend fun getSummary(dateKey: String): DailySummary?

    @Query("SELECT * FROM daily_summaries WHERE dateKey < :dateKey ORDER BY dateKey DESC LIMIT 1")
    suspend fun getLatestSummaryBefore(dateKey: String): DailySummary?

    @Query("SELECT * FROM daily_summaries ORDER BY dateKey DESC")
    fun observeSummaries(): Flow<List<DailySummary>>

    @Query("SELECT * FROM payment_transactions ORDER BY postedAt DESC LIMIT 50")
    fun observeRecentTransactions(): Flow<List<PaymentTransaction>>

    @Query("SELECT * FROM payment_transactions WHERE id = :id LIMIT 1")
    suspend fun getTransaction(id: Long): PaymentTransaction?

    @Query("UPDATE payment_transactions SET category = :newCategory WHERE id = :id")
    suspend fun updateTransactionCategory(id: Long, newCategory: Category)

    @Insert
    suspend fun insertPendingSync(pendingSync: PendingSync)

    @Query("SELECT * FROM pending_syncs ORDER BY createdAt ASC")
    suspend fun getPendingSyncs(): List<PendingSync>

    @Query("DELETE FROM pending_syncs WHERE id = :id")
    suspend fun deletePendingSync(id: Long)
}
