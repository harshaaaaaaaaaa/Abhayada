package com.help.Abhayada

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import androidx.core.app.NotificationCompat
import android.app.AlertDialog
import android.bluetooth.le.AdvertiseSettings
import android.os.Handler
import android.os.Looper

class ShakeService : Service() {

    private lateinit var shakeDetector: ShakeDetector
    override fun onCreate() {
        super.onCreate()
        shakeDetector = ShakeDetector(this@ShakeService) {
            val intent = Intent("com.example.broad.SHAKE_DETECTED")
            sendBroadcast(intent)
        }
        shakeDetector.start()

        val filter = IntentFilter("com.example.broad.SHAKE_DETECTED")
        registerReceiver(shakeReceiver, filter, RECEIVER_NOT_EXPORTED)

        startForegroundService()
    }

    private fun startForegroundService() {
        val channelId =
            createNotificationChannel()

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Shake Service")
            .setContentText("Listening for shake events")
            .setSmallIcon(R.drawable.ic_notification)
            .build()

        startForeground(1, notification)
    }

    private fun createNotificationChannel(): String {
        val channelId = "ShakeServiceChannel"
        val channelName = "Shake Service Channel"
        val chan = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT)
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(chan)
        return channelId
    }

    private val shakeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.example.broad.SHAKE_DETECTED") {
                val builder = AlertDialog.Builder(this@ShakeService)
                builder.setTitle("Start Broadcasting")
                builder.setMessage("Do you want to start broadcasting?")
                builder.setPositiveButton("Yes") { _, _ ->
                    startAdvertising()
                }
                builder.setNegativeButton("No") { _, _ -> }
                val dialog = builder.create()
                dialog.show()

                // Automatically dismiss the dialog after 10 seconds
                Handler(Looper.getMainLooper()).postDelayed({
                    if (dialog.isShowing) {
                        dialog.dismiss()
                    }
                }, 10000)
            }
        }
    }

    private fun startAdvertising() {
        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(true)
            .build()

        BluetoothAdvertiser.startEddystoneAdvertising(this, settings)
        BluetoothAdvertiser.startIBeaconAdvertising(this, settings)
    }

    override fun onDestroy() {
        super.onDestroy()
        shakeDetector.stop()
        unregisterReceiver(shakeReceiver)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}