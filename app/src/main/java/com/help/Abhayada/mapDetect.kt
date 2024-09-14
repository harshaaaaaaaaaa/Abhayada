package com.help.Abhayada

import android.location.Location
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat

fun isUserInMarkedArea(userLatitude: Double, userLongitude: Double, markedArea: MarkedArea): Boolean {
    val userLocation = Location("").apply {
        latitude = userLatitude
        longitude = userLongitude
    }
    val areaLocation = Location("").apply {
        latitude = markedArea.latitude
        longitude = markedArea.longitude
    }
    val distance = userLocation.distanceTo(areaLocation)
    return distance <= markedArea.radius
}

fun showAlertAndNotification(context: Context) {
    // Show dialog box
    AlertDialog.Builder(context)
        .setTitle("Alert")
        .setMessage("You are in a dancger area. Be aware, look around.")
        .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
        .show()

    // Send notification
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val notificationChannelId = "CUSTOM_AREA_ALERT_CHANNEL"

    val notificationChannel = NotificationChannel(
        notificationChannelId,
        "Custom Area Alerts",
        NotificationManager.IMPORTANCE_HIGH
    )
    notificationManager.createNotificationChannel(notificationChannel)

    val notification = NotificationCompat.Builder(context, notificationChannelId)
        .setContentTitle("Alert")
        .setContentText("You are in a custom area. Be aware, look around.")
        .setSmallIcon(R.drawable.ic_notification)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .build()

    notificationManager.notify(1, notification)
}