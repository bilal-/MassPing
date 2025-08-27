package dev.bilalahmad.massping.data.repository

import android.accounts.Account
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dev.bilalahmad.massping.data.database.MassPingDatabase
import dev.bilalahmad.massping.data.database.MessageHistory
import dev.bilalahmad.massping.data.models.Contact
import dev.bilalahmad.massping.data.models.ContactAccount
import dev.bilalahmad.massping.data.models.ContactGroup
import dev.bilalahmad.massping.data.models.IndividualMessage
import dev.bilalahmad.massping.data.models.IndividualMessageStatus
import dev.bilalahmad.massping.data.models.Message
import dev.bilalahmad.massping.data.services.NativeContactsService
import dev.bilalahmad.massping.data.services.MessagePersonalizationService
import dev.bilalahmad.massping.data.services.SmsService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MassPingRepository(private val context: Context) {

    companion object {
        private const val TAG = "MassPingRepository"
        private const val PREFS_NAME = "massping_preferences"
        private const val KEY_SELECTED_ACCOUNTS = "selected_accounts"
    }

    private val sharedPrefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    private val database = MassPingDatabase.getDatabase(context)
    private val messageHistoryDao = database.messageHistoryDao()

    private val nativeContactsService = NativeContactsService(context)
    private val messagePersonalizationService = MessagePersonalizationService()
    private val smsService = SmsService(context)

    init {
        Log.d(TAG, "MassPingRepository created")

        // Listen for SMS status updates and update individual messages
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
            smsService.statusUpdates.collect { (messageId, status) ->
                Log.d(TAG, "Received SMS status update: $messageId -> $status")
                updateIndividualMessageStatus(messageId, status)
            }
        }
    }

    // State flows for data
    private val _contacts = MutableStateFlow<List<Contact>>(emptyList())
    val contacts: StateFlow<List<Contact>> = _contacts.asStateFlow()

    private val _contactGroups = MutableStateFlow<List<ContactGroup>>(emptyList())
    val contactGroups: StateFlow<List<ContactGroup>> = _contactGroups.asStateFlow()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    // Message history from database
    val messageHistory: Flow<List<MessageHistory>> = messageHistoryDao.getAllMessagesFlow()

    private val _individualMessages = MutableStateFlow<Map<String, List<IndividualMessage>>>(emptyMap())
    val individualMessages: StateFlow<Map<String, List<IndividualMessage>>> = _individualMessages.asStateFlow()

    private val _availableAccounts = MutableStateFlow<List<ContactAccount>>(emptyList())
    val availableAccounts: StateFlow<List<ContactAccount>> = _availableAccounts.asStateFlow()

    private val _selectedAccounts = MutableStateFlow<List<ContactAccount>>(emptyList())
    val selectedAccounts: StateFlow<List<ContactAccount>> = _selectedAccounts.asStateFlow()

    // SMS status updates flow
    val smsStatusUpdates: Flow<Pair<String, IndividualMessageStatus>> = smsService.statusUpdates

    suspend fun loadAvailableAccounts(): Result<Unit> {
        return try {
            val accountsResult = nativeContactsService.getAvailableAccounts()
            accountsResult.getOrNull()?.let { accounts ->
                val contactAccounts = accounts.map { account ->
                    ContactAccount(
                        name = account.name,
                        type = account.type,
                        displayName = account.name
                    )
                }
                _availableAccounts.value = contactAccounts

                // Load saved selected accounts, or auto-select Google accounts by default
                if (_selectedAccounts.value.isEmpty()) {
                    val savedAccounts = loadSelectedAccounts()
                    if (savedAccounts.isNotEmpty()) {
                        // Filter saved accounts to only include currently available ones
                        val validSavedAccounts = savedAccounts.filter { saved ->
                            contactAccounts.any { available ->
                                available.name == saved.name && available.type == saved.type
                            }
                        }
                        _selectedAccounts.value = validSavedAccounts
                        Log.d(TAG, "Restored ${validSavedAccounts.size} saved accounts")
                    } else {
                        // First time - auto-select Google accounts
                        _selectedAccounts.value = contactAccounts.filter { it.isGoogleAccount }
                        Log.d(TAG, "First run - auto-selected Google accounts")
                    }
                }
            }

            if (accountsResult.isSuccess) {
                Result.success(Unit)
            } else {
                Result.failure(accountsResult.exceptionOrNull() ?: Exception("Unknown error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun syncContacts(): Result<Unit> {
        return try {
            // Convert ContactAccount back to Account for the service
            val selectedAndroidAccounts = _selectedAccounts.value.map { contactAccount ->
                Account(contactAccount.name, contactAccount.type)
            }

            val accountsToSync = if (selectedAndroidAccounts.isNotEmpty()) selectedAndroidAccounts else null

            val contactsResult = nativeContactsService.fetchContacts(accountsToSync)
            val groupsResult = nativeContactsService.fetchContactGroups(accountsToSync)

            contactsResult.getOrNull()?.let { contacts ->
                _contacts.value = contacts
            }

            groupsResult.getOrNull()?.let { groups ->
                _contactGroups.value = groups
            }

            if (contactsResult.isSuccess && groupsResult.isSuccess) {
                Result.success(Unit)
            } else {
                Result.failure(contactsResult.exceptionOrNull() ?: groupsResult.exceptionOrNull() ?: Exception("Unknown error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun updateSelectedAccounts(accounts: List<ContactAccount>) {
        _selectedAccounts.value = accounts
        saveSelectedAccounts(accounts)
    }

    private fun saveSelectedAccounts(accounts: List<ContactAccount>) {
        try {
            val json = gson.toJson(accounts)
            sharedPrefs.edit()
                .putString(KEY_SELECTED_ACCOUNTS, json)
                .apply()
            Log.d(TAG, "Saved ${accounts.size} selected accounts to preferences")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving selected accounts", e)
        }
    }

    private fun loadSelectedAccounts(): List<ContactAccount> {
        return try {
            val json = sharedPrefs.getString(KEY_SELECTED_ACCOUNTS, null)
            if (json != null) {
                val type = object : TypeToken<List<ContactAccount>>() {}.type
                val accounts = gson.fromJson<List<ContactAccount>>(json, type)
                Log.d(TAG, "Loaded ${accounts.size} selected accounts from preferences")
                accounts
            } else {
                Log.d(TAG, "No saved selected accounts found")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading selected accounts", e)
            emptyList()
        }
    }

    fun updateContactNickname(contactId: String, nickname: String) {
        _contacts.value = _contacts.value.map { contact ->
            if (contact.id == contactId) {
                contact.copy(nickname = nickname)
            } else {
                contact
            }
        }
    }


    fun getContactsByGroupIds(groupIds: List<String>): List<Contact> {
        return _contacts.value.filter { contact ->
            contact.groups.any { groupId -> groupId in groupIds }
        }
    }

    fun createMessage(template: String, selectedGroupIds: List<String>): Message {
        val message = Message(
            id = java.util.UUID.randomUUID().toString(),
            template = template,
            recipientGroups = selectedGroupIds
        )

        _messages.value = _messages.value + message
        return message
    }

    fun generatePersonalizedMessages(messageId: String): List<IndividualMessage> {
        val message = _messages.value.find { it.id == messageId } ?: return emptyList()
        val contacts = getContactsByGroupIds(message.recipientGroups)

        val personalizedMessages = messagePersonalizationService.personalizeMessage(message, contacts)

        _individualMessages.value = _individualMessages.value + (messageId to personalizedMessages)

        return personalizedMessages
    }

    fun previewPersonalizedMessages(template: String, selectedGroupIds: List<String>): List<Pair<Contact, String>> {
        val contacts = getContactsByGroupIds(selectedGroupIds)
        return messagePersonalizationService.previewPersonalization(template, contacts)
    }

    suspend fun sendMessage(messageId: String) {
        val message = _messages.value.find { it.id == messageId } ?: return

        // Generate individual messages if they don't exist
        var individualMessages = _individualMessages.value[messageId]
        if (individualMessages == null) {
            Log.d(TAG, "Generating individual messages for $messageId")
            individualMessages = generatePersonalizedMessages(messageId)
        }

        if (individualMessages.isEmpty()) {
            Log.e(TAG, "No individual messages to send for $messageId")
            return
        }

        Log.d(TAG, "Starting to send ${individualMessages.size} messages for $messageId")

        // Use batch sending with delays to prevent Android's bulk SMS warning
        smsService.sendBatchWithDelay(individualMessages, delayBetweenMessages = 5000L)
    }

    fun updateIndividualMessageStatus(messageId: String, status: IndividualMessageStatus) {
        Log.d(TAG, "updateIndividualMessageStatus: $messageId -> $status")
        _individualMessages.value = _individualMessages.value.mapValues { (key, messages) ->
            messages.map { message ->
                if (message.id == messageId) {
                    message.copy(
                        status = status,
                        sentAt = if (status == IndividualMessageStatus.SENT) System.currentTimeMillis() else message.sentAt,
                        deliveredAt = if (status == IndividualMessageStatus.DELIVERED) System.currentTimeMillis() else message.deliveredAt
                    )
                } else {
                    message
                }
            }
        }

        // Update message history stats when status changes
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            // Find which message this individual message belongs to
            val messageGroupId = _individualMessages.value.entries.find { (_, individualMessages) ->
                individualMessages.any { it.id == messageId }
            }?.key

            if (messageGroupId != null) {
                val individualMessages = _individualMessages.value[messageGroupId] ?: emptyList()

                // Create message history when we get the first SENT status (not SENDING)
                if (status == IndividualMessageStatus.SENT) {
                    val existingHistory = messageHistoryDao.getMessageById(messageGroupId)
                    if (existingHistory == null) {
                        // First SENT status - create the history record now
                        val message = _messages.value.find { it.id == messageGroupId }
                        if (message != null) {
                            Log.d(TAG, "Creating message history for $messageGroupId on first SENT status")
                            saveMessageToHistory(message, individualMessages)
                        }
                    }
                }

                updateMessageHistoryStats(messageGroupId)

                // Check if all messages in this group are completed (sent, delivered, or failed)
                val allCompleted = individualMessages.isNotEmpty() && individualMessages.all {
                    it.status in listOf(IndividualMessageStatus.SENT, IndividualMessageStatus.DELIVERED, IndividualMessageStatus.FAILED)
                }

                if (allCompleted) {
                    Log.d(TAG, "All messages completed for group $messageGroupId, clearing after delay")
                    // Clear completed messages after a short delay to allow user to see final status
                    kotlinx.coroutines.delay(3000L)
                    clearCompletedMessages()
                }
            }
        }
    }

    fun getAvailablePlaceholders(): List<String> {
        return messagePersonalizationService.getAvailablePlaceholders()
    }

    private suspend fun saveMessageToHistory(message: Message, individualMessages: List<IndividualMessage>) {
        try {
            // Get group names for the message
            val groupNames = message.recipientGroups.mapNotNull { groupId ->
                _contactGroups.value.find { it.id == groupId }?.name
            }

            val messageHistory = MessageHistory(
                id = message.id,
                template = message.template,
                recipientCount = individualMessages.size,
                sentCount = 0, // Will be updated as messages are sent
                deliveredCount = 0,
                failedCount = 0,
                createdAt = message.createdAt,
                sentAt = System.currentTimeMillis(),
                recipientGroupNames = gson.toJson(groupNames)
            )

            messageHistoryDao.insertMessage(messageHistory)
            Log.d(TAG, "Saved message history for ${message.id} with ${individualMessages.size} recipients")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving message history", e)
        }
    }

    private suspend fun updateMessageHistoryStats(messageId: String) {
        try {
            val individualMessages = _individualMessages.value[messageId] ?: return
            val sentCount = individualMessages.count { it.status == IndividualMessageStatus.SENT || it.status == IndividualMessageStatus.DELIVERED }
            val deliveredCount = individualMessages.count { it.status == IndividualMessageStatus.DELIVERED }
            val failedCount = individualMessages.count { it.status == IndividualMessageStatus.FAILED }

            val existingHistory = messageHistoryDao.getMessageById(messageId)
            if (existingHistory != null) {
                val updatedHistory = existingHistory.copy(
                    sentCount = sentCount,
                    deliveredCount = deliveredCount,
                    failedCount = failedCount
                )
                messageHistoryDao.updateMessage(updatedHistory)
                Log.d(TAG, "Updated message history stats for $messageId: sent=$sentCount, delivered=$deliveredCount, failed=$failedCount")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating message history stats", e)
        }
    }

    suspend fun getMessageHistoryStats(): Triple<Int, Int, Int> {
        return try {
            Triple(
                messageHistoryDao.getSentMessageCount(),
                messageHistoryDao.getTotalSentSmsCount() ?: 0,
                messageHistoryDao.getTotalDeliveredSmsCount() ?: 0
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting message history stats", e)
            Triple(0, 0, 0)
        }
    }

    fun clearCompletedMessages() {
        // Remove messages where all individual messages are either sent, delivered, or failed (no pending or sending)
        val messagesToKeep = _messages.value.filter { message ->
            val individualMessages = _individualMessages.value[message.id] ?: emptyList()
            individualMessages.any {
                it.status == IndividualMessageStatus.PENDING || it.status == IndividualMessageStatus.SENDING
            }
        }

        val individualMessagesToKeep = messagesToKeep.associate { message ->
            message.id to (_individualMessages.value[message.id] ?: emptyList())
        }

        _messages.value = messagesToKeep
        _individualMessages.value = individualMessagesToKeep

        Log.d(TAG, "Cleared completed messages, kept ${messagesToKeep.size} active messages")
    }

    fun cleanup() {
        smsService.cleanup()
    }
}
