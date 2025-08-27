package dev.bilalahmad.massping.data.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ContactAccount(
    val name: String,
    val type: String,
    val displayName: String = name
) : Parcelable {

    val isGoogleAccount: Boolean
        get() = type == "com.google"

    val accountTypeDisplayName: String
        get() = when (type) {
            "com.google" -> "Google"
            "com.android.exchange" -> "Exchange"
            "com.facebook.auth.login" -> "Facebook"
            "com.whatsapp" -> "WhatsApp"
            else -> type.substringAfterLast('.').replaceFirstChar { it.titlecase() }
        }
}