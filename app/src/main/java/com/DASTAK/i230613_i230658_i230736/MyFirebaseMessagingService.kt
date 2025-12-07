package com.DASTAK.i230613_i230658_i230736

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.d(TAG, "=== FCM MESSAGE RECEIVED ===")
        Log.d(TAG, "From: ${remoteMessage.from}")
        Log.d(TAG, "Message ID: ${remoteMessage.messageId}")
        Log.d(TAG, "Message Type: ${remoteMessage.messageType}")
        Log.d(TAG, "Sent Time: ${remoteMessage.sentTime}")

        // Check if message contains a notification payload
        remoteMessage.notification?.let { notification ->
            Log.d(TAG, "=== NOTIFICATION PAYLOAD ===")
            Log.d(TAG, "Title: ${notification.title}")
            Log.d(TAG, "Body: ${notification.body}")
            Log.d(TAG, "Icon: ${notification.icon}")
            Log.d(TAG, "Sound: ${notification.sound}")
            Log.d(TAG, "Tag: ${notification.tag}")
            Log.d(TAG, "Color: ${notification.color}")
            Log.d(TAG, "Click Action: ${notification.clickAction}")

            sendNotification(
                notification.title ?: "DASTAK",
                notification.body ?: "",
                remoteMessage.data
            )
        } ?: run {
            Log.w(TAG, "Notification payload is NULL")
        }

        // Check if message contains a data payload
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "=== DATA PAYLOAD ===")
            remoteMessage.data.forEach { (key, value) ->
                Log.d(TAG, "Data[$key]: $value")
            }

            // If only data payload (no notification), create notification from data
            if (remoteMessage.notification == null) {
                Log.d(TAG, "Creating notification from data payload")
                val title = remoteMessage.data["title"] ?: "DASTAK"
                val body = remoteMessage.data["body"] ?: remoteMessage.data["message"] ?: ""
                sendNotification(title, body, remoteMessage.data)
            }
        } else {
            Log.w(TAG, "Data payload is EMPTY")
        }

        Log.d(TAG, "=== END FCM MESSAGE ===")
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "=== NEW FCM TOKEN GENERATED ===")
        Log.d(TAG, "New Token: $token")
        Log.d(TAG, "Token Length: ${token.length}")

        // Save token to SharedPreferences for debugging
        val sharedPref = getSharedPreferences("fcm_prefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("fcm_token", token)
            putLong("token_timestamp", System.currentTimeMillis())
            apply()
        }
        Log.d(TAG, "Token saved to SharedPreferences")

        // Save new token to server
        try {
            FCMTokenService.initializeFCM(applicationContext)
            Log.d(TAG, "FCM Token service initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing FCM Token service", e)
        }
    }

    override fun onDeletedMessages() {
        super.onDeletedMessages()
        Log.w(TAG, "=== FCM MESSAGES DELETED ===")
        Log.w(TAG, "Some messages were deleted on the server")
    }

    override fun onMessageSent(msgId: String) {
        super.onMessageSent(msgId)
        Log.d(TAG, "=== FCM MESSAGE SENT ===")
        Log.d(TAG, "Message ID: $msgId")
    }

    override fun onSendError(msgId: String, exception: Exception) {
        super.onSendError(msgId, exception)
        Log.e(TAG, "=== FCM SEND ERROR ===")
        Log.e(TAG, "Message ID: $msgId")
        Log.e(TAG, "Error: ${exception.message}", exception)
    }

    private fun sendNotification(title: String, messageBody: String, data: Map<String, String>) {
        Log.d(TAG, "=== SENDING NOTIFICATION ===")
        Log.d(TAG, "Title: $title")
        Log.d(TAG, "Body: $messageBody")
        Log.d(TAG, "Data size: ${data.size}")

        try {
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

            // Add data to intent based on notification type
            val notificationType = data["type"]
            Log.d(TAG, "Notification Type: $notificationType")

            when (notificationType) {
                "registration_request", "registration_accepted", "registration_rejected" -> {
                    intent.putExtra("open_notifications", true)
                    Log.d(TAG, "Added open_notifications extra to intent")
                }
                "volunteer_removed" -> {
                    intent.putExtra("open_notifications", true)
                    Log.d(TAG, "Added open_notifications extra to intent (volunteer removed)")
                }
            }

            val pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            Log.d(TAG, "PendingIntent created successfully")

            val channelId = "dastak_notifications"

            // Use a default notification icon if custom one doesn't exist
            val notificationIcon = try {
                R.drawable.ic_notification
            } catch (e: Exception) {
                Log.w(TAG, "Custom notification icon not found, using default")
                android.R.drawable.ic_dialog_info
            }

            val notificationBuilder = NotificationCompat.Builder(this, channelId)
                .setSmallIcon(notificationIcon)
                .setContentTitle(title)
                .setContentText(messageBody)
                .setAutoCancel(true)
                .setSound(android.provider.Settings.System.DEFAULT_NOTIFICATION_URI)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setDefaults(NotificationCompat.DEFAULT_ALL)

            Log.d(TAG, "NotificationBuilder configured")

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Create notification channel for Android O and above
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    "DASTAK Notifications",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifications for volunteer opportunities and updates"
                    enableLights(true)
                    enableVibration(true)
                    setShowBadge(true)
                }
                notificationManager.createNotificationChannel(channel)
                Log.d(TAG, "Notification channel created: $channelId")
            }

            val notificationId = System.currentTimeMillis().toInt()
            notificationManager.notify(notificationId, notificationBuilder.build())
            Log.d(TAG, "Notification displayed with ID: $notificationId")

        } catch (e: Exception) {
            Log.e(TAG, "Error sending notification", e)
            Log.e(TAG, "Error details: ${e.message}")
            Log.e(TAG, "Stack trace: ${e.stackTraceToString()}")
        }
    }

    companion object {
        private const val TAG = "FCMService"
    }
}