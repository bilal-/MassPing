package dev.bilalahmad.massping.data.models

data class Contact(
    val id: String,
    val name: String,
    val firstName: String? = null,
    val lastName: String? = null,
    val middleName: String? = null,
    val phoneNumbers: List<ContactPhone> = emptyList(),
    val emails: List<ContactEmail> = emptyList(),
    val nickname: String? = null,
    val company: String? = null,
    val jobTitle: String? = null,
    val department: String? = null,
    val addresses: List<ContactAddress> = emptyList(),
    val birthday: String? = null,
    val websites: List<ContactWebsite> = emptyList(),
    val notes: String? = null,
    val customFields: Map<String, String> = emptyMap(),
    val groups: List<String> = emptyList(),
    val photoUri: String? = null,
    val lastModified: Long? = null
) {
    val displayName: String
        get() = nickname?.takeIf { it.isNotBlank() } ?: name
    
    val primaryPhone: String?
        get() = phoneNumbers.firstOrNull { it.isPrimary }?.number 
            ?: phoneNumbers.firstOrNull { it.isMobile }?.number 
            ?: phoneNumbers.firstOrNull()?.number
    
    val mobilePhone: String?
        get() = phoneNumbers.firstOrNull { it.isMobile }?.number
    
    val primaryEmail: String?
        get() = emails.firstOrNull { it.isPrimary }?.address ?: emails.firstOrNull()?.address
}

data class ContactPhone(
    val number: String,
    val type: String, // Mobile, Home, Work, Other
    val label: String? = null,
    val isPrimary: Boolean = false
) {
    val isMobile: Boolean
        get() = type.contains("Mobile", ignoreCase = true) || 
                type.contains("Cell", ignoreCase = true) ||
                label?.contains("Mobile", ignoreCase = true) == true ||
                label?.contains("Cell", ignoreCase = true) == true
}

data class ContactEmail(
    val address: String,
    val type: String, // Personal, Work, Other
    val label: String? = null,
    val isPrimary: Boolean = false
)

data class ContactAddress(
    val street: String? = null,
    val city: String? = null,
    val state: String? = null,
    val postalCode: String? = null,
    val country: String? = null,
    val type: String, // Home, Work, Other
    val label: String? = null,
    val fullAddress: String
)

data class ContactWebsite(
    val url: String,
    val type: String, // Personal, Work, Blog, Other
    val label: String? = null
)
