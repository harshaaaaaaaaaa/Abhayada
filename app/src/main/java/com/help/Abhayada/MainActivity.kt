package com.help.Abhayada

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
import android.content.Context
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import android.provider.Settings
import android.media.MediaPlayer
import android.telephony.SmsManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.help.Abhayada.hooks.useState
import com.help.Abhayada.ui.component.EmergencyContactsSection
import com.help.Abhayada.ui.component.PopUp
import kotlinx.coroutines.launch


class MainActivity : ComponentActivity() {


    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        BluetoothAdapter.getDefaultAdapter()
    }
    private var bluetoothLeAdvertiser: BluetoothLeAdvertiser? = null
    private var bluetoothLeScanner: BluetoothLeScanner? = null
    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying = false
    private var alertShown = false
    private lateinit var shakeDetector: ShakeDetector
    private val historyDevices = mutableStateListOf<Device>()
    private val discoveredDevices = mutableStateListOf<Device>()
    private val REQUEST_ENABLE_BT = 1
    private val REQUEST_PERMISSIONS = 2
    private lateinit var handler: Handler
    private val macAddresses = mutableListOf<String>()
    private lateinit var locationManager: LocationManager
    private val markedArea = MarkedArea(latitude = 26.4986183, longitude =  80.2857243, radius = 100f) // Example coordinates
    private var currentLatitude by mutableStateOf<Double?>(null)
    private var currentLongitude by mutableStateOf<Double?>(null)
    private lateinit var screenLockReceiver: ScreenLockReceiver
    private val PREFS_NAME = "AreaAlertPrefs"
    private val PREFS_KEY_ALERT_SHOWN = "isAlertShown"


    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handler = Handler(Looper.getMainLooper())

        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0L, 0f, locationListener)
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_PERMISSIONS)
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
            launchSOSTimer()
            }
        }
        shakeDetector.start()

        screenLockReceiver = ScreenLockReceiver {
            startAdvertising()
        }
        val filter = IntentFilter(Intent.ACTION_SCREEN_OFF)
        registerReceiver(screenLockReceiver, filter)

        startScanning()


        setContent {
            BroadTheme {
                HomePage()
            }
        }



    }

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            val userLatitude = location.latitude
            val userLongitude = location.longitude

            if (isUserInMarkedArea(userLatitude, userLongitude, markedArea)) {
                val intent = Intent(this@MainActivity, AreaAlertReceiver::class.java)
                sendBroadcast(intent)

                setContent{
                    BroadTheme {
                        HomePage(true)
                    }
                }
            } else {
                resetAlertShown()
            }
        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    private fun resetAlertShown() {
        val sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPreferences.edit().putBoolean(PREFS_KEY_ALERT_SHOWN, false).apply()
    }


    private fun playAlertSound() {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(
                this,
                R.raw.alert_sound
            ) // Place your audio file in res/raw folder
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


    private fun showEnableBluetoothDialog() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        ) {
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
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                REQUEST_PERMISSIONS
            )
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
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_ADVERTISE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
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

            // Get current location and send SMS
            getCurrentLocation { latitude, longitude ->
                sendHelpSms("+918005320074", latitude, longitude)
            }

            isAdvertising = true
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.BLUETOOTH_ADVERTISE),
                REQUEST_PERMISSIONS
            )
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
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_ADVERTISE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                bluetoothLeAdvertiser?.stopAdvertising(advertiseCallback)
                stopEddystoneAdvertising()
                stopIBeaconAdvertising()
                isAdvertising = false
                Toast.makeText(this, "Broadcasting stopped", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(
                    this,
                    "Bluetooth Advertise permission not granted",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun startScanning() {

        val bluetoothMacAddress = getBluetoothMacAddress()
        if (bluetoothMacAddress != null) {
            macAddresses.add("Bluetooth MAC Address: $bluetoothMacAddress")
        } else {
            Toast.makeText(this, "Failed to retrieve Bluetooth MAC Address", Toast.LENGTH_LONG).show()
        }

        val wifiMacAddress = getWifiMacAddress()
        if (wifiMacAddress != null) {
            macAddresses.add("Wi-Fi MAC Address: $wifiMacAddress")
        } else {
            Toast.makeText(this, "Failed to retrieve Wi-Fi MAC Address", Toast.LENGTH_LONG).show()
        }

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
        ) {
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
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.BLUETOOTH_SCAN),
                REQUEST_PERMISSIONS
            )
        }
    }


    private fun stopScanning() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
        ) {
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
            existingDevice.updateRssi(rssi)
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

            //marked location show notification
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

        private fun showSystemAlert(rssi: Int) {
            val sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val alertShown = sharedPreferences.getBoolean(PREFS_KEY_ALERT_SHOWN, false)

            if (!alertShown && Settings.canDrawOverlays(this@MainActivity)) {
                val alertDialog = AlertDialog.Builder(this@MainActivity)
                    .setTitle("!!!HELP!!!")
                    .setMessage("SOMEONE NEED HELP")
                    .setPositiveButton("Reach") { dialog, _ ->
                        dialog.dismiss()
                        sharedPreferences.edit().putBoolean(PREFS_KEY_ALERT_SHOWN, true).apply()
                        stopAlertSound()
                        setContent {
                            BroadTheme {
                                val initdistance: Double = 0.0
                                val (distance , setDistance) = useState(initdistance)
                                playAlertSound()
                                DiscoveredDevicesWindow(devices = discoveredDevices, setDistance)
                                Surface(modifier = Modifier.fillMaxSize()){
                                    Column(verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize()){
                                        Box(modifier = Modifier.size(300.dp).drawBehind { drawCircle(brush = Brush.radialGradient(colors = listOf(Color(0x33ff0000), Color(0x00ff0000)))) }, contentAlignment = Alignment.Center) {
                                            Text(String.format("%.1f", distance)+"m", maxLines = 1, textAlign = TextAlign.Center, fontSize = 96.sp, fontWeight = FontWeight.Medium, color = Color.Red, modifier = Modifier)
                                        }
                                        Text("HELP!!!", fontSize = 48.sp, fontWeight = FontWeight.Medium, color = Color.Red, modifier = Modifier.padding(top = 30.dp, bottom = 10.dp))
                                        Text("Hey, someone near you needs your help.", textAlign = TextAlign.Center, fontSize = 20.sp, fontWeight = FontWeight.Medium, color = Color(0x55000000), modifier = Modifier.fillMaxWidth(.8f))
                                        Button(onClick = {
                                            stopAlertSound()
                                            setContent{
                                                BroadTheme {
                                                    HomePage()
                                                }
                                            }
                                        }, modifier = Modifier.padding(top = 20.dp)) {
                                            Text("Dismiss")
                                        }
                                    }
                                }
                            }
                        }
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
            setContent {
                BroadTheme {
                    val initdistance: Double = 0.0
                    val (distance , setDistance) = useState(initdistance)
                    playAlertSound()
                    DiscoveredDevicesWindow(devices = discoveredDevices, setDistance)
                    Surface(modifier = Modifier.fillMaxSize()){
                        Column(verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize()){
                            Box(modifier = Modifier.size(300.dp).drawBehind { drawCircle(brush = Brush.radialGradient(colors = listOf(Color(0x33ff0000), Color(0x00ff0000)))) }, contentAlignment = Alignment.Center) {
                                Text(String.format("%.1f", distance)+"m", maxLines = 1, textAlign = TextAlign.Center, fontSize = 96.sp, fontWeight = FontWeight.Medium, color = Color.Red, modifier = Modifier)
                            }
                            Text("HELP!!!", fontSize = 48.sp, fontWeight = FontWeight.Medium, color = Color.Red, modifier = Modifier.padding(top = 30.dp, bottom = 10.dp))
                            Text("Hey, someone near you needs your help.", textAlign = TextAlign.Center, fontSize = 20.sp, fontWeight = FontWeight.Medium, color = Color(0x55000000), modifier = Modifier.fillMaxWidth(.8f))
                            Button(onClick = {
                                stopAlertSound()
                                setContent{
                                    BroadTheme {
                                        HomePage()
                                    }
                                }
                                             }, modifier = Modifier.padding(top = 20.dp)) {
                                Text("Dismiss")
                            }
                        }
                    }
                }
            }
        }
    }

    private fun moveOfflineDevicesToHistory() {
    val currentTime = System.currentTimeMillis()
    val iterator = discoveredDevices.iterator()
    while (iterator.hasNext()) {
        val device = iterator.next()
        if (currentTime - device.lastSeen > 1000) { // 3 seconds timeout
            historyDevices.add(device)
            iterator.remove()
        }
    }
    if (discoveredDevices.isEmpty()) {
        stopAlertSound() // Stop the alert sound if no devices are found
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

            }
        }
    }

    fun launchSOSTimer(){
        setContent {
            HomePage(issos = true)
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

    private fun getCurrentLocation(onLocationReceived: (Double, Double) -> Unit) {
    if (ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    ) {
        locationManager.requestSingleUpdate(
            LocationManager.GPS_PROVIDER,
            object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    onLocationReceived(location.latitude, location.longitude)
                }

                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            },
            null
        )
    } else {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            REQUEST_PERMISSIONS
        )
    }
}



    private fun sendHelpSms(phoneNumber: String, latitude: Double, longitude: Double) {
    val googleMapsLink = "https://www.google.com/maps/search/?api=1&query=$latitude,$longitude"
    val message = "I need help at this location: $googleMapsLink"
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
fun MainActivityContent() {
    var currentLatitude by remember { mutableStateOf<Double?>(null) }
    var currentLongitude by remember { mutableStateOf<Double?>(null) }

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
        Button(onClick = { setContent { BroadTheme { ShowHistoryDevicesWindow(currentLatitude, currentLongitude) } } }) {
            Text(text = "Show History of Devices")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { getCurrentLocation { latitude, longitude ->
            currentLatitude = latitude
            currentLongitude = longitude
        } }) {
            Text(text = "Show Current Location")
        }
    }
}

    @Composable
    fun DiscoveredDevicesWindow(devices: List<Device>, setDistance: (Double) -> Unit) {
        val deviceStates = remember { devices.map { it.id to mutableStateOf(it) }.toMap() }
        val allDevices = remember { mutableStateListOf<Device>() }

        LaunchedEffect(devices) {
            while (true) {
                devices.forEach { device ->
                    val existingDevice = allDevices.find { it.id == device.id }
                    if (existingDevice != null) {
                        existingDevice.updateRssi(device.rssi)
                    } else {
                        allDevices.add(0, device) // Add new devices on top
                    }
                }
                delay(1000) // Update every second
                DeviceList(devices = allDevices, setDistance)
            }
        }

    }

    @Composable
