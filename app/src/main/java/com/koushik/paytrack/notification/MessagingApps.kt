package com.koushik.paytrack.notification

/** Package(s) whose notifications may contain bank debit SMS text. */
object MessagingApps {
    const val GOOGLE_MESSAGES = "com.google.android.apps.messaging"

    val PACKAGE_NAMES = setOf(GOOGLE_MESSAGES)
}
