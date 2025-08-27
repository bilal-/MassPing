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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
    onMessageCreated: () -> Unit = {}
) {
    val contactGroups by viewModel.contactGroups.collectAsState()
    val contacts by viewModel.contacts.collectAsState()
    
    var messageTemplate by remember { mutableStateOf("") }
    var selectedGroupIds by remember { mutableStateOf(setOf<String>()) }
    var showPreview by remember { mutableStateOf(false) }
    var previewMessages by remember { mutableStateOf(emptyList<Pair<Contact, String>>()) }
    
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
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "New Message",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        }
    ) { paddingValues ->
        if (contacts.isEmpty()) {
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
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
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
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            OutlinedTextField(
                                value = messageTemplate,
                                onValueChange = { messageTemplate = it },
                                label = { Text("Your message") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 3,
                                placeholder = { Text("Hi {name}, how are you doing?") }
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
                
                // Contact Groups Section
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                "Select Contact Groups",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
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
                                val groupsToUse = if (selectedGroupIds.isEmpty()) {
                                    // If no groups selected, use all contacts
                                    emptyList()
                                } else {
                                    selectedGroupIds.toList()
                                }
                                
                                previewMessages = viewModel.previewPersonalizedMessages(
                                    messageTemplate, 
                                    groupsToUse
                                )
                                showPreview = true
                            },
                            enabled = messageTemplate.isNotBlank(),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Preview Messages")
                        }
                        
                        Button(
                            onClick = {
                                val groupsToUse = if (selectedGroupIds.isEmpty()) {
                                    emptyList()
                                } else {
                                    selectedGroupIds.toList()
                                }
                                
                                val message = viewModel.createMessage(messageTemplate, groupsToUse)
                                viewModel.generatePersonalizedMessages(message.id)
                                
                                // Reset form
                                messageTemplate = ""
                                selectedGroupIds = emptySet()
                                showPreview = false
                                
                                // Navigate to Messages tab
                                onMessageCreated()
                            },
                            enabled = messageTemplate.isNotBlank(),
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
