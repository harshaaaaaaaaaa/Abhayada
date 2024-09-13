package com.help.Abhayada

import BluetoothScanService
import android.Manifest
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.*
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.help.Abhayada.ui.theme.BroadTheme
import kotlinx.coroutines.delay
import java.util.*
import android.content.BroadcastReceiver
import android.content.Context
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.telephony.SmsManager
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color


class MainActivity : ComponentActivity() {


    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        BluetoothAdapter.getDefaultAdapter()
    }
    private var bluetoothLeAdvertiser: BluetoothLeAdvertiser? = null
    private var bluetoothLeScanner: BluetoothLeScanner? = null
    private var isGattServerRunning = false
    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying = false
    private var alertShown = false
    private lateinit var shakeDetector: ShakeDetector
    private val historyDevices = mutableStateListOf<Device>()
    private val discoveredDevices = mutableStateListOf<Device>()
    private val discoveredBeacons = mutableStateListOf<Device>()
    private val REQUEST_ENABLE_BT = 1
    private val REQUEST_PERMISSIONS = 2
    private lateinit var handler: Handler
    private var isBroadcasting = false
    private var advertisingFrequency = 15000L // Default 15 seconds
    private val minFrequency = 1000L // 1 second
    private val maxFrequency = 60000L // 60 seconds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handler = Handler(Looper.getMainLooper())
        setContent {
            BroadTheme {
                MainActivityContent()
                Scaffold(modifier = Modifier.fillMaxSize().background(color = Color.Black)) { innerPadding ->
                    Box(
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Button(onClick = { startAdvertising() }) {
                                Text(text = "Start Broadcasting")
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { stopAdvertising() }) {
                                Text(text = "Stop Broadcasting")
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { startScanning() }) {
                                Text(text = "Start Scanning")
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { stopScanning() }) {
                                Text(text = "Stop Scanning")
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { showDiscoveredDevicesWindow() }) {
                                Text(text = "Show Discovered Devices")
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { setContent { BroadTheme { ShowHistoryDevicesWindow() } } }) {
                                Text(text = "Show Discovered Devices")
                            }
                        }
                    }
                }
            }
        }

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show()
            return
        }
        if (!bluetoothAdapter!!.isEnabled) {
            showEnableBluetoothDialog()
        } else {
            requestAllPermissionsOnInstall()
            checkPermissions()
            checkAndRequestSmsPermission()
        }

        intent?.getStringExtra("device_address")?.let { deviceAddress ->
            showDiscoveredDevicesWindow()
        }

        shakeDetector = ShakeDetector(this) {
            if (!isAdvertising) {
                startAdvertising()
            }
        }
        shakeDetector.start()

        startScanning()

    }


    private fun playAlertSound() {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(this, R.raw.alert_sound) // Place your audio file in res/raw folder
            mediaPlayer?.isLooping = true
        }
        mediaPlayer?.let {
            if (!isPlaying)
                it.start()
                isPlaying = true
            }
        }

    private fun stopAlertSound() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
                it.reset()
                mediaPlayer = null
                isPlaying = false
            }
        }
    }

    /*private fun triggerVibration() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE / 2)) // Moderate intensity
    }*/

    private fun showEnableBluetoothDialog() {
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
        AlertDialog.Builder(this)
            .setTitle("Enable Bluetooth")
            .setMessage("This app requires Bluetooth to function. Please enable Bluetooth.")
            .setPositiveButton("Enable") { _, _ ->
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
            }
            .setNegativeButton("Cancel") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    } else {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_CONNECT), REQUEST_PERMISSIONS)
    }
}

    private fun checkPermissions() {
        val permissions = arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.SEND_SMS,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION,
            Manifest.permission.SYSTEM_ALERT_WINDOW,
            Manifest.permission.SEND_SMS,
            Manifest.permission.POST_NOTIFICATIONS,
        )
        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                REQUEST_PERMISSIONS
            )
        }
    }

    private var isAdvertising = false
    private fun startAdvertising() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED) {
            bluetoothLeAdvertiser = bluetoothAdapter?.bluetoothLeAdvertiser
            val settings = AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .setConnectable(true)
                .build()
            val data = AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .addServiceUuid(ParcelUuid(UUID.fromString("0000FEAA-0000-1000-8000-00805F9B34FB")))
                .build()
            bluetoothLeAdvertiser?.startAdvertising(settings, data, advertiseCallback)
            stopScanning()
            startEddystoneAdvertising(settings)
            startIBeaconAdvertising(settings)
            sendHelpSms("+918005320074")
            isAdvertising = true
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_ADVERTISE), REQUEST_PERMISSIONS)
        }
    }


    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            super.onStartSuccess(settingsInEffect)
            runOnUiThread {
                Toast.makeText(this@MainActivity, "Advertising started", Toast.LENGTH_SHORT).show()
            }
        }

        override fun onStartFailure(errorCode: Int) {
            super.onStartFailure(errorCode)
            val errorMessage = when (errorCode) {
                AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE -> "Data too large"
                AdvertiseCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> "Too many advertisers"
                AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED -> "Already started"
                AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR -> "Internal error"
                AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED -> "Feature unsupported"
                else -> "Unknown error"
            }
            runOnUiThread {
                Toast.makeText(
                    this@MainActivity,
                    "Advertising failed: $errorMessage",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun stopAdvertising() {
    if (isAdvertising) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED) {
            bluetoothLeAdvertiser?.stopAdvertising(advertiseCallback)
            stopEddystoneAdvertising()
            stopIBeaconAdvertising()
            isAdvertising = false
            Toast.makeText(this, "Broadcasting stopped", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Bluetooth Advertise permission not granted", Toast.LENGTH_SHORT).show()
        }
    }
}

    private fun startScanning() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
            bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner
            val scanSettings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()
            val scanFilter = ScanFilter.Builder()
                .setServiceUuid(ParcelUuid(UUID.fromString("0000FEAA-0000-1000-8000-00805F9B34FB")))
                .build()
            bluetoothLeScanner?.startScan(listOf(scanFilter), scanSettings, scanCallback)
            startForegroundService(Intent(this, BluetoothScanService::class.java))
            stopAlertSound()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_SCAN), REQUEST_PERMISSIONS)
        }
    }


    private fun stopScanning() {
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
        bluetoothLeScanner?.stopScan(scanCallback)
        alertShown = false // Reset the alertShown flag
    } else {
        Toast.makeText(this, "Bluetooth Scan permission not granted", Toast.LENGTH_SHORT).show()
    }
}


    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            val device: BluetoothDevice = result.device
            val deviceAddress = device.address
            val rssi = result.rssi

            val existingDevice = discoveredDevices.find { it.macAddress == deviceAddress }
            if (existingDevice != null) {
                existingDevice.rssi = rssi
                existingDevice.lastSeen = System.currentTimeMillis()
            } else {
                discoveredDevices.add(
                    Device(
                        id = UUID.randomUUID().toString(),
                        macAddress = deviceAddress,
                        rssi = rssi
                    )
                )
            }

            // Trigger system alert and notification only once
            if (!alertShown) {
                showSystemAlert(rssi)
                showNotification()
                alertShown = true
            }

        }

        override fun onBatchScanResults(results: List<ScanResult>) {
            super.onBatchScanResults(results)
            for (result in results) {
                onScanResult(ScanSettings.CALLBACK_TYPE_ALL_MATCHES, result)
            }
        }


        private fun showSystemAlert(rssi: Int) {
    if (Settings.canDrawOverlays(this@MainActivity)) {
        val alertDialog = AlertDialog.Builder(this@MainActivity)
            .setTitle("!!!HELP!!!")
            .setMessage("SOMEONE NEED HELP")
            .setPositiveButton("Reach") { dialog, _ ->
                dialog.dismiss()
                showDiscoveredDevicesWindow()
            }
            .setCancelable(false)
            .create()

        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )

        layoutParams.gravity = Gravity.CENTER
        alertDialog.window?.setType(layoutParams.type)
        alertDialog.show()

        // Play the alert sound
        playAlertSound()
    }
}


        private val REQUEST_CODE_SYSTEM_ALERT_WINDOW = 1001

        private fun showNotification() {
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notificationChannelId = "DEVICE_ALERT_CHANNEL"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val notificationChannel = NotificationChannel(
                    notificationChannelId,
                    "Device Alerts",
                    NotificationManager.IMPORTANCE_HIGH
                )
                notificationManager.createNotificationChannel(notificationChannel)
            }

            val notification = NotificationCompat.Builder(this@MainActivity, notificationChannelId)
                .setContentTitle("HELP NEEDED")
                .setContentText("Someone Need your Help")
                .setSmallIcon(R.drawable.ic_notification)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()

            notificationManager.notify(1, notification)
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            val errorMessage = when (errorCode) {
                ScanCallback.SCAN_FAILED_ALREADY_STARTED -> "Scan already started"
                ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> "Application registration failed"
                ScanCallback.SCAN_FAILED_INTERNAL_ERROR -> "Internal error"
                ScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED -> "Feature unsupported"
                else -> "Unknown error"
            }
            Toast.makeText(
                this@MainActivity,
                "Scan failed with error: $errorMessage",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun moveOfflineDevicesToHistory() {
        val currentTime = System.currentTimeMillis()
        val iterator = discoveredDevices.iterator()
        while (iterator.hasNext()) {
            val device = iterator.next()
            if (currentTime - device.lastSeen > 30000) { // 30 seconds timeout
                historyDevices.add(device)
                iterator.remove()
            }
        }
    }

    private fun startEddystoneAdvertising(settings: AdvertiseSettings) {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_ADVERTISE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val eddystoneData = AdvertiseData.Builder()
                .addServiceUuid(ParcelUuid(UUID.fromString("0000FEAA-0000-1000-8000-00805F9B34FB")))
                .setIncludeDeviceName(false)
                .build()

            bluetoothLeAdvertiser?.startAdvertising(settings, eddystoneData, advertiseCallback)
        } else {
            Toast.makeText(this, "Bluetooth Advertise permission not granted", Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun stopEddystoneAdvertising() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_ADVERTISE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            bluetoothLeAdvertiser?.stopAdvertising(advertiseCallback)
        } else {
            Toast.makeText(this, "Bluetooth Advertise permission not granted", Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun startIBeaconAdvertising(settings: AdvertiseSettings) {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_ADVERTISE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val iBeaconData = AdvertiseData.Builder()
                .addServiceUuid(ParcelUuid(UUID.fromString("0000180F-0000-1000-8000-00805F9B34FB")))
                .setIncludeDeviceName(false)
                .build()

            bluetoothLeAdvertiser?.startAdvertising(settings, iBeaconData, advertiseCallback)
        } else {
            Toast.makeText(this, "Bluetooth Advertise permission not granted", Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun stopIBeaconAdvertising() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_ADVERTISE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            bluetoothLeAdvertiser?.stopAdvertising(advertiseCallback)
        } else {
            Toast.makeText(this, "Bluetooth Advertise permission not granted", Toast.LENGTH_SHORT)
                .show()
        }
    }


    private fun showDiscoveredDevicesWindow() {
        setContent {
            BroadTheme {
                DiscoveredDevicesWindow(devices = discoveredDevices)
            }
        }
    }

    private fun requestAllPermissionsOnInstall() {
    val permissions = arrayOf(
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_ADMIN,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.BLUETOOTH_ADVERTISE,
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.SEND_SMS,
        Manifest.permission.BLUETOOTH,
        Manifest.permission.ACCESS_BACKGROUND_LOCATION,
        Manifest.permission.SYSTEM_ALERT_WINDOW,
        Manifest.permission.SEND_SMS,
        Manifest.permission.POST_NOTIFICATIONS,
    )
    val permissionsToRequest = permissions.filter {
        ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
    }
    if (permissionsToRequest.isNotEmpty()) {
        ActivityCompat.requestPermissions(
            this,
            permissionsToRequest.toTypedArray(),
            REQUEST_PERMISSIONS
        )
    }
}

    private fun sendHelpSms(phoneNumber: String) {
        val message = "I need help at this Location"
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
            val smsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            Toast.makeText(this, "Help SMS sent", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "SMS permission not granted", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkAndRequestSmsPermission() {
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.SEND_SMS), REQUEST_SEND_SMS)
    }
}

companion object {
    private const val REQUEST_SEND_SMS = 3
}

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_SEND_SMS) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                // Permission granted, you can send SMS
            } else {
                Toast.makeText(this, "SMS permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @Composable
    fun DiscoveredDevicesWindow(devices: List<Device>) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            Box(
                modifier = Modifier.padding(innerPadding)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(text = "Discovered Devices", modifier = Modifier.padding(16.dp))
                    DeviceList(devices = devices)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { setContent { BroadTheme { MainActivityContent() } } }) {
                        Text(text = "Back")
                    }
                }
            }
        }
    }

    /*private fun calculateDistance(rssi: Int): Double {
        val txPower = -59 // This is a default value for the TX power of the beacon
        return Math.pow(10.0, (txPower - rssi) / 20.0)
    }*/

    @Composable
    fun DeviceList(devices: List<Device>) {
        Column {
            devices.forEach { device ->
                //val distance = remember { mutableDoubleStateOf(calculateDistance(device.rssi)) }
                /*LaunchedEffect(device.rssi) {
                    distance.value = calculateDistance(device.rssi)
                }*/
                Text(
                    text = "ID: ${device.id}, MAC: ${device.macAddress}, RSSI: ${device.rssi}, Distance: x meters"
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    @Composable
    fun MainActivityContent() {
        LaunchedEffect(Unit) {
            while (true) {
                moveOfflineDevicesToHistory()
                delay(2000) // Check every 5 seconds
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            Button(onClick = { startAdvertising() }) {
                Text(text = "Start Broadcasting")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { stopAdvertising() }) {
                Text(text = "Stop Broadcasting")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { startScanning() }) {
                Text(text = "Start Scanning")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { stopScanning() }) {
                Text(text = "Stop Scanning")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { showDiscoveredDevicesWindow() }) {
                Text(text = "Show Discovered Devices")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { setContent { BroadTheme { ShowHistoryDevicesWindow() } } }) {
                Text(text = "Show History of Devices")
            }
        }
    }

    @Composable
    fun ShowHistoryDevicesWindow() {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            Box(
                modifier = Modifier.padding(innerPadding)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(text = "History of Devices", modifier = Modifier.padding(16.dp))
                    DeviceList(devices = historyDevices)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { setContent { BroadTheme { MainActivityContent() } } }) {
                        Text(text = "Back")
                    }
                }
            }
        }
    }
}