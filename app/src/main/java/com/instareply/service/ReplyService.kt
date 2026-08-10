package com.instareply.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.instareply.R
import com.instareply.ui.main.MainActivity

class ReplyService : Service() {

    companion object {
        private const val CHANNEL_ID = "instareply_service"
        private const val NOTIFICATION_ID = 1
        private const val ACTION_REPLY = "com.instareply.ACTION_REPLY"
        private const val EXTRA_MESSAGE = "com.instareply.EXTRA_MESSAGE"
        private const val EXTRA_RECIPIENT = "com.instareply.EXTRA_RECIPIENT"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_REPLY -> {
                val recipient = intent.getStringExtra(EXTRA_RECIPIENT)
                val message = intent.getStringExtra(EXTRA_MESSAGE)
                if (recipient != null && message != null) {
                    handleReply(recipient, message)
                }
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun handleReply(recipient: String, message: String) {
        val accessibilityService = InstaAccessibilityService.instance
        if (accessibilityService != null) {
            accessibilityService.openInstagramChat(recipient)
            // Delay to allow Instagram to open
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                accessibilityService.typeAndSendMessage(message)
            }, 2000)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "InstaReply Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Running in background to reply to messages"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun startForeground() {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("InstaReply Active")
            .setContentText("Listening for Instagram messages")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }
}
