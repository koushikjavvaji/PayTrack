package com.koushik.paytrack.data

/** Mirrors the category columns of the Google Sheet exactly, in sheet column order. */
enum class Category(val label: String) {
    BREAKFAST("Breakfast"),
    LUNCH("Lunch"),
    DINNER("Dinner"),
    SNACKS("Snacks"),
    TRAVEL("Travel"),
    OTHER_EXPENSES("Other Expenses"),
}
