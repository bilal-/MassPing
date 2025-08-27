package dev.bilalahmad.massping.ui.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.bilalahmad.massping.data.models.Contact
import dev.bilalahmad.massping.data.models.ContactAccount
import dev.bilalahmad.massping.data.models.ContactGroup
import dev.bilalahmad.massping.data.models.IndividualMessage
import dev.bilalahmad.massping.data.models.Message
import dev.bilalahmad.massping.data.repository.MassPingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "MainViewModel"
    }

    init {
        Log.d(TAG, "MainViewModel created")
    }

    private val repository = MassPingRepository.getInstance(application)

    // UI State
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    // Data from repository
    val contacts = repository.contacts
    val contactGroups = repository.contactGroups
    val messages = repository.messages
    val individualMessages = repository.individualMessages
    val availableAccounts = repository.availableAccounts
    val selectedAccounts = repository.selectedAccounts
    val messageHistory = repository.messageHistory
    init {
        Log.d(TAG, "MainViewModel init block executing")
        // Repository handles its own SMS status updates
    }

    fun loadAvailableAccounts() {
        Log.d(TAG, "loadAvailableAccounts() called")
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            repository.loadAvailableAccounts()
                .onSuccess {
                    Log.d(TAG, "Accounts loaded successfully")
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
                .onFailure { error ->
                    Log.e(TAG, "Failed to load accounts: ${error.message}", error)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to load accounts"
                    )
                }
        }
    }

    fun updateSelectedAccounts(accounts: List<ContactAccount>) {
        repository.updateSelectedAccounts(accounts)
        syncContacts()
    }

    fun syncContacts() {
        Log.d(TAG, "syncContacts() called")
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            Log.d(TAG, "Starting contacts sync...")

            repository.syncContacts()
                .onSuccess {
                    Log.d(TAG, "Contacts sync successful")
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
                .onFailure { error ->
                    Log.e(TAG, "Contacts sync failed: ${error.message}", error)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to sync contacts"
                    )
                }
        }
    }

    fun updateContactNickname(contactId: String, nickname: String) {
        repository.updateContactNickname(contactId, nickname)
    }


    fun createMessage(template: String, selectedGroupIds: List<String>): Message {
        return repository.createMessage(template, selectedGroupIds)
    }

    fun generatePersonalizedMessages(messageId: String): List<IndividualMessage> {
        return repository.generatePersonalizedMessages(messageId)
    }

    fun previewPersonalizedMessages(template: String, selectedGroupIds: List<String>): List<Pair<Contact, String>> {
        return repository.previewPersonalizedMessages(template, selectedGroupIds)
    }

    fun previewPersonalizedMessagesForContacts(template: String, selectedContactIds: List<String>): List<Pair<Contact, String>> {
        return repository.previewPersonalizedMessagesForContacts(template, selectedContactIds)
    }

    fun createMessageForContacts(template: String, selectedContactIds: List<String>): Message {
        return repository.createMessageForContacts(template, selectedContactIds)
    }

    fun sendMessage(messageId: String) {
        viewModelScope.launch {
            repository.sendMessage(messageId)
        }
    }

    suspend fun getMessageHistoryStats(): Triple<Int, Int, Int> {
        return repository.getMessageHistoryStats()
    }

    fun getAvailablePlaceholders(): List<String> {
        return repository.getAvailablePlaceholders()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearCompletedMessages() {
        repository.clearCompletedMessages()
    }

    // SMS Settings
    fun getSmsDelay(): Long = repository.getSmsDelay()
    fun setSmsDelay(delaySeconds: Long) = repository.setSmsDelay(delaySeconds)

    fun getSmsTimeout(): Long = repository.getSmsTimeout()
    fun setSmsTimeout(timeoutSeconds: Long) = repository.setSmsTimeout(timeoutSeconds)

    override fun onCleared() {
        super.onCleared()
        repository.cleanup()
    }
}

data class MainUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)
