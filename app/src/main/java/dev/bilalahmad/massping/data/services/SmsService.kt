package dev.bilalahmad.massping.data.services

import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.telephony.SmsManager
import dev.bilalahmad.massping.data.models.IndividualMessage
import dev.bilalahmad.massping.data.models.IndividualMessageStatus
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

class SmsService(private val context: Context) {
    
    companion object {
        private const val SMS_SENT_ACTION = "SMS_SENT"
        private const val SMS_DELIVERED_ACTION = "SMS_DELIVERED"
    }
    
    private val smsManager = SmsManager.getDefault()
    private val statusUpdatesChannel = Channel<Pair<String, IndividualMessageStatus>>(Channel.UNLIMITED)
    
    val statusUpdates: Flow<Pair<String, IndividualMessageStatus>> = statusUpdatesChannel.receiveAsFlow()
    
    private val sentReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val messageId = intent?.getStringExtra("messageId") ?: return
            val status = when (resultCode) {
                Activity.RESULT_OK -> {
                    android.util.Log.d("SmsService", "SMS sent successfully for message: $messageId")
                    IndividualMessageStatus.SENT
                }
                SmsManager.RESULT_ERROR_GENERIC_FAILURE -> {
                    android.util.Log.e("SmsService", "SMS failed - Generic failure for message: $messageId")
                    IndividualMessageStatus.FAILED
                }
                SmsManager.RESULT_ERROR_NO_SERVICE -> {
                    android.util.Log.e("SmsService", "SMS failed - No service for message: $messageId")
                    IndividualMessageStatus.FAILED
                }
                SmsManager.RESULT_ERROR_NULL_PDU -> {
                    android.util.Log.e("SmsService", "SMS failed - Null PDU for message: $messageId")
                    IndividualMessageStatus.FAILED
                }
                SmsManager.RESULT_ERROR_RADIO_OFF -> {
                    android.util.Log.e("SmsService", "SMS failed - Radio off for message: $messageId")
                    IndividualMessageStatus.FAILED
                }
                else -> {
                    android.util.Log.e("SmsService", "SMS failed - Unknown error ($resultCode) for message: $messageId")
                    IndividualMessageStatus.FAILED
                }
            }
            statusUpdatesChannel.trySend(messageId to status)
        }
    }
    
    private val deliveredReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val messageId = intent?.getStringExtra("messageId") ?: return
            statusUpdatesChannel.trySend(messageId to IndividualMessageStatus.DELIVERED)
        }
    }
    
    init {
        context.registerReceiver(sentReceiver, IntentFilter(SMS_SENT_ACTION), Context.RECEIVER_NOT_EXPORTED)
        context.registerReceiver(deliveredReceiver, IntentFilter(SMS_DELIVERED_ACTION), Context.RECEIVER_NOT_EXPORTED)
    }
    
    fun sendSms(message: IndividualMessage) {
        try {
            // Validate phone number
            if (message.phoneNumber.isBlank()) {
                android.util.Log.e("SmsService", "Cannot send SMS - blank phone number for message: ${message.id}")
                statusUpdatesChannel.trySend(message.id to IndividualMessageStatus.FAILED)
                return
            }
            
            // Validate message content
            if (message.personalizedContent.isBlank()) {
                android.util.Log.e("SmsService", "Cannot send SMS - blank message content for message: ${message.id}")
                statusUpdatesChannel.trySend(message.id to IndividualMessageStatus.FAILED)
                return
            }
            
            android.util.Log.d("SmsService", "Sending SMS to ${message.phoneNumber} for message: ${message.id}")
            
            val sentIntent = PendingIntent.getBroadcast(
                context, message.id.hashCode(),
                Intent(SMS_SENT_ACTION).putExtra("messageId", message.id),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            val deliveredIntent = PendingIntent.getBroadcast(
                context, message.id.hashCode() + 1,
                Intent(SMS_DELIVERED_ACTION).putExtra("messageId", message.id),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            // Update status to sending
            statusUpdatesChannel.trySend(message.id to IndividualMessageStatus.SENDING)
            
            // Send SMS
            smsManager.sendTextMessage(
                message.phoneNumber,
                null,
                message.personalizedContent,
                sentIntent,
                deliveredIntent
            )
        } catch (e: Exception) {
            android.util.Log.e("SmsService", "Exception sending SMS for message: ${message.id}", e)
            statusUpdatesChannel.trySend(message.id to IndividualMessageStatus.FAILED)
        }
    }
    
    suspend fun sendBatchWithDelay(messages: List<IndividualMessage>, delayBetweenMessages: Long = 2000L) {
        messages.forEach { message ->
            sendSms(message)
            // Add delay between messages to prevent carrier blocking
            kotlinx.coroutines.delay(delayBetweenMessages)
        }
    }
    
    fun cleanup() {
        try {
            context.unregisterReceiver(sentReceiver)
            context.unregisterReceiver(deliveredReceiver)
        } catch (e: Exception) {
            // Receivers might already be unregistered
        }
        statusUpdatesChannel.close()
    }
}
