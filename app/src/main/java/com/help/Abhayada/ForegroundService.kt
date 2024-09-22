package com.help.Abhayada

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.compose.runtime.Composable
import androidx.core.app.NotificationCompat

class AreaAlertService : Service() {

    override fun onCreate() {
        super.onCreate()
        showNotification()
    }

    private fun showNotification() {
        val notificationChannelId = "CUSTOM_AREA_ALERT_CHANNEL"

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                notificationChannelId,
                "Custom Area Alerts",
                NotificationManager.IMPORTANCE_HIGH
            )
        }

        val notification: Notification = NotificationCompat.Builder(this, notificationChannelId)
            .setContentTitle("Alert")
            .setContentText("You are in a custom area. Be aware, look around.")
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        startForeground(1, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}