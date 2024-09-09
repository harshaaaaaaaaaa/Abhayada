package com.help.Abhayada

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class ShakeService : Service() {

    private lateinit var shakeDetector: ShakeDetector
    private val bluetoothAdapter: BluetoothAdapter by lazy {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    override fun onCreate() {
        super.onCreate()
        shakeDetector = ShakeDetector(this@ShakeService) {
            val intent = Intent("com.example.broad.SHAKE_DETECTED")
            sendBroadcast(intent)
        }
        shakeDetector.start()

        val filter = IntentFilter("com.example.broad.SHAKE_DETECTED")
        registerReceiver(shakeReceiver, filter)

        startForegroundService()
    }

    private fun startForegroundService() {
        val channelId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        } else {
            ""
        }

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
                startBluetoothGattServer()
            }
        }
    }

    private fun startBluetoothGattServer() {
        // Implement logic to start Bluetooth GATT server and begin broadcasting
        // Example:
        // val gattServer = bluetoothAdapter.bluetoothLeAdvertiser
        // gattServer.startAdvertising(...)
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