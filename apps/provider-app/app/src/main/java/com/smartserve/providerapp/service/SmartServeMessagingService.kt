package com.smartserve.providerapp.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.smartserve.providerapp.MainActivity
import com.smartserve.providerapp.R

class SmartServeMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        saveTokenToFirestore(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        val title = remoteMessage.data["title"]
            ?: remoteMessage.notification?.title
            ?: "New Message"
        val body = remoteMessage.data["body"]
            ?: remoteMessage.notification?.body
            ?: ""
        val bookingId = remoteMessage.data["bookingId"] ?: ""
        showNotification(title, body, bookingId)
    }

    private fun showNotification(title: String, body: String, bookingId: String) {
        val channelId = "chat_messages"
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Chat Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for new chat messages"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("bookingId", bookingId)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, bookingId.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val remoteInput = RemoteInput.Builder("key_reply_text")
            .setLabel("Type a reply...")
            .build()

        val replyIntent = Intent(this, MainActivity::class.java).apply {
            action = "ACTION_REPLY"
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("bookingId", bookingId)
        }

        val replyPendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getActivity(
                this,
                bookingId.hashCode() + 1,
                replyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
        } else {
            PendingIntent.getActivity(
                this,
                bookingId.hashCode() + 1,
                replyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        val replyAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_send,
            "Reply",
            replyPendingIntent
        )
            .addRemoteInput(remoteInput)
            .setAllowGeneratedReplies(true)
            .build()

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .addAction(replyAction)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)  
            .setGroupSummary(false)     
            .build()

        notificationManager.notify(bookingId.hashCode(), notification)
    }

    private fun saveTokenToFirestore(token: String) {
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance()
            .currentUser?.uid ?: return
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("provider_profiles")
            .document(uid)
            .update("fcmToken", token)
    }
}
