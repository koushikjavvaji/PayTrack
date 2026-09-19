package com.koushik.paytrack

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.koushik.paytrack.data.Category
import com.koushik.paytrack.data.DailySummary
import com.koushik.paytrack.data.PaymentRepository
import com.koushik.paytrack.data.PaymentTransaction
import com.koushik.paytrack.ui.theme.PayTrackTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PayTrackTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
                    HomeScreen(modifier = Modifier.padding(padding))
                }
            }
        }
    }
}

private fun isNotificationListenerEnabled(context: Context): Boolean {
    val enabledListeners = Settings.Secure.getString(
        context.contentResolver,
        "enabled_notification_listeners",
    ) ?: return false
    return enabledListeners.contains(context.packageName)
}

@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var notificationAccessGranted by remember { mutableStateOf(isNotificationListenerEnabled(context)) }
    var overlayGranted by remember { mutableStateOf(android.provider.Settings.canDrawOverlays(context)) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationAccessGranted = isNotificationListenerEnabled(context)
                overlayGranted = android.provider.Settings.canDrawOverlays(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val repository = remember { PaymentRepository(context) }
    val coroutineScope = rememberCoroutineScope()
    val summaries by repository.observeSummaries().collectAsState(initial = emptyList())
    val transactions by repository.observeRecentTransactions().collectAsState(initial = emptyList())

    var showAddDialog by remember { mutableStateOf(false) }
    var correctingTransaction by remember { mutableStateOf<PaymentTransaction?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(text = "PayTrack", style = MaterialTheme.typography.headlineMedium)
        }

        item {
            PermissionRow(
                granted = notificationAccessGranted,
                grantedText = "Notification access granted",
                missingText = "Notification access needed to detect GPay/Paytm payments",
                buttonText = "Grant notification access",
                onClick = { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) },
            )
        }

        item {
            PermissionRow(
                granted = overlayGranted,
                grantedText = "Overlay permission granted",
                missingText = "Overlay permission needed to show the category popup",
                buttonText = "Grant overlay permission",
                onClick = {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}"),
                        ),
                    )
                },
            )
        }

        item {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Button(onClick = { showAddDialog = true }) {
                Text("Add missed payment manually")
            }
        }

        item {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Text(text = "Recent days", style = MaterialTheme.typography.titleMedium)
        }

        items(summaries) { summary -> DailySummaryRow(summary) }

        item {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Text(text = "Recent transactions (tap to fix category)", style = MaterialTheme.typography.titleMedium)
        }

        items(transactions) { transaction ->
            TransactionRow(transaction, onClick = { correctingTransaction = transaction })
        }
    }

    if (showAddDialog) {
        AddTransactionDialog(
            onDismiss = { showAddDialog = false },
            onSubmit = { amount, merchant, category ->
                coroutineScope.launch {
                    repository.recordPayment(
                        amount = amount,
                        merchant = merchant,
                        app = "Manual entry",
                        category = category,
                        postedAt = System.currentTimeMillis(),
                    )
                }
                showAddDialog = false
            },
        )
    }

    correctingTransaction?.let { transaction ->
        CategoryPickerDialog(
            title = "Recategorize ₹${"%.2f".format(transaction.amount)}${transaction.merchant?.let { " – $it" } ?: ""}",
            onDismiss = { correctingTransaction = null },
            onSelected = { newCategory ->
                coroutineScope.launch { repository.correctCategory(transaction.id, newCategory) }
                correctingTransaction = null
            },
        )
    }
}

@Composable
private fun PermissionRow(
    granted: Boolean,
    grantedText: String,
    missingText: String,
    buttonText: String,
    onClick: () -> Unit,
) {
    Column {
        Text(text = if (granted) grantedText else missingText, style = MaterialTheme.typography.bodyMedium)
        if (!granted) {
            Button(onClick = onClick, modifier = Modifier.padding(top = 4.dp)) {
                Text(buttonText)
            }
        }
    }
}

@Composable
private fun DailySummaryRow(summary: DailySummary) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = summary.displayDate, style = MaterialTheme.typography.bodyLarge)
        Text(text = "₹${"%.2f".format(summary.total)}", style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun TransactionRow(transaction: PaymentTransaction, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(text = transaction.merchant ?: transaction.category.label, style = MaterialTheme.typography.bodyMedium)
            Text(text = transaction.category.label, style = MaterialTheme.typography.bodySmall)
        }
        Text(text = "₹${"%.2f".format(transaction.amount)}", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun CategoryPickerDialog(
    title: String,
    onDismiss: () -> Unit,
    onSelected: (Category) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Category.entries.chunked(2).forEach { rowCategories ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowCategories.forEach { category ->
                            Button(onClick = { onSelected(category) }, modifier = Modifier.weight(1f)) {
                                Text(category.label)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun AddTransactionDialog(
    onDismiss: () -> Unit,
    onSubmit: (amount: Double, merchant: String?, category: Category) -> Unit,
) {
    var amountText by remember { mutableStateOf("") }
    var merchantText by remember { mutableStateOf("") }
    var pickingCategory by remember { mutableStateOf(false) }

    if (pickingCategory) {
        CategoryPickerDialog(
            title = "Category for ₹$amountText",
            onDismiss = { pickingCategory = false },
            onSelected = { category ->
                val amount = amountText.toDoubleOrNull()
                if (amount != null) {
                    onSubmit(amount, merchantText.ifBlank { null }, category)
                }
            },
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add missed payment") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = merchantText,
                    onValueChange = { merchantText = it },
                    label = { Text("Merchant (optional)") },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (amountText.toDoubleOrNull() != null) pickingCategory = true },
            ) {
                Text("Next")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
