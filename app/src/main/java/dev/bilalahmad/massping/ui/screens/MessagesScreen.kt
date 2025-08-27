package dev.bilalahmad.massping.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.bilalahmad.massping.data.models.IndividualMessageStatus
import dev.bilalahmad.massping.data.models.Message
import dev.bilalahmad.massping.ui.viewmodels.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen(viewModel: MainViewModel) {
    val messages by viewModel.messages.collectAsState()
    val individualMessages by viewModel.individualMessages.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Messages",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        }
    ) { paddingValues ->
        if (messages.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "💬",
                        style = MaterialTheme.typography.displayMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No messages yet",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Create your first message from the New Message tab",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                items(messages.sortedByDescending { it.createdAt }) { message ->
                    MessageItem(
                        message = message,
                        individualMessages = individualMessages[message.id] ?: emptyList(),
                        onSendMessage = { viewModel.sendMessage(message.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageItem(
    message: Message,
    individualMessages: List<dev.bilalahmad.massping.data.models.IndividualMessage>,
    onSendMessage: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault())
                            .format(Date(message.createdAt)),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        message.template,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        "Recipients: ${individualMessages.size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (individualMessages.isNotEmpty()) {
                    Button(
                        onClick = onSendMessage,
                        enabled = individualMessages.any { it.status == IndividualMessageStatus.PENDING }
                    ) {
                        Text("Send")
                    }
                }
            }

            if (individualMessages.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))

                val sentCount = individualMessages.count {
                    it.status == IndividualMessageStatus.SENT
                }
                val deliveredCount = individualMessages.count {
                    it.status == IndividualMessageStatus.DELIVERED
                }
                val failedCount = individualMessages.count {
                    it.status == IndividualMessageStatus.FAILED
                }
                val pendingCount = individualMessages.count {
                    it.status == IndividualMessageStatus.PENDING ||
                    it.status == IndividualMessageStatus.SENDING
                }

                // Progress indicators
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Sent: $sentCount, Delivered: $deliveredCount/${individualMessages.size}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            "${((sentCount + deliveredCount).toFloat() / individualMessages.size * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    LinearProgressIndicator(
                        progress = (sentCount + deliveredCount).toFloat() / individualMessages.size,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (failedCount > 0) {
                            Text(
                                "Failed: $failedCount",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        if (pendingCount > 0) {
                            Text(
                                "Pending: $pendingCount",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
