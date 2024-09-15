package com.help.Abhayada

import android.app.job.JobParameters
import android.app.job.JobService
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult

class ScanJobService : JobService() {

    private var bluetoothLeScanner: BluetoothLeScanner? = null

    override fun onStartJob(params: JobParameters?): Boolean {
        startScanning()
        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        stopScanning()
        return true
    }

    private fun startScanning() {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
        val scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                super.onScanResult(callbackType, result)
                // Handle scan result
            }
        }
        bluetoothLeScanner?.startScan(scanCallback)
    }

    private fun stopScanning() {
        bluetoothLeScanner?.stopScan(object : ScanCallback() {})
    }
}