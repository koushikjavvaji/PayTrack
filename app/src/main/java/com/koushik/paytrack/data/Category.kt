package com.koushik.paytrack.data

/** Mirrors the category columns of the Google Sheet exactly, in sheet column order. */
enum class Category(val label: String, val sheetKey: String) {
    BREAKFAST("Breakfast", "breakfast"),
    LUNCH("Lunch", "lunch"),
    DINNER("Dinner", "dinner"),
    SNACKS("Snacks", "snacks"),
    TRAVEL("Travel", "travel"),
    OTHER_EXPENSES("Other Expenses", "otherExpenses"),
}
