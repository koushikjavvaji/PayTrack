package com.koushik.paytrack

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.koushik.paytrack.data.Budget
import com.koushik.paytrack.data.Category
import com.koushik.paytrack.data.DailySummary
import com.koushik.paytrack.data.PaymentRepository
import com.koushik.paytrack.data.PaymentTransaction
import com.koushik.paytrack.ui.CategoryBadge
import com.koushik.paytrack.ui.accentColor
import com.koushik.paytrack.ui.formatRupees
import com.koushik.paytrack.ui.readableOnColor
import com.koushik.paytrack.ui.theme.PayTrackTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PayTrackTheme {
                HomeScreen()
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
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
    val snackbarHostState = remember { SnackbarHostState() }
    val summaries by repository.observeSummaries().collectAsState(initial = emptyList())
    val transactions by repository.observeRecentTransactions().collectAsState(initial = emptyList())
    val budgets by repository.observeBudgets().collectAsState(initial = emptyList())
    val monthSummaries by repository.observeCurrentMonthSummaries().collectAsState(initial = emptyList())

    var showAddDialog by remember { mutableStateOf(false) }
    var showBudgetDialog by remember { mutableStateOf(false) }
    var correctingTransaction by remember { mutableStateOf<PaymentTransaction?>(null) }

    LaunchedEffect(repository) { repository.retryPendingSyncs() }

    val monthTotal = monthSummaries.sumOf { it.total }
    val monthBudgetTotal = budgets.sumOf { it.monthlyLimit }.takeIf { budgets.isNotEmpty() && it > 0 }

    fun deleteWithUndo(transaction: PaymentTransaction) {
        coroutineScope.launch {
            repository.deleteTransaction(transaction.id)
            val result = snackbarHostState.showSnackbar(
                message = "Deleted ${transaction.merchant ?: transaction.category.label}",
                actionLabel = "Undo",
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) {
                repository.recordPayment(
                    amount = transaction.amount,
                    merchant = transaction.merchant,
                    app = transaction.app,
                    category = transaction.category,
                    postedAt = transaction.postedAt,
                )
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(title = { Text("PayTrack", fontWeight = FontWeight.SemiBold) })
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text("Add expense") },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                onClick = { showAddDialog = true },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                HeroCard(monthTotal = monthTotal, monthBudgetTotal = monthBudgetTotal)
            }

            if (!notificationAccessGranted || !overlayGranted) {
                item {
                    SetupCard(
                        notificationAccessGranted = notificationAccessGranted,
                        overlayGranted = overlayGranted,
                        onGrantNotificationAccess = {
                            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                        },
                        onGrantOverlay = {
                            context.startActivity(
                                Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}"),
                                ),
                            )
                        },
                    )
                }
            }

            item {
                SectionCard(
                    title = "Budgets this month",
                    action = {
                        TextButton(onClick = { showBudgetDialog = true }) {
                            Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.size(4.dp))
                            Text("Edit")
                        }
                    },
                ) {
                    Category.entries.forEachIndexed { index, category ->
                        val spent = monthSummaries.sumOf { it.amountFor(category) }
                        val budget = budgets.find { it.category == category }?.monthlyLimit
                        BudgetRow(category = category, spent = spent, budget = budget)
                        if (index != Category.entries.lastIndex) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            }

            item {
                SectionCard(title = "Recent days") {
                    if (summaries.isEmpty()) {
                        EmptyRow("No spending recorded yet")
                    } else {
                        summaries.forEachIndexed { index, summary ->
                            DailySummaryRow(summary)
                            if (index != summaries.lastIndex) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            }
                        }
                    }
                }
            }

            item {
                SectionCard(title = "Recent transactions", subtitle = "Tap to recategorize · swipe to delete") {
                    if (transactions.isEmpty()) {
                        EmptyRow("No transactions yet")
                    } else {
                        Column {
                            transactions.forEachIndexed { index, transaction ->
                                SwipeableTransactionRow(
                                    transaction = transaction,
                                    onClick = { correctingTransaction = transaction },
                                    onDelete = { deleteWithUndo(transaction) },
                                )
                                if (index != transactions.lastIndex) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                }
                            }
                        }
                    }
                }
            }
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
            title = "Recategorize ${formatRupees(transaction.amount)}${transaction.merchant?.let { " – $it" } ?: ""}",
            onDismiss = { correctingTransaction = null },
            onSelected = { newCategory ->
                coroutineScope.launch { repository.correctCategory(transaction.id, newCategory) }
                correctingTransaction = null
            },
        )
    }

    if (showBudgetDialog) {
        SetBudgetsDialog(
            budgets = budgets,
            onDismiss = { showBudgetDialog = false },
            onSave = { category, limit ->
                coroutineScope.launch { repository.setBudget(category, limit) }
            },
        )
    }
}