fun ShowHistoryDevicesWindow(currentLatitude: Double?, currentLongitude: Double?) {
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Box(
            modifier = Modifier.padding(innerPadding)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(text = "History of Devices", modifier = Modifier.padding(16.dp))
                macAddresses.forEach { macAddress ->
                    Text(text = macAddress)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                currentLatitude?.let {
                    Text(text = "Current Latitude: $it")
                    Spacer(modifier = Modifier.height(8.dp))
                }
                currentLongitude?.let {
                    Text(text = "Current Longitude: $it")
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { setContent { BroadTheme { MainActivityContent() } } }) {
                    Text(text = "Back")
                }
            }
        }
    }
}


    fun DeviceList(devices: List<Device>, setDistance: (Double) -> Unit) {
        val currentTime = System.currentTimeMillis()
        var distance: Double = 0.0

        devices.forEach() { device->
            distance = device.calculateDistance()
            val isActive = currentTime - device.lastSeen <= 2000 // 2 seconds timeout
            val textColor = if (isActive) Color.Black else Color.Gray
            val backgroundColor = if (isActive) Color.Yellow else Color.Transparent
        }

        setDistance(distance)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun HomePage(openalert: Boolean = false, issos: Boolean = false){
        val (openAlert, setOpenAlert) = useState(openalert)
        val (isExpanded, setIsExpanded) = useState(false)
        val (isSOS, setIsSOS) = useState(issos)
        val (helpAlert, setHelpAlert) = useState(false)



        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val scope = rememberCoroutineScope()
        var user_name = "Harshita Kumari"
        var user_phoneNumber = "+91-9149358878"



        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(modifier = Modifier.fillMaxWidth(.8f)) {
                    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 50.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text(user_name, fontSize = 25.sp, fontWeight = FontWeight.SemiBold)
                                Text(user_phoneNumber, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0x88000000))
                            }
                            Spacer(modifier = Modifier.fillMaxWidth(.45f))
                            Image(painter = painterResource(R.drawable.profile), contentDescription = "Profile Image", modifier = Modifier.size(50.dp).fillMaxWidth())
                        }
                        HorizontalDivider(modifier = Modifier.align(Alignment.CenterHorizontally).fillMaxWidth(.2f).padding(vertical = 20.dp), thickness = 3.dp)
                        NavigationDrawerItem(label = {
                            Text("Home", fontSize = 17.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 10.dp))
                        },
                            onClick = {},
                            selected = true)
                        NavigationDrawerItem(label = {
                            Text("About", fontSize = 17.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 10.dp))
                        },
                            onClick = {},
                            selected = false)
                        NavigationDrawerItem(label = {
                            Text("Profile", fontSize = 17.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 10.dp))
                        },
                            onClick = {},
                            selected = false)
                        NavigationDrawerItem(label = {
                            Text("Aadhar Verification", fontSize = 17.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 10.dp))
                        },
                            onClick = { setContent{
                                BroadTheme {
                                    LoginPage()
                                }
                            }},
                            selected = false)
                        NavigationDrawerItem(label = {
                            Text("Hotspots Near You", fontSize = 17.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 10.dp))
                        },
                            onClick = {},
                            selected = false)
                        NavigationDrawerItem(label = {
                            Column {
                                Text(
                                    "Featured",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.Red,
                                    modifier = Modifier.padding(start = 10.dp)
                                )
                            }
                        },
                            onClick = { setIsExpanded(!isExpanded) },
                            selected = false)
                        AnimatedVisibility(visible = isExpanded) {
                            Column(modifier = Modifier.padding(start = 40.dp)) {
                                TextButton(onClick = {}) {
                                    Text("Self Defence", fontWeight = FontWeight.Medium, modifier = Modifier)
                                }
                                TextButton(onClick = {}) {
                                    Text(
                                        "Youtube Training Sessions",
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                TextButton(onClick = {}) {
                                    Text("Things to Buy", fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }
            }
        ) {
            Scaffold(
                modifier = Modifier,
                topBar = {
                    TopAppBar(title = {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.absoluteOffset(x = (-20).dp)
                            ) {
                                Button(
                                    onClick = { scope.launch { drawerState.open() } },
                                    shape = CircleShape,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                                ) {
                                    Icon(
                                        imageVector = ImageVector.vectorResource(R.drawable.menu),
                                        contentDescription = "Menu button icon",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    Text(
                                        "I am ",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.Red
                                    )
                                    Text(
                                        text = "Abhayada",
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            HorizontalDivider(thickness = (1.5).dp, modifier = Modifier.fillMaxWidth().offset(x = (-10).dp).padding(top = 2.dp))
                        }
                    })
                }
            ) { innerPadding ->
                Column(modifier = Modifier.padding(innerPadding).fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                    when{
                        !helpAlert ->
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Spacer(modifier = Modifier.height(30.dp))
                                Button(
                                    onClick = {
                                        setIsSOS(true)
                                    },
                                    shape = CircleShape,
                                    modifier = Modifier.height(200.dp).width(200.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xffFFE0E0)
                                    )
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "SEND",
                                            fontSize = 15.sp,
                                            color = Color.Red,
                                            modifier = Modifier.offset(y = 5.dp)
                                        )
                                        Text(
                                            text = "SOS",
                                            fontSize = 64.sp,
                                            color = Color.Red,
                                            fontWeight = FontWeight.Normal
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(30.dp))
                                OutlinedButton(
                                    onClick = { setOpenAlert(true) },
                                    contentPadding = PaddingValues(
                                        horizontal = 20.dp,
                                        vertical = 15.dp
                                    ),
                                    modifier = Modifier.fillMaxWidth(.9f)
                                ) {
                                    Text(
                                        text = "Your current location.",
                                        fontSize = 20.sp,
                                        modifier = Modifier
                                    )
                                    Spacer(modifier = Modifier.fillMaxWidth(.7f))
                                    Icon(
                                        imageVector = ImageVector.vectorResource(R.drawable.location),
                                        contentDescription = "Location Icon"
                                    )
                                }
                                HorizontalDivider(
                                    modifier = Modifier.fillMaxWidth(.2f).padding(vertical = 20.dp),
                                    thickness = 3.dp
                                )
                                EmergencyContactsSection()
                                PopUp(openAlert, setOpenAlert)
                                when {
                                    isSOS ->
                                        AlertDialog(
                                            modifier = Modifier.fillMaxWidth().fillMaxHeight(.9f),
                                            containerColor = Color.White,
                                            title = {
                                                Column(modifier = Modifier.padding(vertical = 30.dp)){
                                                    Text("SOS trigger detected", fontSize = 28.sp, fontWeight = FontWeight.SemiBold, color = Color(0x99000000))
                                                    Text("Calling 181 and sharing location with emergency contacts in...", lineHeight = 25.sp, modifier = Modifier.padding(top = 10.dp), fontSize = 18.sp, fontWeight = FontWeight.Medium, color = Color(0x55000000))
                                                }
                                            },
                                            text = {
                                                Column(verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize()) {
                                                    val (timer, setTimer) = useState(30f)
                                                    val (timeOut, setTimeOut) = useState(false)

                                                    val animateTimer: Float by animateFloatAsState(timer, label = "Timer Animation")

                                                    LaunchedEffect(timer) {
                                                        delay(1000)
                                                        if(timer > 0f && !timeOut) {
                                                            setTimer(timer - 1)
                                                        }
                                                        if(timer == 0f) {
                                                            setTimeOut(true)
                                                        }
                                                    }

                                                    Spacer(modifier = Modifier.fillMaxHeight(.1f))
                                                    Box(modifier = Modifier.drawBehind {
                                                            drawCircle(color = Color(0x22000000))
                                                        }
                                                        .height(200.dp)
                                                        .width(200.dp)
                                                    ){
                                                        Box(modifier = Modifier.drawBehind {
                                                                drawArc(color = Color(0x88000000), startAngle = -90f, sweepAngle = (animateTimer/30)*360f, true)
                                                            }
                                                            .height(200.dp)
                                                            .width(200.dp),
                                                            contentAlignment = Alignment.Center
                                                        ){
                                                            Box(modifier = Modifier.drawBehind {
                                                                    drawCircle(color = Color.White)
                                                                }
                                                                .height(190.dp)
                                                                .width(190.dp),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                if(timeOut){
                                                                    startAdvertising()
                                                                    Column(modifier = Modifier.fillMaxWidth()){
                                                                        Text("SOS send!!!", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontSize = 24.sp, color = Color(0xaa000000))
                                                                        Text("Help is on the way.", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontSize = 16.sp, color = Color(0x55000000))
                                                                    }
                                                                }
                                                                else{
                                                                    Text(timer.toInt().toString(), fontSize = 70.sp, color = Color(0xaa000000))
                                                                }
                                                            }
                                                        }
                                                    }

                                                    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Bottom, horizontalAlignment = Alignment.CenterHorizontally){
                                                        Button(onClick = {
                                                            stopAdvertising()
                                                            startScanning()
                                                            setIsSOS(false)
                                                        },
                                                            modifier = Modifier.fillMaxWidth(),
                                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xff2ED436), contentColor = Color(0xff075400))) {
                                                            Text(" I'm OK ", fontSize = 20.sp,  modifier = Modifier.padding(8.dp))
                                                        }
                                                        Spacer(modifier = Modifier.height(10.dp))
                                                        Button(onClick = {
                                                            setTimeOut(true)
                                                        },
                                                            modifier = Modifier.fillMaxWidth(),
                                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xddFF3737), contentColor = Color(0xff640000))) {
                                                            Text("Send SOS", fontSize = 20.sp,  modifier = Modifier.padding(8.dp))
                                                        }
                                                    }
                                                }
                                            },
                                            onDismissRequest = {
                                                stopAdvertising()
                                                startScanning()
                                                setIsSOS(false)
                                            },
                                            confirmButton = {}
                                        )
                                }

                            }
                        helpAlert -> {}
                    }
                }
            }
        }
    }

    @Composable
    fun LoginPage(){
        var aadharNumber by remember { mutableStateOf("") }

        Surface(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 40.dp)) {
            Column(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.padding(top = 40.dp)) {
                    Text(
                        text = "Aadhar",
                        fontSize = 40.sp,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Verification",
                        fontSize = 40.sp,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold
                    )
                }
                TextField(
                    modifier = Modifier.fillMaxWidth().padding(top = 20.dp).border(width = 1.dp, color = Color(0x22000000), shape = RoundedCornerShape(size = 15.dp)).height(59.dp),
                    value = aadharNumber,
                    onValueChange = {aadharNumber = it},
                    placeholder = { Text(text = "XXXX - XXXX - XXXX - XXXX", fontSize = 20.sp, fontWeight = FontWeight.Medium, modifier = Modifier.alpha(0.25f)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    ),
                    textStyle = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Medium, color = Color(0xaa000000)),
                    shape = RoundedCornerShape(size = 15.dp)
                )
                Row(modifier = Modifier.padding(top = 10.dp, start = 5.dp)) {
                    RadioButton(selected = false, onClick = null, modifier = Modifier)
                    Spacer(modifier = Modifier.size(5.dp))
                    Text(text = "Yes, I agree to ", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xaa000000))
                    Text(text = "Terms of Service.", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.Blue)
                }
                Box(modifier = Modifier.fillMaxSize().padding(bottom = 30.dp), contentAlignment = Alignment.BottomCenter) {
                    Button(onClick = {
                        setContent {
                            BroadTheme {
                                HomePage()
                            }
                        }
                    }, modifier = Modifier.height(55.dp), shape = RoundedCornerShape(size = 15.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xffd9d9d9))) {
                        Text(
                            text = "Send OTP",
                            textAlign = TextAlign.Center,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xaa000000),
                            modifier = Modifier.fillMaxWidth(0.9f)
                        )
                    }
                }
            }
        }
    }
}