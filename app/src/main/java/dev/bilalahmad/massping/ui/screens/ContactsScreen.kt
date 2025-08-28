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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.bilalahmad.massping.data.models.Contact
import dev.bilalahmad.massping.ui.components.ContactDetailsDialog
import dev.bilalahmad.massping.ui.components.PermissionScreen
import dev.bilalahmad.massping.ui.utils.rememberPermissionHandler
import dev.bilalahmad.massping.ui.viewmodels.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
    viewModel: MainViewModel,
    onSendMessage: (List<String>) -> Unit = {}
) {
    val contacts by viewModel.contacts.collectAsState()
    val contactGroups by viewModel.contactGroups.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Filter states
    var selectedGroupId by remember { mutableStateOf<String?>(null) }
    var showGroupDropdown by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    // Selection states
    var selectedContactIds by remember { mutableStateOf(setOf<String>()) }

    // Filter contacts by selected group and search query
    val filteredContacts = remember(contacts, selectedGroupId, searchQuery) {
        var filtered = if (selectedGroupId == null) {
            contacts
        } else {
            contacts.filter { contact ->
                contact.groups.contains(selectedGroupId)
            }
        }

        // Apply search filter
        if (searchQuery.isNotBlank()) {
            val query = searchQuery.lowercase().trim()
            filtered = filtered.filter { contact ->
                contact.name.lowercase().contains(query) ||
                contact.nickname?.lowercase()?.contains(query) == true ||
                contact.company?.lowercase()?.contains(query) == true ||
                contact.jobTitle?.lowercase()?.contains(query) == true ||
                contact.department?.lowercase()?.contains(query) == true
            }
        }

        filtered
    }

    val selectedGroupName = remember(contactGroups, selectedGroupId) {
        if (selectedGroupId == null) "All Contacts"
        else contactGroups.find { it.id == selectedGroupId }?.name ?: "Unknown Group"
    }

    var hasPermissions by remember { mutableStateOf(false) }

    val permissionHandler = rememberPermissionHandler(
        onPermissionsGranted = {
            hasPermissions = true
            viewModel.loadAvailableAccounts()
        },
        onPermissionsDenied = { deniedPermissions ->
            hasPermissions = false
        }
    )

    // Check initial permission state
    LaunchedEffect(Unit) {
        hasPermissions = permissionHandler.hasAllPermissions()
        if (hasPermissions) {
            viewModel.loadAvailableAccounts()
        }
    }


    // Check if we need to show permission screen
    if (!hasPermissions) {
        val missingPermissions = permissionHandler.getMissingPermissions()
        PermissionScreen(
            missingPermissions = missingPermissions,
            permissionHandler = permissionHandler,
            onRequestPermissions = {
                permissionHandler.checkAndRequestPermissions()
            }
        )
        return
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Main content
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Contacts (${filteredContacts.size})",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            if (selectedContactIds.isNotEmpty()) {
                                Text(
                                    "${selectedContactIds.size} selected",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            } else if (selectedGroupId != null) {
                                Text(
                                    "Filtered by: $selectedGroupName",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (selectedContactIds.isNotEmpty()) {
                            // Select All / Clear All button when in selection mode
                            Button(
                                onClick = {
                                    selectedContactIds = if (selectedContactIds.size == filteredContacts.size) {
                                        emptySet()
                                    } else {
                                        filteredContacts.map { it.id }.toSet()
                                    }
                                }
                            ) {
                                Text(
                                    if (selectedContactIds.size == filteredContacts.size) "Clear" else "All",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            }

            // Content area
            if (filteredContacts.isEmpty() && !uiState.isLoading) {
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
                        style = MaterialTheme.typography.displayLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        when {
                            contacts.isEmpty() -> "No contacts found"
                            searchQuery.isNotBlank() -> "No contacts match your search"
                            selectedGroupId != null -> "No contacts in this group"
                            else -> "No contacts available"
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        when {
                            contacts.isEmpty() -> "Go to Settings to sync your contacts"
                            searchQuery.isNotBlank() -> "Try a different search term or clear the search"
                            selectedGroupId != null -> "Try selecting a different group or 'All Contacts'"
                            else -> "Pull down to refresh your contacts"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .height(48.dp),
                    placeholder = {
                        Text(
                            "Search contacts, jobs, companies...",
                            style = MaterialTheme.typography.bodySmall
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Search",
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.Clear,
                                    contentDescription = "Clear search",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    textStyle = MaterialTheme.typography.bodySmall
                )

                // Group filter dropdown
                if (contactGroups.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Box {
                            OutlinedButton(
                                onClick = { showGroupDropdown = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(2.dp)
                                    .height(40.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        selectedGroupName,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Icon(
                                        Icons.Default.ArrowDropDown,
                                        contentDescription = "Select Group"
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = showGroupDropdown,
                                onDismissRequest = { showGroupDropdown = false }
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text("All Contacts (${contacts.size})")
                                    },
                                    onClick = {
                                        selectedGroupId = null
                                        showGroupDropdown = false
                                    }
                                )

                                contactGroups.forEach { group ->
                                    DropdownMenuItem(
                                        text = {
                                            Text("${group.name} (${group.contactIds.size})")
                                        },
                                        onClick = {
                                            selectedGroupId = group.id
                                            showGroupDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                // Loading indicator at the top
                if (uiState.isLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.height(20.dp).width(20.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    "Syncing contacts...",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }

                // Contacts list
                items(filteredContacts) { contact ->
                    ContactItem(
                        contact = contact,
                        isSelected = contact.id in selectedContactIds,
                        onSelectionChange = { isSelected ->
                            selectedContactIds = if (isSelected) {
                                selectedContactIds + contact.id
                            } else {
                                selectedContactIds - contact.id
                            }
                        }
                    )
                }
                    }
                }
            }
        }

        // Floating Action Button
        if (selectedContactIds.isNotEmpty()) {
            FloatingActionButton(
                onClick = {
                    onSendMessage(selectedContactIds.toList())
                    selectedContactIds = emptySet() // Clear selection after sending
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send Message")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Send (${selectedContactIds.size})")
                }
            }
        }
    }
}

@Composable
private fun ContactItem(
    contact: Contact,
    isSelected: Boolean = false,
    onSelectionChange: (Boolean) -> Unit = {}
) {
    var showDetailsDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Selection checkbox
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = onSelectionChange
                )

                Spacer(modifier = Modifier.width(8.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        contact.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    // Show mobile number first (SMS priority)
                    contact.mobilePhone?.let { mobile ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "📱",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                mobile,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Show other phone numbers if no mobile or additional numbers
                    if (contact.mobilePhone == null) {
                        contact.primaryPhone?.let { phone ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "☎️",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    phone,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Show count of additional numbers if any
                    if (contact.phoneNumbers.size > 1) {
                        Text(
                            "+${contact.phoneNumbers.size - 1} more numbers",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Show professional info if available
                    if (contact.jobTitle?.isNotBlank() == true || contact.department?.isNotBlank() == true || contact.company?.isNotBlank() == true) {
                        Spacer(modifier = Modifier.height(3.dp))

                        // Job title and department
                        if (contact.jobTitle?.isNotBlank() == true) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "💼",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    contact.jobTitle!!,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                if (contact.department?.isNotBlank() == true) {
                                    Text(
                                        " • ${contact.department}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else if (contact.department?.isNotBlank() == true) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "🏢",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    contact.department!!,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Company name if different from job title line
                        if (contact.company?.isNotBlank() == true && contact.jobTitle?.isBlank() != false) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "🏢",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    contact.company!!,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Info button to show detailed view
                IconButton(
                    onClick = { showDetailsDialog = true }
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = "View Details",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Show nickname if available (read-only)
            if (contact.nickname?.isNotBlank() == true) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "💭",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "Nickname: ${contact.nickname}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }
        }

        // Contact Details Dialog
        if (showDetailsDialog) {
            ContactDetailsDialog(
                contact = contact,
                onDismiss = { showDetailsDialog = false }
            )
        }
    }
}
