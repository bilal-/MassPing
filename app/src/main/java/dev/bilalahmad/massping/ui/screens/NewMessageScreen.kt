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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.bilalahmad.massping.data.models.Contact
import dev.bilalahmad.massping.data.models.ContactGroup
import dev.bilalahmad.massping.ui.viewmodels.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewMessageScreen(
    viewModel: MainViewModel,
    preselectedContactIds: List<String> = emptyList(),
    onMessageCreated: () -> Unit = {}
) {
    val contactGroups by viewModel.contactGroups.collectAsState()
    val contacts by viewModel.contacts.collectAsState()
    val copiedTemplate by viewModel.copiedTemplate.collectAsState()

    var messageTemplate by remember { mutableStateOf("") }
    var selectedGroupIds by remember { mutableStateOf(setOf<String>()) }
    var selectedContactIds by remember { mutableStateOf(preselectedContactIds.toSet()) }
    var showPreview by remember { mutableStateOf(false) }
    var previewMessages by remember { mutableStateOf(emptyList<Pair<Contact, String>>()) }
    var recipientSelectionMode by remember {
        mutableStateOf(if (preselectedContactIds.isNotEmpty()) "contacts" else "groups")
    }

    val placeholders = remember { viewModel.getAvailablePlaceholders() }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Auto-scroll to preview section when preview is shown
    LaunchedEffect(showPreview) {
        if (showPreview && previewMessages.isNotEmpty()) {
            // Scroll to the preview section (item index 3: template, groups, buttons, then preview)
            coroutineScope.launch {
                listState.animateScrollToItem(3)
            }
        }
    }

    // Check for copied template when copiedTemplate changes
    LaunchedEffect(copiedTemplate) {
        copiedTemplate?.let { template ->
            messageTemplate = template
            viewModel.clearCopiedTemplate()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Title section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    "New Message",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Content area
        if (contacts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "📱",
                        style = MaterialTheme.typography.displayMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No contacts available",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Please sync your contacts first",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Pre-selected contacts info card
                if (preselectedContactIds.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    "✅ Contacts Selected",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "${preselectedContactIds.size} contacts selected from Contacts tab",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                // Show names of pre-selected contacts
                                val preselectedContacts = contacts.filter { it.id in preselectedContactIds }
                                if (preselectedContacts.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        preselectedContacts.take(3).joinToString(", ") { it.name } +
                                        if (preselectedContacts.size > 3) " and ${preselectedContacts.size - 3} more..." else "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                // Message Template Section
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                "Message Template",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            // Show template copied feedback
                            if (copiedTemplate != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "📋 Template copied from message history!",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            val messageLength = messageTemplate.length
                            val willBeSplit = messageLength > 160
                            val estimatedParts = if (willBeSplit) (messageLength / 150) + 1 else 1 // Roughly estimate parts

                            OutlinedTextField(
                                value = messageTemplate,
                                onValueChange = { messageTemplate = it },
                                label = { Text("Your message") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 3,
                                placeholder = { Text("Hi {name}, how are you doing?") },
                                supportingText = {
                                    Text(
                                        if (willBeSplit) {
                                            "$messageLength characters • Will be split into ~$estimatedParts parts"
                                        } else {
                                            "$messageLength characters"
                                        },
                                        color = if (willBeSplit) {
                                            MaterialTheme.colorScheme.tertiary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                "Available placeholders:",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                placeholders.joinToString(" • "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Recipients Selection Section
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                "Select Recipients",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Mode selection buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { recipientSelectionMode = "groups" },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(if (recipientSelectionMode == "groups") "✓ Groups" else "Groups")
                                }
                                Button(
                                    onClick = { recipientSelectionMode = "contacts" },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(if (recipientSelectionMode == "contacts") "✓ Contacts" else "Contacts")
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            when (recipientSelectionMode) {
                                "groups" -> {
                                    if (contactGroups.isEmpty()) {
                                        Text(
                                            "No contact groups available. All contacts will be included.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    } else {
                                        contactGroups.forEach { group ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Checkbox(
                                                    checked = group.id in selectedGroupIds,
                                                    onCheckedChange = { isChecked ->
                                                        selectedGroupIds = if (isChecked) {
                                                            selectedGroupIds + group.id
                                                        } else {
                                                            selectedGroupIds - group.id
                                                        }
                                                    }
                                                )

                                                Spacer(modifier = Modifier.width(8.dp))

                                                Column {
                                                    Text(
                                                        group.name,
                                                        style = MaterialTheme.typography.bodyMedium
                                                    )
                                                    Text(
                                                        "${group.contactCount} contacts",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                "contacts" -> {
                                    if (contacts.isEmpty()) {
                                        Text(
                                            "No contacts available. Please sync your contacts first.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    } else {
                                        Text(
                                            "Select individual contacts (${selectedContactIds.size} selected):",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))

                                        // Add select all / deselect all buttons
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = { selectedContactIds = contacts.map { it.id }.toSet() },
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("Select All")
                                            }
                                            Button(
                                                onClick = { selectedContactIds = emptySet() },
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("Clear All")
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        contacts.sortedBy { it.name }.forEach { contact ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Checkbox(
                                                    checked = contact.id in selectedContactIds,
                                                    onCheckedChange = { isChecked ->
                                                        selectedContactIds = if (isChecked) {
                                                            selectedContactIds + contact.id
                                                        } else {
                                                            selectedContactIds - contact.id
                                                        }
                                                    }
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column {
                                                    Text(
                                                        contact.name,
                                                        style = MaterialTheme.typography.bodyMedium
                                                    )
                                                    contact.primaryPhone?.let { phone ->
                                                        Text(
                                                            phone,
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
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Action Buttons
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                when (recipientSelectionMode) {
                                    "groups" -> {
                                        val groupsToUse = if (selectedGroupIds.isEmpty()) {
                                            emptyList()
                                        } else {
                                            selectedGroupIds.toList()
                                        }
                                        previewMessages = viewModel.previewPersonalizedMessages(
                                            messageTemplate,
                                            groupsToUse
                                        )
                                    }
                                    "contacts" -> {
                                        val contactsToUse = if (selectedContactIds.isEmpty()) {
                                            emptyList()
                                        } else {
                                            selectedContactIds.toList()
                                        }
                                        previewMessages = viewModel.previewPersonalizedMessagesForContacts(
                                            messageTemplate,
                                            contactsToUse
                                        )
                                    }
                                }
                                showPreview = true
                            },
                            enabled = messageTemplate.isNotBlank() &&
                                    ((recipientSelectionMode == "groups" && selectedGroupIds.isNotEmpty()) ||
                                     (recipientSelectionMode == "contacts" && selectedContactIds.isNotEmpty())),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Preview Messages")
                        }

                        Button(
                            onClick = {
                                when (recipientSelectionMode) {
                                    "groups" -> {
                                        val groupsToUse = if (selectedGroupIds.isEmpty()) {
                                            emptyList()
                                        } else {
                                            selectedGroupIds.toList()
                                        }
                                        val message = viewModel.createMessage(messageTemplate, groupsToUse)
                                        viewModel.generatePersonalizedMessages(message.id)
                                    }
                                    "contacts" -> {
                                        val contactsToUse = if (selectedContactIds.isEmpty()) {
                                            emptyList()
                                        } else {
                                            selectedContactIds.toList()
                                        }
                                        val message = viewModel.createMessageForContacts(messageTemplate, contactsToUse)
                                        viewModel.generatePersonalizedMessages(message.id)
                                    }
                                }

                                // Reset form
                                messageTemplate = ""
                                selectedGroupIds = emptySet()
                                selectedContactIds = emptySet()
                                showPreview = false

                                // Navigate to Messages tab
                                onMessageCreated()
                            },
                            enabled = messageTemplate.isNotBlank() &&
                                    ((recipientSelectionMode == "groups" && selectedGroupIds.isNotEmpty()) ||
                                     (recipientSelectionMode == "contacts" && selectedContactIds.isNotEmpty())),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Create Message")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Preview Section
                if (showPreview && previewMessages.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    "Message Preview (first 5 recipients)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    items(previewMessages) { (contact, personalizedMessage) ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    "To: ${contact.displayName} (${contact.primaryPhone ?: "No phone"})",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                SelectionContainer {
                                    Text(
                                        personalizedMessage,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
