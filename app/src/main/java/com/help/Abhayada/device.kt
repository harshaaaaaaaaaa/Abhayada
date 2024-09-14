package com.help.Abhayada

import kotlin.math.pow

data class Device(
    val id: String,
    val macAddress: String,
    var rssi: Int,
    var lastSeen: Long = System.currentTimeMillis()
) {
    fun calculateDistance(): Double {
        val txPower = -59 // This is a default value for the TX power of the beacon
        if (rssi == 0) {
            return -1.0 // if we cannot determine distance, return -1.
        }

        val ratio = rssi * 1.0 / txPower
        return if (ratio < 1.0) {
            ratio.pow(10.0)
        } else {
            (0.89976 * ratio.pow(7.7095) + 0.111)
        }
    }

    fun updateRssi(newRssi: Int) {
        rssi = newRssi
        lastSeen = System.currentTimeMillis()
    }
}