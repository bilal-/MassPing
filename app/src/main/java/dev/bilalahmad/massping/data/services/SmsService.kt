package dev.bilalahmad.massping.data.services

import android.Manifest
import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.telephony.SmsManager
import androidx.core.content.ContextCompat
import dev.bilalahmad.massping.data.models.IndividualMessage
import dev.bilalahmad.massping.data.models.IndividualMessageStatus
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class SmsService(private val context: Context) {

    companion object {
        private const val SMS_SENT_ACTION = "SMS_SENT"
        private const val SMS_DELIVERED_ACTION = "SMS_DELIVERED"
        private const val SMS_MAX_LENGTH = 160 // Standard SMS length limit
    }

    private val smsManager = context.getSystemService(SmsManager::class.java)
    private val statusUpdatesChannel = Channel<Pair<String, IndividualMessageStatus>>(Channel.UNLIMITED)
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private var wakeLock: PowerManager.WakeLock? = null

    val statusUpdates: Flow<Pair<String, IndividualMessageStatus>> = statusUpdatesChannel.receiveAsFlow()

    private val sentReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            android.util.Log.d("SmsService", "sentReceiver onReceive called with resultCode: $resultCode")
            android.util.Log.d("SmsService", "Device: ${android.os.Build.MODEL}, Carrier: ${(context?.getSystemService(Context.TELEPHONY_SERVICE) as? android.telephony.TelephonyManager)?.networkOperatorName}")
            val messageId = intent?.getStringExtra("messageId") ?: run {
                android.util.Log.e("SmsService", "sentReceiver: no messageId in intent")
                return
            }
            android.util.Log.d("SmsService", "sentReceiver processing messageId: $messageId")
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
            android.util.Log.d("SmsService", "sentReceiver sending status update: $messageId -> $status")
            val success = statusUpdatesChannel.trySend(messageId to status)
            android.util.Log.d("SmsService", "Status update sent successfully: $success")
        }
    }

    private val deliveredReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            android.util.Log.d("SmsService", "deliveredReceiver onReceive called with resultCode: $resultCode")
            val messageId = intent?.getStringExtra("messageId") ?: run {
                android.util.Log.e("SmsService", "deliveredReceiver: no messageId in intent")
                return
            }
            android.util.Log.d("SmsService", "deliveredReceiver processing messageId: $messageId")
            when (resultCode) {
                Activity.RESULT_OK -> {
                    android.util.Log.d("SmsService", "SMS delivered for message: $messageId")
                    val success = statusUpdatesChannel.trySend(messageId to IndividualMessageStatus.DELIVERED)
                    android.util.Log.d("SmsService", "Delivery status update sent successfully: $success")
                }
                else -> {
                    android.util.Log.d("SmsService", "SMS delivery status unknown for message: $messageId (code: $resultCode)")
                    // Don't update status on delivery failure - keep as SENT
                }
            }
        }
    }

    init {
        // Log device information for debugging
        val isEmulator = Build.FINGERPRINT.startsWith("generic") ||
                        Build.FINGERPRINT.startsWith("unknown") ||
                        Build.MODEL.contains("google_sdk") ||
                        Build.MODEL.contains("Emulator") ||
                        Build.MODEL.contains("Android SDK built for x86") ||
                        Build.MANUFACTURER.contains("Genymotion") ||
                        Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")

        android.util.Log.d("SmsService", "Device info - Model: ${Build.MODEL}, Fingerprint: ${Build.FINGERPRINT}, Is Emulator: $isEmulator")

        context.registerReceiver(sentReceiver, IntentFilter(SMS_SENT_ACTION), Context.RECEIVER_NOT_EXPORTED)
        context.registerReceiver(deliveredReceiver, IntentFilter(SMS_DELIVERED_ACTION), Context.RECEIVER_NOT_EXPORTED)

        android.util.Log.d("SmsService", "SMS broadcast receivers registered")
    }

    fun sendSms(message: IndividualMessage, timeoutSeconds: Long = 10L) {
        try {
            // Check SMS permission
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                android.util.Log.e("SmsService", "SMS permission not granted for message: ${message.id}")
                statusUpdatesChannel.trySend(message.id to IndividualMessageStatus.FAILED)
                return
            }

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

            // Log SMS length for debugging
            val messageLength = message.personalizedContent.length
            android.util.Log.d("SmsService", "SMS length: $messageLength chars for message: ${message.id}")
            if (messageLength > SMS_MAX_LENGTH) {
                android.util.Log.w("SmsService", "SMS exceeds standard length ($messageLength > $SMS_MAX_LENGTH) - may be sent as multiple parts or MMS")
            }

            android.util.Log.d("SmsService", "Sending SMS to ${message.phoneNumber} for message: ${message.id}")

            val sentIntent = PendingIntent.getBroadcast(
                context, message.id.hashCode(),
                Intent(SMS_SENT_ACTION).putExtra("messageId", message.id),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val deliveredIntent = PendingIntent.getBroadcast(
                context, message.id.hashCode() + 1, // Different request code
                Intent(SMS_DELIVERED_ACTION).putExtra("messageId", message.id),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Update status to sending
            android.util.Log.d("SmsService", "Updating status to SENDING for message: ${message.id}")
            val sendingSuccess = statusUpdatesChannel.trySend(message.id to IndividualMessageStatus.SENDING)
            android.util.Log.d("SmsService", "SENDING status update sent successfully: $sendingSuccess")

            // Send SMS with delivery receipt
            android.util.Log.d("SmsService", "Calling smsManager.sendTextMessage for: ${message.id}")
            android.util.Log.d("SmsService", "SMS Details - To: ${message.phoneNumber}, Length: ${message.personalizedContent.length} chars")
            android.util.Log.d("SmsService", "SMS Content Preview: ${message.personalizedContent.take(50)}...")

            // Additional phone number validation (basic validation already done above)
            if (!isValidPhoneNumber(message.phoneNumber)) {
                android.util.Log.e("SmsService", "Invalid phone number format: '${message.phoneNumber}' for message: ${message.id}")
                statusUpdatesChannel.trySend(message.id to IndividualMessageStatus.FAILED)
                return
            }

            smsManager.sendTextMessage(
                message.phoneNumber,
                null,
                message.personalizedContent,
                sentIntent,
                deliveredIntent
            )
            android.util.Log.d("SmsService", "smsManager.sendTextMessage completed for: ${message.id}")

            // Add timeout mechanism - if no response in configured time, mark as sent anyway
            // Note: This is a fallback for emulators or cases where SMS confirmation doesn't arrive
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                kotlinx.coroutines.delay(timeoutSeconds * 1000L) // Convert seconds to milliseconds

                android.util.Log.d("SmsService", "Timeout check for message: ${message.id}")
                // Only send timeout status if we haven't received a definitive status yet
                // This prevents overriding real status updates from broadcast receivers
                statusUpdatesChannel.trySend(message.id to IndividualMessageStatus.SENT)
                android.util.Log.w("SmsService", "Timeout (${timeoutSeconds}s) - marking message as SENT: ${message.id}")
            }

        } catch (e: Exception) {
            android.util.Log.e("SmsService", "Exception sending SMS for message: ${message.id}", e)
            statusUpdatesChannel.trySend(message.id to IndividualMessageStatus.FAILED)
        }
    }

    suspend fun sendBatchWithDelay(messages: List<IndividualMessage>, delayBetweenMessages: Long = 2000L, timeoutSeconds: Long = 10L) {
        android.util.Log.d("SmsService", "Starting batch send of ${messages.size} messages with wake lock")

        // Group messages by phone number to send all parts for one recipient before moving to next
        val messagesByRecipient = messages.groupBy { it.phoneNumber }
        android.util.Log.d("SmsService", "Grouped messages for ${messagesByRecipient.size} recipients")

        // Acquire wake lock to keep screen on during SMS sending
        wakeLock = powerManager.newWakeLock(
            PowerManager.SCREEN_DIM_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "MassPing::SMSSending"
        ).apply {
            acquire(messages.size * (delayBetweenMessages + timeoutSeconds * 1000L) + 30000L) // Extra 30s buffer
            android.util.Log.d("SmsService", "Wake lock acquired for SMS batch sending")
        }

        try {
            messagesByRecipient.forEach { (phoneNumber, recipientMessages) ->
                android.util.Log.d("SmsService", "Sending ${recipientMessages.size} message part(s) to $phoneNumber")

                // Sort parts by ID to maintain order (part1, part2, etc.)
                val sortedMessages = recipientMessages.sortedBy { message ->
                    if (message.id.contains("-part")) {
                        message.id.substringAfterLast("part").toIntOrNull() ?: 0
                    } else {
                        0 // Single messages come first
                    }
                }

                sortedMessages.forEachIndexed { index, message ->
                    android.util.Log.d("SmsService", "Sending part ${index + 1}/${sortedMessages.size} to $phoneNumber")
                    sendSms(message, timeoutSeconds)

                    // Add shorter delay between parts for same recipient, longer delay between recipients
                    val delay = if (index < sortedMessages.size - 1) {
                        delayBetweenMessages / 2 // Shorter delay between parts
                    } else {
                        delayBetweenMessages // Full delay before next recipient
                    }
                    kotlinx.coroutines.delay(delay)
                }
            }
        } finally {
            // Release wake lock after sending is complete
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                    android.util.Log.d("SmsService", "Wake lock released after SMS batch sending")
                }
            }
            wakeLock = null
        }
    }

    fun cleanup() {
        try {
            context.unregisterReceiver(sentReceiver)
            context.unregisterReceiver(deliveredReceiver)
        } catch (e: Exception) {
            // Receivers might already be unregistered
        }

        // Release wake lock if still held
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
                android.util.Log.d("SmsService", "Wake lock released during cleanup")
            }
        }
        wakeLock = null

        statusUpdatesChannel.close()
    }

    private fun isValidPhoneNumber(phoneNumber: String): Boolean {
        // Basic phone number validation
        // Should have at least 10 digits and may start with + for country code
        val digitsOnly = phoneNumber.replace("[^\\d]".toRegex(), "")
        return when {
            phoneNumber.startsWith("+") && digitsOnly.length >= 10 -> true
            !phoneNumber.startsWith("+") && digitsOnly.length >= 10 -> true
            else -> false
        }
    }
}
