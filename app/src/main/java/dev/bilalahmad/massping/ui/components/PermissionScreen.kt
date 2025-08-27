package dev.bilalahmad.massping.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.bilalahmad.massping.ui.utils.PermissionHandler

@Composable
fun PermissionScreen(
    missingPermissions: List<String>,
    permissionHandler: PermissionHandler,
    onRequestPermissions: () -> Unit,
    onSkip: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🔐",
                    style = MaterialTheme.typography.displayLarge
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Permissions Needed",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "MassPing needs these permissions to function properly:",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                LazyColumn {
                    items(missingPermissions) { permission ->
                        PermissionItem(
                            permission = permission,
                            explanation = permissionHandler.getPermissionExplanation(permission)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onRequestPermissions,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Grant Permissions")
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onSkip,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Skip for Now")
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "You can change these permissions later in your device settings.",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PermissionItem(
    permission: String,
    explanation: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = getPermissionIcon(permission),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(end = 12.dp, top = 2.dp)
        )

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = getPermissionTitle(permission),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = explanation,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun getPermissionIcon(permission: String): String {
    return when {
        permission.contains("SMS") -> "📱"
        permission.contains("CONTACTS") -> "👥"
        permission.contains("ACCOUNTS") -> "🔄"
        permission.contains("PHONE_STATE") -> "📶"
        permission.contains("FOREGROUND_SERVICE") -> "⚡"
        permission.contains("NOTIFICATIONS") -> "🔔"
        else -> "🔐"
    }
}

private fun getPermissionTitle(permission: String): String {
    return when {
        permission.contains("SMS") -> "SMS Messages"
        permission.contains("CONTACTS") -> "Contacts Access"
        permission.contains("ACCOUNTS") -> "Account Access"
        permission.contains("PHONE_STATE") -> "Phone Status"
        permission.contains("FOREGROUND_SERVICE") -> "Background Service"
        permission.contains("NOTIFICATIONS") -> "Notifications"
        else -> "System Permission"
    }
}
