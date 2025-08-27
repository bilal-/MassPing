package dev.bilalahmad.massping.data.repository

import android.accounts.Account
import android.content.Context
import android.util.Log
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

class MassPingRepository(private val context: Context) {

    companion object {
        private const val TAG = "MassPingRepository"
    }

    init {
        Log.d(TAG, "MassPingRepository created")
    }

    private val nativeContactsService = NativeContactsService(context)
    private val messagePersonalizationService = MessagePersonalizationService()
    private val smsService = SmsService(context)

    // State flows for data
    private val _contacts = MutableStateFlow<List<Contact>>(emptyList())
    val contacts: StateFlow<List<Contact>> = _contacts.asStateFlow()

    private val _contactGroups = MutableStateFlow<List<ContactGroup>>(emptyList())
    val contactGroups: StateFlow<List<ContactGroup>> = _contactGroups.asStateFlow()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

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

                // Auto-select Google accounts by default
                if (_selectedAccounts.value.isEmpty()) {
                    _selectedAccounts.value = contactAccounts.filter { it.isGoogleAccount }
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
        val individualMessages = _individualMessages.value[messageId] ?: return

        // Use batch sending with delays to prevent Android's bulk SMS warning
        smsService.sendBatchWithDelay(individualMessages, delayBetweenMessages = 2000L)
    }

    fun updateIndividualMessageStatus(messageId: String, status: IndividualMessageStatus) {
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
    }

    fun getAvailablePlaceholders(): List<String> {
        return messagePersonalizationService.getAvailablePlaceholders()
    }

    fun cleanup() {
        smsService.cleanup()
    }
}
