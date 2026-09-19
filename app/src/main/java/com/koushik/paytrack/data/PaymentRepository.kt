package com.koushik.paytrack.data

import android.content.Context
import java.util.Date

class PaymentRepository(context: Context) {
    private val dao = AppDatabase.get(context).paymentDao()
    private val prefs = AppPreferences(context)

    fun observeSummaries() = dao.observeSummaries()
    fun observeRecentTransactions() = dao.observeRecentTransactions()

    /** Does today's row exist? Add to it; otherwise create it. Then roll the running total forward. */
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
        val updated = base.withAdded(category, amount)

        val previousTotal = dao.getLatestSummaryBefore(dateKey)?.totalSpentTillDate ?: prefs.baselineTotal
        dao.upsertSummary(updated.copy(totalSpentTillDate = previousTotal + updated.total))
    }
}
