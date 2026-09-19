package com.koushik.paytrack

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.koushik.paytrack.data.DailySummary
import com.koushik.paytrack.data.PaymentRepository
import com.koushik.paytrack.ui.theme.PayTrackTheme

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
    val summaries by repository.observeSummaries().collectAsState(initial = emptyList())

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
            Text(text = "Recent days", style = MaterialTheme.typography.titleMedium)
        }

        items(summaries) { summary -> DailySummaryRow(summary) }
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
