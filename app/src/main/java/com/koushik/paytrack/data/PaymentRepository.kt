package com.koushik.paytrack.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date

class PaymentRepository(context: Context) {
    private val dao = AppDatabase.get(context).paymentDao()

    fun observeSummaries() = dao.observeSummaries()
    fun observeRecentTransactions() = dao.observeRecentTransactions()

    /**
     * Saves the transaction locally (audit trail + offline-safe record), updates today's local
     * cache for the app's own UI, then pushes just the delta to the sheet — the sheet script
     * does its own read-add-write against the real row, using its full month history to compute
     * the month-to-date total, since the phone never has that history.
     */
    suspend fun recordPayment(amount: Double, merchant: String?, app: String, category: Category, postedAt: Long) {
        val date = Date(postedAt)
        val dateKey = DateKeys.keyFor(date)

        dao.insertTransaction(
            PaymentTransaction(
                dateKey = dateKey,
                amount = amount,
                merchant = merchant,
                app = app,
                category = category,
                postedAt = postedAt,
            ),
        )

        val existing = dao.getSummary(dateKey)
        val base = existing ?: DailySummary(dateKey = dateKey, displayDate = DateKeys.displayFor(date))
        dao.upsertSummary(base.withAdded(category, amount))

        withContext(Dispatchers.IO) {
            SheetSyncClient.pushDelta(DateKeys.displayFor(date), category, amount)
        }
    }

    /** Moves a transaction to a different category, adjusting both the local cache and the sheet. */
    suspend fun correctCategory(transactionId: Long, newCategory: Category) {
        val transaction = dao.getTransaction(transactionId) ?: return
        if (transaction.category == newCategory) return

        dao.updateTransactionCategory(transactionId, newCategory)

        val existing = dao.getSummary(transaction.dateKey)
        if (existing != null) {
            val adjusted = existing
                .withAdded(transaction.category, -transaction.amount)
                .withAdded(newCategory, transaction.amount)
            dao.upsertSummary(adjusted)
        }

        val displayDate = DateKeys.displayFor(Date(transaction.postedAt))
        withContext(Dispatchers.IO) {
            SheetSyncClient.pushDelta(displayDate, transaction.category, -transaction.amount)
            SheetSyncClient.pushDelta(displayDate, newCategory, transaction.amount)
        }
    }
}
