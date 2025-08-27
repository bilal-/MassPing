package dev.bilalahmad.massping.data.services

import dev.bilalahmad.massping.data.models.Contact
import dev.bilalahmad.massping.data.models.IndividualMessage
import dev.bilalahmad.massping.data.models.Message
import java.util.UUID

class MessagePersonalizationService {
    
    companion object {
        private const val NAME_PLACEHOLDER = "{name}"
        private const val NICKNAME_PLACEHOLDER = "{nickname}"
        private const val FIRST_NAME_PLACEHOLDER = "{firstname}"
    }
    
    fun personalizeMessage(message: Message, contacts: List<Contact>): List<IndividualMessage> {
        return contacts.map { contact ->
            val personalizedContent = personalizeTemplate(message.template, contact)
            
            IndividualMessage(
                id = UUID.randomUUID().toString(),
                messageId = message.id,
                contactId = contact.id,
                contactName = contact.name,
                phoneNumber = contact.primaryPhone ?: "",
                personalizedContent = personalizedContent
            )
        }
    }
    
    private fun personalizeTemplate(template: String, contact: Contact): String {
        var personalized = template
        
        // Replace name placeholder with display name (nickname if available, otherwise full name)
        personalized = personalized.replace(NAME_PLACEHOLDER, contact.displayName, ignoreCase = true)
        
        // Replace nickname placeholder
        contact.nickname?.let { nickname ->
            personalized = personalized.replace(NICKNAME_PLACEHOLDER, nickname, ignoreCase = true)
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
}
