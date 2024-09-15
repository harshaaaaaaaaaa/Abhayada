package com.help.Abhayada

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.ParcelUuid
import androidx.core.app.NotificationCompat
import java.util.UUID
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

class BluetoothScanService : Service() {

    private lateinit var bluetoothLeScanner: BluetoothLeScanner
    private var bluetoothGattServer: BluetoothGattServer? = null

    override fun onCreate() {
        super.onCreate()
        bluetoothLeScanner = (getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter.bluetoothLeScanner
        startForegroundService()
        startScanning()
    }

    private fun startForegroundService() {
        val channelId = "BluetoothScanServiceChannel"
        val channel = NotificationChannel(
            channelId,
            "Bluetooth Scan Service Channel",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Bluetooth Scan Service")
            .setContentText("Scanning for Bluetooth devices...")
            .setSmallIcon(R.drawable.ic_notification)
            .build()

        startForeground(1, notification)
    }


    private fun startScanning() {
        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
            .build()
        val scanFilter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(UUID.fromString("0000FEAA-0000-1000-8000-00805F9B34FB")))
            .build()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
            try {
                bluetoothLeScanner.startScan(listOf(scanFilter), scanSettings, scanCallback)
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        }
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            // Handle scan result
        }

        override fun onBatchScanResults(results: List<ScanResult>) {
            super.onBatchScanResults(results)
            // Handle batch scan results
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            // Handle scan failure
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startScanning()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
            try {
                bluetoothLeScanner.stopScan(scanCallback)
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED) {
            try {
                bluetoothGattServer?.close()
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        }
    }
}