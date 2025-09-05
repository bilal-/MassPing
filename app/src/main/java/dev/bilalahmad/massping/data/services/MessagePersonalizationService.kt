package dev.bilalahmad.massping.data.services

import android.util.Log
import dev.bilalahmad.massping.data.models.Contact
import dev.bilalahmad.massping.data.models.IndividualMessage
import dev.bilalahmad.massping.data.models.Message
import java.util.UUID

class MessagePersonalizationService {

    companion object {
        private const val TAG = "MessagePersonalizationService"
        private const val NAME_PLACEHOLDER = "{name}"
        private const val NICKNAME_PLACEHOLDER = "{nickname}"
        private const val FIRST_NAME_PLACEHOLDER = "{firstname}"
        private const val SMS_PART_LIMIT = 160 // Standard SMS part limit
    }

    fun personalizeMessage(message: Message, contacts: List<Contact>): List<IndividualMessage> {
        val individualMessages = mutableListOf<IndividualMessage>()

        contacts.forEach { contact ->
            val personalizedContent = personalizeTemplate(message.template, contact)
            val phoneNumber = contact.primaryPhone

            // Debug logging for phone number issues
            Log.d(TAG, "Contact: ${contact.name}")
            Log.d(TAG, "  ID: ${contact.id}")
            Log.d(TAG, "  Phone numbers count: ${contact.phoneNumbers.size}")
            contact.phoneNumbers.forEachIndexed { index, phone ->
                Log.d(TAG, "    [$index] ${phone.number} (${phone.type}) - Primary: ${phone.isPrimary}, Mobile: ${phone.isMobile}")
            }
            Log.d(TAG, "  Primary phone: $phoneNumber")

            if (phoneNumber.isNullOrBlank()) {
                Log.w(TAG, "Skipping contact ${contact.name} - no valid phone number found")
                return@forEach
            }

            // Split the personalized message into parts if needed
            val messageParts = splitMessageIntelligently(personalizedContent)
            Log.d(TAG, "Creating ${messageParts.size} individual message(s) for ${contact.name}")

            messageParts.forEachIndexed { partIndex, messagePart ->
                val partId = if (messageParts.size == 1) {
                    UUID.randomUUID().toString()
                } else {
                    "${UUID.randomUUID()}-part${partIndex + 1}"
                }

                individualMessages.add(
                    IndividualMessage(
                        id = partId,
                        messageId = message.id,
                        contactId = contact.id,
                        contactName = contact.name,
                        phoneNumber = phoneNumber,
                        personalizedContent = messagePart
                    )
                )
                Log.d(TAG, "Created part ${partIndex + 1}/${messageParts.size} for ${contact.name}: ${messagePart.length} chars")
            }
        }

        Log.d(TAG, "Total individual messages created: ${individualMessages.size}")
        return individualMessages
    }

    private fun personalizeTemplate(template: String, contact: Contact): String {
        var personalized = template

        // Replace name placeholder with display name (nickname if available, otherwise full name)
        personalized = personalized.replace(NAME_PLACEHOLDER, contact.displayName, ignoreCase = true)

        // Replace nickname placeholder
        contact.nickname?.let { nickname ->
            personalized = personalized.replace(NICKNAME_PLACEHOLDER, nickname, ignoreCase = true)
        } ?: run {
            // Log warning if template contains {nickname} but contact has no nickname
            if (personalized.contains(NICKNAME_PLACEHOLDER, ignoreCase = true)) {
                Log.w(TAG, "Contact '${contact.name}' has no nickname but template contains {nickname}")
            }
        }

        // Replace first name placeholder
        val firstName = contact.name.split(" ").firstOrNull() ?: contact.name
        personalized = personalized.replace(FIRST_NAME_PLACEHOLDER, firstName, ignoreCase = true)

        return personalized
    }

    fun getAvailablePlaceholders(): List<String> {
        return listOf(
            NAME_PLACEHOLDER,
            NICKNAME_PLACEHOLDER,
            FIRST_NAME_PLACEHOLDER
        )
    }

    fun previewPersonalization(template: String, sampleContacts: List<Contact>): List<Pair<Contact, String>> {
        return sampleContacts.take(5).map { contact ->
            contact to personalizeTemplate(template, contact)
        }
    }

    fun splitMessageIntelligently(message: String): List<String> {
        if (message.length <= SMS_PART_LIMIT) {
            return listOf(message)
        }

        Log.d(TAG, "Splitting long message of ${message.length} characters")
        val parts = mutableListOf<String>()
        var remainingText = message

        while (remainingText.length > SMS_PART_LIMIT) {
            var splitPoint = SMS_PART_LIMIT

            // Look for natural break points within the limit, starting from the end
            val preferredBreaks = listOf("\n\n", "\n", ". ", "! ", "? ", ", ", " ")
            var foundBreak = false

            for (breakPattern in preferredBreaks) {
                val lastBreakIndex = remainingText.substring(0, splitPoint).lastIndexOf(breakPattern)
                if (lastBreakIndex > SMS_PART_LIMIT / 2) { // Don't split too early (at least half way)
                    splitPoint = lastBreakIndex + breakPattern.length
                    foundBreak = true
                    Log.d(TAG, "Found natural break at '$breakPattern' at position $lastBreakIndex")
                    break
                }
            }

            if (!foundBreak) {
                // If no natural break found, just split at word boundary
                val lastSpaceIndex = remainingText.substring(0, SMS_PART_LIMIT).lastIndexOf(' ')
                if (lastSpaceIndex > SMS_PART_LIMIT / 2) {
                    splitPoint = lastSpaceIndex + 1
                    Log.d(TAG, "Split at word boundary at position $lastSpaceIndex")
                } else {
                    // Last resort: hard split at character limit
                    Log.d(TAG, "Hard split at character limit")
                }
            }

            val part = remainingText.substring(0, splitPoint).trim()
            parts.add(part)
            remainingText = remainingText.substring(splitPoint).trim()

            Log.d(TAG, "Created part ${parts.size}: ${part.length} chars")
        }

        if (remainingText.isNotEmpty()) {
            parts.add(remainingText)
            Log.d(TAG, "Final part: ${remainingText.length} chars")
        }

        Log.d(TAG, "Message split into ${parts.size} parts")
        return parts
    }
}
