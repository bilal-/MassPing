package dev.bilalahmad.massping.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "message_history")
data class MessageHistory(
    @PrimaryKey
    val id: String,
    val template: String,
    val recipientCount: Int,
    val sentCount: Int,
    val deliveredCount: Int,
    val failedCount: Int,
    val createdAt: Long,
    val sentAt: Long?,
    val recipientGroupNames: String // JSON array of group names
)