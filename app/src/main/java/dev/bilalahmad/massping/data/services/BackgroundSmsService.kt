package dev.bilalahmad.massping.data.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import dev.bilalahmad.massping.data.models.IndividualMessage
import dev.bilalahmad.massping.data.repository.MassPingRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class BackgroundSmsService : Service() {

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "sms_sending_channel"
        const val ACTION_START_SENDING = "START_SENDING"
        const val ACTION_STOP_SENDING = "STOP_SENDING"
        const val EXTRA_MESSAGES = "messages"
        const val EXTRA_MESSAGE_ID = "message_id"
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var sendingJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_SENDING -> {
                val messages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableArrayListExtra(EXTRA_MESSAGES, IndividualMessage::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableArrayListExtra<IndividualMessage>(EXTRA_MESSAGES)
                }
                val messageId = intent.getStringExtra(EXTRA_MESSAGE_ID)

                if (messages != null && messageId != null) {
                    startForegroundService()
                    startSendingMessages(messages, messageId)
                }
            }
            ACTION_STOP_SENDING -> {
                stopSending()
            }
        }

        return START_NOT_STICKY
    }

    private fun startForegroundService() {
        val notification = createNotification("Preparing to send messages...")
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun startSendingMessages(messages: List<IndividualMessage>, messageId: String) {
        sendingJob = serviceScope.launch {
            val repository = MassPingRepository.getInstance(this@BackgroundSmsService)
            val totalCount = messages.size

            // Get SMS settings from shared preferences
            val sharedPrefs = getSharedPreferences("massping_preferences", MODE_PRIVATE)
            val delaySeconds = sharedPrefs.getLong("sms_delay_seconds", 5L)
            val timeoutSeconds = sharedPrefs.getLong("sms_timeout_seconds", 10L)
            val delayMs = delaySeconds * 1000L

            // Track progress through repository's individualMessages flow
            launch {
                repository.individualMessages.collect { individualMessagesMap ->
                    val currentMessages = individualMessagesMap[messageId] ?: emptyList()
                    if (currentMessages.isNotEmpty()) {
                        val sentCount = currentMessages.count {
                            it.status == dev.bilalahmad.massping.data.models.IndividualMessageStatus.SENT ||
                            it.status == dev.bilalahmad.massping.data.models.IndividualMessageStatus.DELIVERED
                        }
                        val failedCount = currentMessages.count {
                            it.status == dev.bilalahmad.massping.data.models.IndividualMessageStatus.FAILED
                        }

                        updateProgressNotification(sentCount, failedCount, totalCount)
                    }
                }
            }

            messages.forEachIndexed { index, message ->
                try {
                    // Update notification with current sending status
                    updateNotification("Sending ${index + 1} of $totalCount to ${message.contactName}")

                    // Send the SMS through repository (single SmsService)
                    repository.sendIndividualSms(message, timeoutSeconds)

                    // Delay between messages using configurable delay
                    if (index < messages.size - 1) { // Don't delay after the last message
                        delay(delayMs)
                    }

                } catch (e: Exception) {
                    android.util.Log.e("BackgroundSmsService", "Error sending SMS to ${message.contactName}", e)
                }
            }

            // Wait for all messages to complete (sent/delivered/failed)
            // Give extra time for all confirmations to come in
            delay(timeoutSeconds * 1000L + 2000L)

            // Update final notification with current status
            val finalMessages = repository.individualMessages.value[messageId] ?: emptyList()
            val finalSentCount = finalMessages.count {
                it.status == dev.bilalahmad.massping.data.models.IndividualMessageStatus.SENT ||
                it.status == dev.bilalahmad.massping.data.models.IndividualMessageStatus.DELIVERED
            }
            val finalFailedCount = finalMessages.count {
                it.status == dev.bilalahmad.massping.data.models.IndividualMessageStatus.FAILED
            }
            updateCompletionNotification(finalSentCount, finalFailedCount, totalCount)

            // Stop service after showing completion
            delay(5000L)
            stopSelf()
        }
    }

    private fun stopSending() {
        sendingJob?.cancel()
        updateNotification("Message sending stopped")
        stopSelf()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "SMS Sending",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows progress while sending bulk SMS messages"
        }

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification(content: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentTitle("MassPing - Sending Messages")
            .setContentText(content)
            .setOngoing(true)
            .setProgress(0, 0, true)
            .build()
    }

    private fun updateNotification(content: String) {
        val notification = createNotification(content)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun updateProgressNotification(sentCount: Int, failedCount: Int, totalCount: Int) {
        val progress = sentCount + failedCount
        val percentage = if (totalCount > 0) (progress * 100) / totalCount else 0

        val title = "MassPing - Sending Messages ($percentage%)"
        val content = "Sent: $sentCount | Failed: $failedCount | Remaining: ${totalCount - progress}"

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentTitle(title)
            .setContentText(content)
            .setOngoing(true)
            .setProgress(totalCount, progress, false)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun updateCompletionNotification(sentCount: Int, failedCount: Int, totalCount: Int) {
        val successRate = if (totalCount > 0) (sentCount * 100) / totalCount else 0
        val title = "MassPing - Completed"
        val content = "✅ $sentCount sent | ❌ $failedCount failed | $successRate% success rate"

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentTitle(title)
            .setContentText(content)
            .setOngoing(false)
            .setAutoCancel(true)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }


    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
