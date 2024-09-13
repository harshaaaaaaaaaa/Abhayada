package com.help.Abhayada

import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import android.widget.Toast
import androidx.core.content.ContextCompat
import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.BluetoothLeAdvertiser
import android.bluetooth.le.BluetoothLeScanner
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import java.util.UUID

object BluetoothAdvertiser {

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        BluetoothAdapter.getDefaultAdapter()
    }
    private var bluetoothLeAdvertiser: BluetoothLeAdvertiser? = null
    private var bluetoothLeScanner: BluetoothLeScanner? = null

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            // Handle success
        }

        override fun onStartFailure(errorCode: Int) {
            // Handle failure
        }
    }


    fun startEddystoneAdvertising(context: Context, settings: AdvertiseSettings) {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        val bluetoothLeAdvertiser = bluetoothAdapter.bluetoothLeAdvertiser
        val eddystoneData = AdvertiseData.Builder()
            .addServiceUuid(ParcelUuid(UUID.fromString("0000FEAA-0000-1000-8000-00805F9B34FB")))
            .setIncludeDeviceName(false)
            .build()

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED) {
            bluetoothLeAdvertiser.startAdvertising(settings, eddystoneData, object : AdvertiseCallback() {
                override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
                    runOnUiThread {
                        Toast.makeText(context, "Eddystone Advertising started", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onStartFailure(errorCode: Int) {
                    runOnUiThread {
                        Toast.makeText(
                            context,
                            "Eddystone Advertising failed: $errorCode",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            })
        } else {
            runOnUiThread {
                Toast.makeText(
                    context,
                    "Bluetooth Advertise permission not granted",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    fun startIBeaconAdvertising(context: Context, settings: AdvertiseSettings) {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        val bluetoothLeAdvertiser = bluetoothAdapter.bluetoothLeAdvertiser
        val iBeaconData = AdvertiseData.Builder()
            .addServiceUuid(ParcelUuid(UUID.fromString("0000180F-0000-1000-8000-00805F9B34FB")))
            .setIncludeDeviceName(false)
            .build()

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED) {
            bluetoothLeAdvertiser.startAdvertising(settings, iBeaconData, object : AdvertiseCallback() {
                override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
                    runOnUiThread {
                        Toast.makeText(context, "iBeacon Advertising started", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onStartFailure(errorCode: Int) {
                    runOnUiThread {
                        Toast.makeText(
                            context,
                            "iBeacon Advertising failed: $errorCode",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            })
        } else {
            runOnUiThread {
                Toast.makeText(
                    context,
                    "Bluetooth Advertise permission not granted",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun stopIBeaconAdvertising(context: Context) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_ADVERTISE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            bluetoothLeAdvertiser?.stopAdvertising(advertiseCallback)
        } else {
            runOnUiThread {
                Toast.makeText(context, "Bluetooth Advertise permission not granted", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun stopEddystoneAdvertising(context: Context) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_ADVERTISE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            bluetoothLeAdvertiser?.stopAdvertising(advertiseCallback)
        } else {
            runOnUiThread {
                Toast.makeText(context, "Bluetooth Advertise permission not granted", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun runOnUiThread(action: () -> Unit) {
        Handler(Looper.getMainLooper()).post(action)
    }
}