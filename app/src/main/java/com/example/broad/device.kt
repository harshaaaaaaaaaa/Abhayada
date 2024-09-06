package com.example.broad

data class Device(
    val id: String,
    val macAddress: String,
    var rssi: Int,
    var lastSeen: Long = System.currentTimeMillis()
)