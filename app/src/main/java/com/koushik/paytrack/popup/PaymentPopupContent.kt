package com.koushik.paytrack.popup

import android.os.Handler
import android.os.Looper
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.koushik.paytrack.data.Category
import com.koushik.paytrack.data.CategorySpend
import com.koushik.paytrack.notification.PaymentInfo
import com.koushik.paytrack.ui.CategoryBadge
import com.koushik.paytrack.ui.accentColor
import com.koushik.paytrack.ui.formatRupees
import com.koushik.paytrack.ui.theme.PayTrackTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val CATEGORY_ROWS = listOf(
    Category.BREAKFAST to Category.LUNCH,
    Category.DINNER to Category.SNACKS,
    Category.TRAVEL to Category.OTHER_EXPENSES,
)

/** How long the exit animation gets to play before the overlay window is actually torn down. */
private const val EXIT_ANIMATION_MS = 180L

/** How long the "you're over budget" state stays on screen before the popup auto-dismisses. */
private const val BREACH_DISPLAY_MS = 3000L

private data class BudgetBreach(val category: Category, val projected: Double, val limit: Double)

@Composable
fun PaymentPopupContent(
    payment: PaymentInfo,
    budgetStatus: Map<Category, CategorySpend> = emptyMap(),
    onCategorySelected: (Category) -> Unit,
    onSkip: () -> Unit,
) {
    PayTrackTheme {
        var visible by remember { mutableStateOf(false) }
        var breach by remember { mutableStateOf<BudgetBreach?>(null) }
        val shakeOffset = remember { Animatable(0f) }
        val haptics = LocalHapticFeedback.current
        LaunchedEffect(Unit) { visible = true }

        fun dismissWith(action: () -> Unit) {
            visible = false
            Handler(Looper.getMainLooper()).postDelayed({ action() }, EXIT_ANIMATION_MS)
        }

        fun selectCategory(category: Category) {
            val status = budgetStatus[category]
            val limit = status?.limit
            val projected = (status?.spentSoFar ?: 0.0) + (payment.amount ?: 0.0)
            if (limit != null && limit > 0 && projected > limit) {
                breach = BudgetBreach(category, projected, limit)
            } else {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                dismissWith { onCategorySelected(category) }
            }
        }

        LaunchedEffect(breach) {
            val info = breach ?: return@LaunchedEffect
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            delay(90)
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            launch {
                shakeOffset.snapTo(0f)
                listOf(22f, -22f, 16f, -16f, 10f, -10f, 4f, -4f, 0f).forEach { target ->
                    shakeOffset.animateTo(target, animationSpec = tween(durationMillis = 45))
                }
            }
            delay(BREACH_DISPLAY_MS)
            dismissWith { onCategorySelected(info.category) }
        }

        val cardColor by animateColorAsState(
            targetValue = if (breach != null) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surface,
            animationSpec = tween(durationMillis = 150),
            label = "breachCardColor",
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = if (visible) 0.45f else 0f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { dismissWith(onSkip) },
                ),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMedium)) +
                    scaleIn(
                        initialScale = 0.85f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                    ),
                exit = fadeOut() + scaleOut(targetScale = 0.9f),
            ) {
                Surface(
                    modifier = Modifier
                        .padding(horizontal = 28.dp)
                        .offset { IntOffset(shakeOffset.value.roundToInt(), 0) }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {},
                        ),
                    color = cardColor,
                    shape = RoundedCornerShape(28.dp),
                    tonalElevation = 6.dp,
                    shadowElevation = 12.dp,
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .animateContentSize(),
                    ) {
                        val activeBreach = breach
                        if (activeBreach != null) {
                            BreachMessage(activeBreach)
                        } else {
                            Text(
                                text = payment.amount?.let { formatAmount(it) } ?: "Payment detected",
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = payment.merchant ?: "What was this for?",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 2.dp, bottom = 20.dp),
                            )

                            CATEGORY_ROWS.forEach { (left, right) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 5.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    CategoryButton(left, Modifier.weight(1f)) { selectCategory(left) }
                                    CategoryButton(right, Modifier.weight(1f)) { selectCategory(right) }
                                }
                            }

                            TextButton(
                                onClick = { dismissWith(onSkip) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp),
                            ) {
                                Text("Skip")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BreachMessage(breach: BudgetBreach) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Filled.Warning,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(40.dp),
        )
        Spacer(modifier = Modifier.size(12.dp))
        Text(
            text = "Over budget",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onErrorContainer,
        )
        Text(
            text = "That's ${formatRupees(breach.projected)} of your ${formatRupees(breach.limit)} " +
                "${breach.category.label} budget this month.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
private fun CategoryButton(category: Category, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = category.accentColor().copy(alpha = 0.12f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CategoryBadge(category, size = 36.dp)
            Spacer(modifier = Modifier.size(6.dp))
            Text(
                text = category.label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

private fun formatAmount(amount: Double): String {
    val value = if (amount == amount.toLong().toDouble()) amount.toLong().toString() else "%.2f".format(amount)
    return "₹$value"
}
