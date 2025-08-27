package dev.bilalahmad.massping.data.models

data class ContactGroup(
    val id: String,
    val name: String,
    val contactIds: List<String> = emptyList(),
    val isSelected: Boolean = false
) {
    val contactCount: Int
        get() = contactIds.size
}
