package dev.bilalahmad.massping.data.services

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import android.database.Cursor
import android.provider.ContactsContract
import android.util.Log
import dev.bilalahmad.massping.data.models.Contact
import dev.bilalahmad.massping.data.models.ContactEmail
import dev.bilalahmad.massping.data.models.ContactGroup
import dev.bilalahmad.massping.data.models.ContactPhone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NativeContactsService(private val context: Context) {

    companion object {
        private const val TAG = "NativeContactsService"
    }

    init {
        Log.d(TAG, "NativeContactsService created")
    }

    suspend fun getAvailableAccounts(): Result<List<Account>> {
        return withContext(Dispatchers.IO) {
            try {
                val accountManager = AccountManager.get(context)
                val accounts = accountManager.getAccountsByType("com.google")
                    .plus(accountManager.accounts.filter { it.type != "com.google" })

                Log.d(TAG, "Found ${accounts.size} accounts")
                Result.success(accounts.toList())
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching accounts", e)
                Result.failure(e)
            }
        }
    }

    suspend fun fetchContacts(selectedAccounts: List<Account>? = null): Result<List<Contact>> {
        return withContext(Dispatchers.IO) {
            try {
                val contacts = mutableMapOf<String, Contact>()

                // Get all contacts with phone numbers first
                val contactCursor = context.contentResolver.query(
                    ContactsContract.Contacts.CONTENT_URI,
                    arrayOf(
                        ContactsContract.Contacts._ID,
                        ContactsContract.Contacts.LOOKUP_KEY,
                        ContactsContract.Contacts.DISPLAY_NAME,
                        ContactsContract.Contacts.HAS_PHONE_NUMBER,
                        ContactsContract.Contacts.PHOTO_URI
                    ),
                    "${ContactsContract.Contacts.HAS_PHONE_NUMBER} = 1",
                    null,
                    "${ContactsContract.Contacts.DISPLAY_NAME} ASC"
                )

                contactCursor?.use { cursor ->
                    while (cursor.moveToNext()) {
                        val contactId = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID))
                        val lookupKey = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.LOOKUP_KEY))
                        val displayName = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME))
                        val photoUri = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.PHOTO_URI))

                        // Filter by account if specified
                        if (!selectedAccounts.isNullOrEmpty()) {
                            val contactAccounts = getContactAccounts(contactId)
                            val hasMatchingAccount = contactAccounts.any { contactAccount ->
                                selectedAccounts.any { selectedAccount ->
                                    contactAccount.name == selectedAccount.name && contactAccount.type == selectedAccount.type
                                }
                            }
                            if (!hasMatchingAccount) {
                                continue // Skip this contact
                            }
                        }

                        // Get structured name data
                        val (firstName, lastName, middleName) = getStructuredName(contactId)

                        // Get nickname
                        val nickname = getNickname(contactId)

                        // Get organization data
                        val (company, jobTitle, department) = getOrganizationData(contactId)

                        contacts[contactId] = Contact(
                            id = lookupKey ?: contactId,
                            name = displayName ?: "",
                            firstName = firstName,
                            lastName = lastName,
                            middleName = middleName,
                            phoneNumbers = emptyList(),
                            emails = emptyList(),
                            nickname = nickname,
                            company = company,
                            jobTitle = jobTitle,
                            department = department,
                            groups = emptyList(),
                            photoUri = photoUri
                        )
                    }
                }

                // Get phone numbers for all contacts (SMS priority)
                contacts.keys.forEach { contactId ->
                    val phoneNumbers = getPhoneNumbers(contactId)
                    contacts[contactId] = contacts[contactId]!!.copy(phoneNumbers = phoneNumbers)
                }

                // Get email addresses for detailed view
                contacts.keys.forEach { contactId ->
                    val emails = getEmailAddresses(contactId)
                    contacts[contactId] = contacts[contactId]!!.copy(emails = emails)
                }

                // Get group memberships
                contacts.keys.forEach { contactId ->
                    val groups = getGroupMemberships(contactId)
                    contacts[contactId] = contacts[contactId]!!.copy(groups = groups)
                }

                Log.d(TAG, "Fetched ${contacts.size} contacts from ${selectedAccounts?.size ?: 0} selected accounts")
                Result.success(contacts.values.toList())

            } catch (e: Exception) {
                Log.e(TAG, "Error fetching contacts", e)
                Result.failure(e)
            }
        }
    }

    suspend fun fetchContactGroups(selectedAccounts: List<Account>? = null): Result<List<ContactGroup>> {
        return withContext(Dispatchers.IO) {
            try {
                val groups = mutableListOf<ContactGroup>()

                // Build selection criteria for account filtering
                val selection = StringBuilder("${ContactsContract.Groups.DELETED} = 0 AND ${ContactsContract.Groups.TITLE} IS NOT NULL")
                val selectionArgs = mutableListOf<String>()

                // Add account filtering if specified
                if (!selectedAccounts.isNullOrEmpty()) {
                    val accountConditions = mutableListOf<String>()
                    selectedAccounts.forEach { account ->
                        accountConditions.add("(${ContactsContract.Groups.ACCOUNT_NAME} = ? AND ${ContactsContract.Groups.ACCOUNT_TYPE} = ?)")
                        selectionArgs.add(account.name)
                        selectionArgs.add(account.type)
                    }
                    selection.append(" AND (${accountConditions.joinToString(" OR ")})")
                }

                val groupCursor = context.contentResolver.query(
                    ContactsContract.Groups.CONTENT_URI,
                    arrayOf(
                        ContactsContract.Groups._ID,
                        ContactsContract.Groups.TITLE,
                        ContactsContract.Groups.SYSTEM_ID,
                        ContactsContract.Groups.ACCOUNT_NAME,
                        ContactsContract.Groups.ACCOUNT_TYPE
                    ),
                    selection.toString(),
                    selectionArgs.toTypedArray(),
                    "${ContactsContract.Groups.TITLE} ASC"
                )

                groupCursor?.use { cursor ->
                    while (cursor.moveToNext()) {
                        val groupId = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Groups._ID))
                        val title = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Groups.TITLE))
                        val systemId = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Groups.SYSTEM_ID))
                        val accountName = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Groups.ACCOUNT_NAME))
                        val accountType = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Groups.ACCOUNT_TYPE))

                        // Skip system groups we don't want to show
                        if (systemId != null && (systemId == "Contacts" || systemId == "Family" || systemId == "Friends")) {
                            continue
                        }

                        // Get contact IDs in this group (only from synced accounts)
                        val contactIds = getContactsInGroup(groupId, selectedAccounts)

                        // Only include groups that have contacts from synced accounts
                        if (contactIds.isNotEmpty()) {
                            groups.add(
                                ContactGroup(
                                    id = groupId,
                                    name = title ?: "Unnamed Group",
                                    contactIds = contactIds
                                )
                            )

                            Log.d(TAG, "Added group '$title' from account $accountName ($accountType) with ${contactIds.size} contacts")
                        }
                    }
                }

                val accountInfo = if (selectedAccounts.isNullOrEmpty()) "all accounts" else "${selectedAccounts.size} selected accounts"
                Log.d(TAG, "Fetched ${groups.size} contact groups from $accountInfo")
                Result.success(groups)

            } catch (e: Exception) {
                Log.e(TAG, "Error fetching contact groups", e)
                Result.failure(e)
            }
        }
    }

    private fun getStructuredName(contactId: String): Triple<String?, String?, String?> {
        val cursor = context.contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME,
                ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME,
                ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME
            ),
            "${ContactsContract.Data.CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
            arrayOf(contactId, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE),
            null
        )

        cursor?.use {
            if (it.moveToFirst()) {
                val firstName = it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME))
                val lastName = it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME))
                val middleName = it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME))
                return Triple(firstName, lastName, middleName)
            }
        }

        return Triple(null, null, null)
    }

    private fun getNickname(contactId: String): String? {
        val cursor = context.contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Nickname.NAME),
            "${ContactsContract.Data.CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
            arrayOf(contactId, ContactsContract.CommonDataKinds.Nickname.CONTENT_ITEM_TYPE),
            null
        )

        cursor?.use {
            if (it.moveToFirst()) {
                val nickname = it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Nickname.NAME))
                Log.d(TAG, "Found nickname for contact $contactId: $nickname")
                return nickname
            }
        }

        return null
    }

    private fun getOrganizationData(contactId: String): Triple<String?, String?, String?> {
        val cursor = context.contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Organization.COMPANY,
                ContactsContract.CommonDataKinds.Organization.TITLE,
                ContactsContract.CommonDataKinds.Organization.DEPARTMENT
            ),
            "${ContactsContract.Data.CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
            arrayOf(contactId, ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE),
            null
        )

        cursor?.use {
            if (it.moveToFirst()) {
                val company = it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Organization.COMPANY))
                val jobTitle = it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Organization.TITLE))
                val department = it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Organization.DEPARTMENT))
                return Triple(company, jobTitle, department)
            }
        }

        return Triple(null, null, null)
    }

    private fun getPhoneNumbers(contactId: String): List<ContactPhone> {
        val phoneNumbers = mutableListOf<ContactPhone>()

        val phoneCursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.TYPE,
                ContactsContract.CommonDataKinds.Phone.LABEL,
                ContactsContract.CommonDataKinds.Phone.IS_PRIMARY
            ),
            "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
            arrayOf(contactId),
            "${ContactsContract.CommonDataKinds.Phone.IS_PRIMARY} DESC, ${ContactsContract.CommonDataKinds.Phone.TYPE} ASC"
        )

        phoneCursor?.use { cursor ->
            while (cursor.moveToNext()) {
                val number = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER))
                val type = cursor.getInt(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.TYPE))
                val label = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.LABEL))
                val isPrimary = cursor.getInt(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.IS_PRIMARY)) == 1

                val typeLabel = ContactsContract.CommonDataKinds.Phone.getTypeLabel(
                    context.resources, type, label
                ).toString()

                val cleanNumber = normalizePhoneNumber(number)
                if (cleanNumber.isNotEmpty()) {
                    phoneNumbers.add(
                        ContactPhone(
                            number = cleanNumber,
                            type = typeLabel,
                            label = label,
                            isPrimary = isPrimary
                        )
                    )
                }
            }
        }

        return phoneNumbers
    }

    private fun normalizePhoneNumber(rawNumber: String?): String {
        if (rawNumber.isNullOrBlank()) return ""

        // Remove all whitespace and common separators, but preserve + and digits
        var cleaned = rawNumber.replace("\\s".toRegex(), "") // Remove spaces
            .replace("-".toRegex(), "") // Remove dashes
            .replace("\\(".toRegex(), "") // Remove opening parentheses
            .replace("\\)".toRegex(), "") // Remove closing parentheses
            .replace("\\.".toRegex(), "") // Remove periods
            .trim()

        // Keep only digits, +, and # (for extensions)
        cleaned = cleaned.replace("[^\\d+#]".toRegex(), "")

        // Handle US numbers - ensure they have country code
        if (cleaned.matches("^\\d{10}$".toRegex())) {
            // 10-digit US number, add +1
            cleaned = "+1$cleaned"
        } else if (cleaned.matches("^1\\d{10}$".toRegex())) {
            // 11-digit US number starting with 1, add +
            cleaned = "+$cleaned"
        } else if (cleaned.startsWith("1") && cleaned.length == 11) {
            // 11-digit number starting with 1, add +
            cleaned = "+$cleaned"
        }

        Log.d("NativeContactsService", "Normalized phone: '$rawNumber' -> '$cleaned'")
        return cleaned
    }

    private fun getEmailAddresses(contactId: String): List<ContactEmail> {
        val emails = mutableListOf<ContactEmail>()

        val emailCursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Email.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Email.ADDRESS,
                ContactsContract.CommonDataKinds.Email.TYPE,
                ContactsContract.CommonDataKinds.Email.LABEL,
                ContactsContract.CommonDataKinds.Email.IS_PRIMARY
            ),
            "${ContactsContract.CommonDataKinds.Email.CONTACT_ID} = ?",
            arrayOf(contactId),
            null
        )

        emailCursor?.use { cursor ->
            while (cursor.moveToNext()) {
                val address = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.ADDRESS))
                val type = cursor.getInt(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.TYPE))
                val label = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.LABEL))
                val isPrimary = cursor.getInt(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.IS_PRIMARY)) == 1

                val typeLabel = ContactsContract.CommonDataKinds.Email.getTypeLabel(
                    context.resources, type, label
                ).toString()

                emails.add(
                    ContactEmail(
                        address = address ?: "",
                        type = typeLabel,
                        label = label,
                        isPrimary = isPrimary
                    )
                )
            }
        }

        return emails
    }

    private fun getGroupMemberships(contactId: String): List<String> {
        val groups = mutableListOf<String>()

        val groupCursor = context.contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID),
            "${ContactsContract.Data.CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
            arrayOf(contactId, ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE),
            null
        )

        groupCursor?.use { cursor ->
            while (cursor.moveToNext()) {
                val groupId = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID))
                if (groupId != null) {
                    groups.add(groupId)
                }
            }
        }

        return groups
    }

    private fun getContactsInGroup(groupId: String, selectedAccounts: List<Account>? = null): List<String> {
        val contactIds = mutableListOf<String>()

        val cursor = context.contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            arrayOf(ContactsContract.Data.CONTACT_ID),
            "${ContactsContract.Data.MIMETYPE} = ? AND ${ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID} = ?",
            arrayOf(ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE, groupId),
            null
        )

        cursor?.use {
            while (it.moveToNext()) {
                val contactId = it.getString(it.getColumnIndexOrThrow(ContactsContract.Data.CONTACT_ID))

                // Filter by account if specified
                if (!selectedAccounts.isNullOrEmpty()) {
                    val contactAccounts = getContactAccounts(contactId)
                    val hasMatchingAccount = contactAccounts.any { contactAccount ->
                        selectedAccounts.any { selectedAccount ->
                            contactAccount.name == selectedAccount.name && contactAccount.type == selectedAccount.type
                        }
                    }
                    if (hasMatchingAccount) {
                        contactIds.add(contactId)
                    }
                } else {
                    contactIds.add(contactId)
                }
            }
        }

        return contactIds
    }

    private fun getContactAccounts(contactId: String): List<Account> {
        val accounts = mutableListOf<Account>()

        val cursor = context.contentResolver.query(
            ContactsContract.RawContacts.CONTENT_URI,
            arrayOf(
                ContactsContract.RawContacts.ACCOUNT_NAME,
                ContactsContract.RawContacts.ACCOUNT_TYPE
            ),
            "${ContactsContract.RawContacts.CONTACT_ID} = ?",
            arrayOf(contactId),
            null
        )

        cursor?.use {
            while (it.moveToNext()) {
                val accountName = it.getString(it.getColumnIndexOrThrow(ContactsContract.RawContacts.ACCOUNT_NAME))
                val accountType = it.getString(it.getColumnIndexOrThrow(ContactsContract.RawContacts.ACCOUNT_TYPE))

                if (accountName != null && accountType != null) {
                    accounts.add(Account(accountName, accountType))
                }
            }
        }

        return accounts
    }
}