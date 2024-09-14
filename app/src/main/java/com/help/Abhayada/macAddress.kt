package com.help.Abhayada

import java.io.BufferedReader
import java.io.InputStreamReader

fun getBluetoothMacAddress(): String? {
    return try {
        val process = Runtime.getRuntime().exec("getprop persist.odm.bt.address")
        val reader = BufferedReader(InputStreamReader(process.inputStream))
        val macAddress = reader.readLine()
        reader.close()
        macAddress
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun getWifiMacAddress(): String? {
    return try {
        val process = Runtime.getRuntime().exec("getprop persist.odm.wifimac")
        val reader = BufferedReader(InputStreamReader(process.inputStream))
        val macAddress = reader.readLine()
        reader.close()
        macAddress
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}