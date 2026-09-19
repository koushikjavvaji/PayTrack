package com.koushik.paytrack.notification

/**
 * Best-effort text parser for GPay/Paytm payment notifications.
 *
 * Notification copy differs across app versions (e.g. "You paid ₹355 to Vinayak Sandur",
 * "₹100 paid to Merchant using Bank Account", "Money sent ₹50 to John"), so this tries a
 * few regex shapes rather than one strict pattern.
 */
object PaymentNotificationParser {

    private val AMOUNT_REGEX = Regex("""(?:₹|Rs\.?|INR)\s?([0-9][0-9,]*(?:\.[0-9]{1,2})?)""", RegexOption.IGNORE_CASE)

    private val MERCHANT_REGEXES = listOf(
        Regex("""\bto\s+([A-Za-z0-9&.,'\-\s]+?)(?:\s+using\b|\s+from\b|\s+via\b|[.!]|$)""", RegexOption.IGNORE_CASE),
        Regex("""\bpaid\s+to\s+([A-Za-z0-9&.,'\-\s]+?)(?:\s+using\b|\s+via\b|[.!]|$)""", RegexOption.IGNORE_CASE),
    )

    /** Only GPay/Paytm notifications that look like an outgoing payment are worth parsing. */
    private val PAYMENT_KEYWORDS = listOf("paid", "payment", "sent", "debited")

    fun parse(packageName: String, title: String?, text: String?, key: String, postedAt: Long): PaymentInfo? {
        if (packageName !in PaymentApps.PACKAGE_NAMES) return null

        val combined = listOfNotNull(title, text).joinToString(" ")
        if (combined.isBlank()) return null
        if (PAYMENT_KEYWORDS.none { combined.contains(it, ignoreCase = true) }) return null

        val amount = AMOUNT_REGEX.find(combined)
            ?.groupValues
            ?.getOrNull(1)
            ?.replace(",", "")
            ?.toDoubleOrNull()

        val merchant = MERCHANT_REGEXES.firstNotNullOfOrNull { regex ->
            regex.find(combined)?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() }
        }

        if (amount == null && merchant == null) return null

        return PaymentInfo(
            app = PaymentApps.displayName(packageName),
            amount = amount,
            merchant = merchant,
            rawTitle = title.orEmpty(),
            rawText = text.orEmpty(),
            postedAt = postedAt,
            notificationKey = key,
        )
    }
}
