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
        if (packageName !in PaymentApps.PACKAGE_NAMES) return

        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text = (extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
            ?: extras.getCharSequence(Notification.EXTRA_TEXT))?.toString()

        Log.d(TAG, "Notification from $packageName -> title=\"$title\" text=\"$text\"")

        val payment = PaymentNotificationParser.parse(
            packageName = packageName,
            title = title,
            text = text,
            key = sbn.key,
            postedAt = sbn.postTime,
        ) ?: return

        val amount = payment.amount
        Log.i(TAG, "Detected payment: app=${payment.app} amount=$amount merchant=${payment.merchant}")
        if (amount == null) return

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

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        super.onNotificationRemoved(sbn)
    }

    companion object {
        private const val TAG = "PayTrack"
    }
}
