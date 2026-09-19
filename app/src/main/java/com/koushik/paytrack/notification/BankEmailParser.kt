package com.koushik.paytrack.notification

/**
 * Parses Axis Bank's debit email, delivered through Gmail's notification. Gmail synthesizes its
 * own plain-text summary from the email's text/plain part, which reads differently from the
 * rendered HTML email:
 *
 *   INR 1.00 was debited from your A/c no. XX7180.
 *   ...
 *   Transaction Info:
 *   UPI/P2A/662814405573/Garipelly Sai Nayan
 *
 * In testing this came through in full (not truncated to a short snippet), and more reliably
 * than both GPay/Paytm's own notification and the bank SMS — so this is a primary source, not
 * just a backup, despite the class name.
 */
object BankEmailParser {

    private val AMOUNT_REGEX = Regex(
        """INR\s+([0-9][0-9,]*(?:\.[0-9]{1,2})?)\s+was\s+debited""",
        RegexOption.IGNORE_CASE,
    )
    private val UPI_PAYEE_REGEX = Regex("""UPI/[A-Za-z0-9]+/\d+/([^\n\r]+)""")

    fun parse(packageName: String, title: String?, text: String?, key: String, postedAt: Long): PaymentInfo? {
        if (packageName !in EmailApps.PACKAGE_NAMES) return null

        val body = listOfNotNull(title, text).joinToString("\n")
        if (!body.contains("debited", ignoreCase = true)) return null

        val amount = AMOUNT_REGEX.find(body)?.groupValues?.getOrNull(1)?.replace(",", "")?.toDoubleOrNull()
            ?: return null

        val merchant = UPI_PAYEE_REGEX.find(body)?.groupValues?.getOrNull(1)?.trim()

        return PaymentInfo(
            app = "Bank Email",
            amount = amount,
            merchant = merchant,
            rawTitle = title.orEmpty(),
            rawText = text.orEmpty(),
            postedAt = postedAt,
            notificationKey = key,
        )
    }
}
