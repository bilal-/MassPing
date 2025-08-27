package dev.bilalahmad.massping.data.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import dev.bilalahmad.massping.data.models.IndividualMessage
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

    private lateinit var smsService: SmsService
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var sendingJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        smsService = SmsService(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_SENDING -> {
                val messages = intent.getParcelableArrayListExtra<IndividualMessage>(EXTRA_MESSAGES)
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
            var sentCount = 0
            val totalCount = messages.size

            messages.forEach { message ->
                try {
                    // Update notification with progress
                    updateNotification("Sending message ${sentCount + 1} of $totalCount to ${message.contactName}")

                    // Send the SMS
                    smsService.sendSms(message)
                    sentCount++

                    // Delay between messages to prevent carrier throttling
                    delay(2000L)

                } catch (e: Exception) {
                    // Log error but continue with next message
                }
            }

            // Update final notification
            updateNotification("Completed sending $sentCount of $totalCount messages")

            // Stop service after a delay to show completion
            delay(3000L)
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

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        smsService.cleanup()
    }
}
