package com.example.amicitia.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.amicitia.MainActivity
import com.example.amicitia.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class AmicitiaMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // 之後要做：把 token 寫回 users/{uid}，用於「指定用戶推播」
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val data = message.data
        val roomId = data["roomId"].orEmpty()
        val title = data["title"] ?: message.notification?.title ?: "新訊息"
        val body = data["body"] ?: message.notification?.body ?: "你有一則新訊息"

        showChatNotification(
            context = this,
            roomId = roomId,
            title = title,
            body = body
        )
    }

    private fun showChatNotification(
        context: Context,
        roomId: String,
        title: String,
        body: String
    ) {
        val channelId = "chat_messages"


        val nmCompat = NotificationManagerCompat.from(context)
        if (!nmCompat.areNotificationsEnabled()) return

        // Android 8+ 必須先建立 Channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val ch = NotificationChannel(
                channelId,
                "聊天訊息",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "顯示聊天新訊息通知"
            }
            nm.createNotificationChannel(ch)
        }

        // deep link：amicitia://chat/room/{roomId}
        val uri = if (roomId.isNotBlank()) {
            Uri.parse("amicitia://chat/room/$roomId")
        } else {
            Uri.parse("amicitia://chat/room")
        }

        val intent = Intent(Intent.ACTION_VIEW, uri, context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            /* requestCode = */ 0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val notificationId = roomId.hashCode().takeIf { it != 0 } ?: 1
        nmCompat.notify(notificationId, notification)
    }
}