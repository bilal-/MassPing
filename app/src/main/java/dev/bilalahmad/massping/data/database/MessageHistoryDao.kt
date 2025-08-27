package dev.bilalahmad.massping.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageHistoryDao {

    @Query("SELECT * FROM message_history ORDER BY sentAt DESC, createdAt DESC")
    fun getAllMessagesFlow(): Flow<List<MessageHistory>>

    @Query("SELECT * FROM message_history ORDER BY sentAt DESC, createdAt DESC")
    suspend fun getAllMessages(): List<MessageHistory>

    @Query("SELECT * FROM message_history WHERE id = :id")
    suspend fun getMessageById(id: String): MessageHistory?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageHistory)

    @Update
    suspend fun updateMessage(message: MessageHistory)

    @Delete
    suspend fun deleteMessage(message: MessageHistory)

    @Query("DELETE FROM message_history WHERE id = :id")
    suspend fun deleteMessageById(id: String)

    @Query("SELECT COUNT(*) FROM message_history WHERE sentAt IS NOT NULL")
    suspend fun getSentMessageCount(): Int

    @Query("SELECT SUM(sentCount) FROM message_history WHERE sentAt IS NOT NULL")
    suspend fun getTotalSentSmsCount(): Int?

    @Query("SELECT SUM(deliveredCount) FROM message_history WHERE sentAt IS NOT NULL")
    suspend fun getTotalDeliveredSmsCount(): Int?
}