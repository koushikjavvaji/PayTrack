package com.koushik.paytrack.notification

/**
 * Parses bank debit SMS text delivered through the Messages app notification, e.g. (Axis Bank):
 *
 *   INR 40.00 debited
 *   A/c no. XX7180
 *   17-09-26, 10:02:56
 *   UPI/P2M/626027951234/DHRUMI ENTERPRISES
 *   Not you? SMS BLOCKUPI Cust ID to 919951860002
 *   Axis Bank
 *
 * Bank SMS is more reliable than GPay/Paytm's own notification (RBI made SMS alerts optional
 * below ₹500 in July 2026, but Axis still sends them for every UPI debit regardless of amount),
 * so this runs as a second, independent source alongside PaymentNotificationParser.
 */
object BankSmsParser {

    private val AMOUNT_REGEX = Regex("""INR\s+([0-9][0-9,]*(?:\.[0-9]{1,2})?)\s+debited""", RegexOption.IGNORE_CASE)
    private val UPI_PAYEE_REGEX = Regex("""UPI/[A-Za-z0-9]+/\d+/([^\n\r]+)""")

    fun parse(packageName: String, title: String?, text: String?, key: String, postedAt: Long): PaymentInfo? {
        if (packageName !in MessagingApps.PACKAGE_NAMES) return null

        val body = listOfNotNull(title, text).joinToString("\n")
        if (!body.contains("debited", ignoreCase = true)) return null

        val amount = AMOUNT_REGEX.find(body)?.groupValues?.getOrNull(1)?.replace(",", "")?.toDoubleOrNull()
            ?: return null

        val merchant = UPI_PAYEE_REGEX.find(body)?.groupValues?.getOrNull(1)?.trim()

        return PaymentInfo(
            app = "Bank SMS",
            amount = amount,
            merchant = merchant,
            rawTitle = title.orEmpty(),
            rawText = text.orEmpty(),
            postedAt = postedAt,
            notificationKey = key,
        )
    }
}
