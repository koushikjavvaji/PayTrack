package com.koushik.paytrack.popup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.koushik.paytrack.data.Category
import com.koushik.paytrack.notification.PaymentInfo
import com.koushik.paytrack.ui.theme.PayTrackTheme

private val CATEGORY_ROWS = listOf(
    Category.BREAKFAST to Category.LUNCH,
    Category.DINNER to Category.SNACKS,
    Category.TRAVEL to Category.OTHER_EXPENSES,
)

@Composable
fun PaymentPopupContent(
    payment: PaymentInfo,
    onCategorySelected: (Category) -> Unit,
    onSkip: () -> Unit,
) {
    PayTrackTheme {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 8.dp,
            shadowElevation = 8.dp,
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = payment.amount?.let { "₹${formatAmount(it)}" } ?: "Payment detected",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                payment.merchant?.let { merchant ->
                    Text(
                        text = merchant,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
                    )
                } ?: androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 12.dp))

                CATEGORY_ROWS.forEach { (left, right) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CategoryButton(left, Modifier.weight(1f), onCategorySelected)
                        CategoryButton(right, Modifier.weight(1f), onCategorySelected)
                    }
                }

                TextButton(
                    onClick = onSkip,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                ) {
                    Text("Skip")
                }
            }
        }
    }
}

@Composable
private fun CategoryButton(category: Category, modifier: Modifier, onClick: (Category) -> Unit) {
    Button(onClick = { onClick(category) }, modifier = modifier) {
        Text(category.label)
    }
}

private fun formatAmount(amount: Double): String =
    if (amount == amount.toLong().toDouble()) amount.toLong().toString() else "%.2f".format(amount)
