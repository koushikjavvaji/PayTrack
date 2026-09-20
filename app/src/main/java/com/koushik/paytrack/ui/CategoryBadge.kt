package com.koushik.paytrack.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.koushik.paytrack.data.Category

/** A soft tinted circle with the category's icon centered in it — the fintech-app "category chip" look. */
@Composable
fun CategoryBadge(category: Category, size: Dp = 40.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(category.accentColor().copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = category.icon(),
            contentDescription = category.label,
            tint = category.accentColor(),
            modifier = Modifier.size(size * 0.55f),
        )
    }
}
