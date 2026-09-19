package com.koushik.paytrack.notification

/** Package names of apps whose payment notifications we listen for. */
object PaymentApps {
    const val GPAY = "com.google.android.apps.nbu.paisa.user"
    const val PAYTM = "net.one97.paytm"

    val PACKAGE_NAMES = setOf(GPAY, PAYTM)

    fun displayName(packageName: String): String = when (packageName) {
        GPAY -> "Google Pay"
        PAYTM -> "Paytm"
        else -> packageName
    }
}