@Composable
private fun HeroCard(monthTotal: Double, monthBudgetTotal: Double?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = "Spent this month",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text = formatRupees(monthTotal),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(top = 4.dp),
            )
            if (monthBudgetTotal != null) {
                val fraction by animateFloatAsState(
                    targetValue = (monthTotal / monthBudgetTotal).toFloat().coerceIn(0f, 1f),
                    animationSpec = tween(durationMillis = 700),
                    label = "monthProgress",
                )
                LinearProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f),
                )
                Text(
                    text = "of ${formatRupees(monthBudgetTotal)} budgeted",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    subtitle: String? = null,
    action: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    subtitle?.let {
                        Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                action?.invoke()
            }
            Spacer(modifier = Modifier.size(4.dp))
            content()
        }
    }
}

@Composable
private fun SetupCard(
    notificationAccessGranted: Boolean,
    overlayGranted: Boolean,
    onGrantNotificationAccess: () -> Unit,
    onGrantOverlay: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = "Setup needed",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
            if (!notificationAccessGranted) {
                PermissionRow(
                    text = "Notification access needed to detect GPay/Paytm payments",
                    buttonText = "Grant notification access",
                    onClick = onGrantNotificationAccess,
                )
            }
            if (!overlayGranted) {
                PermissionRow(
                    text = "Overlay permission needed to show the category popup",
                    buttonText = "Grant overlay permission",
                    onClick = onGrantOverlay,
                )
            }
        }
    }
}

@Composable
private fun PermissionRow(text: String, buttonText: String, onClick: () -> Unit) {
    Column(modifier = Modifier.padding(top = 12.dp)) {
        Text(text = text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onTertiaryContainer)
        Button(onClick = onClick, modifier = Modifier.padding(top = 6.dp)) {
            Text(buttonText)
        }
    }
}

@Composable
private fun BudgetRow(category: Category, spent: Double, budget: Double?) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CategoryBadge(category, size = 32.dp)
                Spacer(modifier = Modifier.size(10.dp))
                Text(text = category.label, style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                text = if (budget != null) {
                    "${formatRupees(spent)} / ${formatRupees(budget)}"
                } else {
                    "${formatRupees(spent)} spent"
                },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
        }
        if (budget != null && budget > 0) {
            val overBudget = spent > budget
            val fraction by animateFloatAsState(
                targetValue = (spent / budget).toFloat().coerceIn(0f, 1f),
                animationSpec = tween(durationMillis = 700),
                label = "budgetProgress",
            )
            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (overBudget) MaterialTheme.colorScheme.error else category.accentColor(),
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        }
    }
}

@Composable
private fun EmptyRow(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 12.dp),
    )
}

@Composable
private fun DailySummaryRow(summary: DailySummary) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = summary.displayDate, style = MaterialTheme.typography.bodyLarge)
        Text(text = formatRupees(summary.total), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun TransactionRow(transaction: PaymentTransaction, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CategoryBadge(transaction.category, size = 40.dp)
            Spacer(modifier = Modifier.size(12.dp))
            Column {
                Text(text = transaction.merchant ?: transaction.category.label, style = MaterialTheme.typography.bodyMedium)
                Text(text = transaction.category.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text(text = formatRupees(transaction.amount), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableTransactionRow(
    transaction: PaymentTransaction,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onDelete()
                true
            } else {
                false
            }
        },
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        },
    ) {
        TransactionRow(transaction, onClick = onClick)
    }
}

@Composable
private fun CategoryPickerDialog(
    title: String,
    onDismiss: () -> Unit,
    onSelected: (Category) -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Category.entries.chunked(2).forEach { rowCategories ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowCategories.forEach { category ->
                            Button(
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onSelected(category)
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = category.accentColor(),
                                    contentColor = category.accentColor().readableOnColor(),
                                ),
                            ) {
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

@Composable
private fun SetBudgetsDialog(
    budgets: List<Budget>,
    onDismiss: () -> Unit,
    onSave: (Category, Double) -> Unit,
) {
    val amounts = remember {
        Category.entries.associateWith { category ->
            mutableStateOf(budgets.find { it.category == category }?.monthlyLimit?.let { "%.0f".format(it) } ?: "")
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set monthly budgets") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Category.entries.forEach { category ->
                    var value by amounts.getValue(category)
                    OutlinedTextField(
                        value = value,
                        onValueChange = { value = it },
                        label = { Text(category.label) },
                        leadingIcon = { CategoryBadge(category, size = 28.dp) },
                        singleLine = true,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    Category.entries.forEach { category ->
                        amounts.getValue(category).value.toDoubleOrNull()?.let { onSave(category, it) }
                    }
                    onDismiss()
                },
            ) {
                Text("Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
