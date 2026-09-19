package com.koushik.paytrack.notification

data class PaymentInfo(
    val app: String,
    val amount: Double?,
    val merchant: String?,
    val rawTitle: String,
    val rawText: String,
    val postedAt: Long,
    val notificationKey: String,
)
