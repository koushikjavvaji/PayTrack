package com.koushik.paytrack.data

/** How much a category has taken so far this month, and its budget limit if one is set. */
data class CategorySpend(val spentSoFar: Double, val limit: Double?)
