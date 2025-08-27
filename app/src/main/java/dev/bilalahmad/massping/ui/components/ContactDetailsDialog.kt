package dev.bilalahmad.massping.ui.components

import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import dev.bilalahmad.massping.data.models.Contact
import dev.bilalahmad.massping.data.models.ContactPhone

@Composable
fun ContactDetailsDialog(
    contact: Contact,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                // Header with close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = contact.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )

                        contact.nickname?.let { nickname ->
                            Text(
                                text = "\"$nickname\"",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row {
                        IconButton(
                            onClick = {
                                fun editContact() {
                                    android.util.Log.d("ContactDetailsDialog", "Attempting to edit contact: ${contact.name} (ID: ${contact.id})")
                                    
                                    // Try multiple approaches to find and edit the specific contact
                                    val editStrategies = listOf(
                                        // Strategy 1: Modern ContactsContract URI with lookup key
                                        {
                                            Intent(Intent.ACTION_EDIT).apply {
                                                data = ContactsContract.Contacts.getLookupUri(contact.id.toLongOrNull() ?: 0, contact.id)
                                                putExtra("finishActivityOnSaveCompleted", true)
                                            }
                                        },
                                        // Strategy 2: Direct ContactsContract.Contacts URI
                                        {
                                            Intent(Intent.ACTION_EDIT).apply {
                                                data = ContactsContract.Contacts.CONTENT_URI.buildUpon()
                                                    .appendPath(contact.id)
                                                    .build()
                                                putExtra("finishActivityOnSaveCompleted", true)
                                            }
                                        },
                                        // Strategy 3: Legacy content URI format
                                        {
                                            Intent(Intent.ACTION_EDIT).apply {
                                                data = Uri.parse("content://com.android.contacts/contacts/${contact.id}")
                                                putExtra("finishActivityOnSaveCompleted", true)
                                            }
                                        }
                                    )

                                    // Try each strategy
                                    for ((index, strategy) in editStrategies.withIndex()) {
                                        try {
                                            val intent = strategy()
                                            android.util.Log.d("ContactDetailsDialog", "Trying strategy ${index + 1}: ${intent.data}")
                                            // Check if there's an app that can handle this intent
                                            if (intent.resolveActivity(context.packageManager) != null) {
                                                android.util.Log.d("ContactDetailsDialog", "Strategy ${index + 1} succeeded, launching intent")
                                                context.startActivity(intent)
                                                return
                                            } else {
                                                android.util.Log.d("ContactDetailsDialog", "Strategy ${index + 1} failed - no app can handle this intent")
                                            }
                                        } catch (e: Exception) {
                                            android.util.Log.e("ContactDetailsDialog", "Strategy ${index + 1} threw exception: ${e.message}")
                                            // Continue to next strategy
                                        }
                                    }

                                    // Fallback 1: Try to open Google Contacts app specifically
                                    try {
                                        val googleContactsIntent = context.packageManager.getLaunchIntentForPackage("com.google.android.contacts")
                                        if (googleContactsIntent != null) {
                                            context.startActivity(googleContactsIntent)
                                            Toast.makeText(context, "Opened Google Contacts - please search for ${contact.name}", Toast.LENGTH_LONG).show()
                                            return
                                        }
                                    } catch (e: Exception) {
                                        // Continue to next fallback
                                    }

                                    // Fallback 2: Try universal contacts picker
                                    try {
                                        val contactsIntent = Intent(Intent.ACTION_PICK).apply {
                                            data = ContactsContract.Contacts.CONTENT_URI
                                        }
                                        if (contactsIntent.resolveActivity(context.packageManager) != null) {
                                            context.startActivity(contactsIntent)
                                            Toast.makeText(context, "Opening contacts picker - please find ${contact.name}", Toast.LENGTH_LONG).show()
                                            return
                                        }
                                    } catch (e: Exception) {
                                        android.util.Log.e("ContactDetailsDialog", "Universal contacts picker failed: ${e.message}")
                                    }

                                    // Fallback 3: Try any default contacts app
                                    try {
                                        val contactsIntent = Intent(Intent.ACTION_VIEW).apply {
                                            data = ContactsContract.Contacts.CONTENT_URI
                                        }
                                        if (contactsIntent.resolveActivity(context.packageManager) != null) {
                                            context.startActivity(contactsIntent)
                                            Toast.makeText(context, "Opened default contacts app - please search for ${contact.name}", Toast.LENGTH_LONG).show()
                                            return
                                        }
                                    } catch (e: Exception) {
                                        android.util.Log.e("ContactDetailsDialog", "Generic contacts view failed: ${e.message}")
                                    }

                                    // Final fallback: Show error message
                                    Toast.makeText(
                                        context,
                                        "No contacts app found. Please install Google Contacts or a compatible contacts app to edit contacts.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }

                                editContact()
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit Contact",
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    modifier = Modifier
                        .weight(1f, fill = true)
                        .fillMaxWidth()
                ) {
                    // Structured Name
                    if (contact.firstName != null || contact.lastName != null || contact.middleName != null) {
                        item {
                            ContactDetailSection(
                                title = "Full Name",
                                icon = "👤"
                            ) {
                                val nameParts = listOfNotNull(
                                    contact.firstName,
                                    contact.middleName,
                                    contact.lastName
                                ).joinToString(" ")

                                if (nameParts.isNotBlank()) {
                                    ContactDetailItem(
                                        label = "Structured Name",
                                        value = nameParts
                                    )
                                }
                            }
                        }
                    }

                    // Organization Information
                    if (contact.company != null || contact.jobTitle != null || contact.department != null) {
                        item {
                            ContactDetailSection(
                                title = "Organization",
                                icon = "🏢"
                            ) {
                                contact.company?.let { company ->
                                    ContactDetailItem(
                                        label = "Company",
                                        value = company
                                    )
                                }

                                contact.jobTitle?.let { jobTitle ->
                                    ContactDetailItem(
                                        label = "Job Title",
                                        value = jobTitle
                                    )
                                }

                                contact.department?.let { department ->
                                    ContactDetailItem(
                                        label = "Department",
                                        value = department
                                    )
                                }
                            }
                        }
                    }

                    // Phone Numbers
                    if (contact.phoneNumbers.isNotEmpty()) {
                        item {
                            ContactDetailSection(
                                title = "Phone Numbers",
                                icon = "📞"
                            ) {
                                contact.phoneNumbers.forEach { phone ->
                                    ContactDetailItem(
                                        label = "${phone.type}${if (phone.isMobile) " 📱" else ""}${if (phone.isPrimary) " ⭐" else ""}",
                                        value = phone.number,
                                        isHighlighted = phone.isMobile
                                    )
                                }
                            }
                        }
                    }

                    // Email Addresses
                    if (contact.emails.isNotEmpty()) {
                        item {
                            ContactDetailSection(
                                title = "Email Addresses",
                                icon = "📧"
                            ) {
                                contact.emails.forEach { email ->
                                    ContactDetailItem(
                                        label = "${email.type}${if (email.isPrimary) " ⭐" else ""}",
                                        value = email.address
                                    )
                                }
                            }
                        }
                    }

                    // Contact Groups
                    if (contact.groups.isNotEmpty()) {
                        item {
                            ContactDetailSection(
                                title = "Groups",
                                icon = "👥"
                            ) {
                                ContactDetailItem(
                                    label = "Member of",
                                    value = "${contact.groups.size} groups"
                                )
                            }
                        }
                    }

                    // Additional Info
                    item {
                        ContactDetailSection(
                            title = "Details",
                            icon = "ℹ️"
                        ) {
                            ContactDetailItem(
                                label = "Contact ID",
                                value = contact.id
                            )

                            contact.photoUri?.let { uri ->
                                ContactDetailItem(
                                    label = "Has Photo",
                                    value = "Yes"
                                )
                            }

                            contact.lastModified?.let { timestamp ->
                                ContactDetailItem(
                                    label = "Last Modified",
                                    value = java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault())
                                        .format(java.util.Date(timestamp))
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactDetailSection(
    title: String,
    icon: String,
    content: @Composable () -> Unit
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = icon,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Column(
            modifier = Modifier.padding(start = 24.dp)
        ) {
            content()
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun ContactDetailItem(
    label: String,
    value: String,
    isHighlighted: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isHighlighted) FontWeight.Medium else FontWeight.Normal,
            color = if (isHighlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(2f)
        )
    }
}