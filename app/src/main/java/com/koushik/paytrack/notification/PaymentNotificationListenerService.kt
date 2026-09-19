package com.koushik.paytrack.notification

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.koushik.paytrack.data.PaymentRepository
import com.koushik.paytrack.popup.PaymentPopupOverlay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class PaymentNotificationListenerService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var repository: PaymentRepository

    /** Same payment can be reported by both the payment app and the bank SMS; drop the second. */
    private val recentAmounts = ArrayDeque<Pair<Double, Long>>()

    override fun onCreate() {
        super.onCreate()
        repository = PaymentRepository(applicationContext)
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "Notification listener connected")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)

        val packageName = sbn.packageName
        Log.v(TAG, "Notification posted from $packageName")

        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text = (extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
            ?: extras.getCharSequence(Notification.EXTRA_TEXT))?.toString()

        val isTrackedSource = packageName in PaymentApps.PACKAGE_NAMES ||
            packageName in MessagingApps.PACKAGE_NAMES ||
            packageName in EmailApps.PACKAGE_NAMES
        if (isTrackedSource) {
            Log.d(TAG, "Notification from $packageName -> title=\"$title\" text=\"$text\"")
        }

        val payment = when (packageName) {
            in PaymentApps.PACKAGE_NAMES ->
                PaymentNotificationParser.parse(packageName, title, text, sbn.key, sbn.postTime)
            in MessagingApps.PACKAGE_NAMES ->
                BankSmsParser.parse(packageName, title, text, sbn.key, sbn.postTime)
            in EmailApps.PACKAGE_NAMES ->
                BankEmailParser.parse(packageName, title, text, sbn.key, sbn.postTime)
            else -> null
        } ?: return

        val amount = payment.amount
        Log.i(TAG, "Detected payment: app=${payment.app} amount=$amount merchant=${payment.merchant}")
        if (amount == null || isDuplicate(amount)) return

        PaymentPopupOverlay.show(applicationContext, payment) { category ->
            if (category == null) return@show
            serviceScope.launch {
                repository.recordPayment(
                    amount = amount,
                    merchant = payment.merchant,
                    app = payment.app,
                    category = category,
                    postedAt = payment.postedAt,
                )
            }
        }
    }

    private fun isDuplicate(amount: Double): Boolean {
        val now = System.currentTimeMillis()
        while (recentAmounts.isNotEmpty() && now - recentAmounts.first().second > DEDUP_WINDOW_MS) {
            recentAmounts.removeFirst()
        }
        val isDuplicate = recentAmounts.any { it.first == amount }
        if (!isDuplicate) recentAmounts.addLast(amount to now)
        return isDuplicate
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        super.onNotificationRemoved(sbn)
    }

    companion object {
        private const val TAG = "PayTrack"
        private const val DEDUP_WINDOW_MS = 20_000L
    }
}
