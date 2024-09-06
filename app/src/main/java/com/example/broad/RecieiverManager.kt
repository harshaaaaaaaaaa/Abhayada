package com.example.broad

import androidx.compose.runtime.mutableStateListOf

object ReceiverManager {
    val broadcastingDevices = mutableStateListOf<Device>()

    fun addDevice(device: Device) {
        if (!broadcastingDevices.any { it.macAddress == device.macAddress }) {
            broadcastingDevices.add(device)
        }
    }

    fun removeDevice(device: Device) {
        broadcastingDevices.removeAll { it.macAddress == device.macAddress }
    }
}