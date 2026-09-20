package com.koushik.paytrack.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DinnerDining
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FreeBreakfast
import androidx.compose.material.icons.filled.Icecream
import androidx.compose.material.icons.filled.LunchDining
import androidx.compose.material.icons.filled.Payments
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import com.koushik.paytrack.data.Category
import java.text.NumberFormat
import java.util.Locale

/** A distinct accent color per category, used as a quick visual identifier across the app. */
fun Category.accentColor(): Color = when (this) {
    Category.BREAKFAST -> Color(0xFFE08A1E)
    Category.LUNCH -> Color(0xFFE0562F)
    Category.DINNER -> Color(0xFF7C4DD6)
    Category.SNACKS -> Color(0xFF1E9E8C)
    Category.TRAVEL -> Color(0xFF2F7DE0)
    Category.OTHER_EXPENSES -> Color(0xFF75665A)
}

fun Category.icon(): ImageVector = when (this) {
    Category.BREAKFAST -> Icons.Filled.FreeBreakfast
    Category.LUNCH -> Icons.Filled.LunchDining
    Category.DINNER -> Icons.Filled.DinnerDining
    Category.SNACKS -> Icons.Filled.Icecream
    Category.TRAVEL -> Icons.Filled.DirectionsCar
    Category.OTHER_EXPENSES -> Icons.Filled.Payments
}

/** Picks readable black/white text for an arbitrary fill color, rather than assuming a fixed shade works for all six accents. */
fun Color.readableOnColor(): Color = if (luminance() > 0.45f) Color.Black else Color.White

private val rupeeFormat: NumberFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
    maximumFractionDigits = 2
}

fun formatRupees(amount: Double): String = rupeeFormat.format(amount)
