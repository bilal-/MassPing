package dev.bilalahmad.massping.data.models

import android.os.Parcel
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

data class Message(
    val id: String,
    val template: String,
    val recipientGroups: List<String>,
    val individualContacts: List<String> = emptyList(), // Individual contact IDs when not using groups
    val recipientGroupNames: List<String> = emptyList(),
    val totalRecipients: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val sentAt: Long? = null,
    val completedAt: Long? = null,
    val status: MessageStatus = MessageStatus.DRAFT
)

enum class MessageStatus {
    DRAFT,
    PREVIEWING,
    SENDING,
    COMPLETED,
    FAILED,
    PAUSED
}

@Parcelize
data class IndividualMessage(
    val id: String,
    val messageId: String,
    val contactId: String,
    val contactName: String,
    val phoneNumber: String,
    val personalizedContent: String,
    val status: IndividualMessageStatus = IndividualMessageStatus.PENDING,
    val sentAt: Long? = null,
    val deliveredAt: Long? = null,
    val failureReason: String? = null
) : Parcelable

enum class IndividualMessageStatus {
    PENDING,
    SENDING,
    SENT,
    DELIVERED,
    FAILED
}
