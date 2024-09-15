package com.help.Abhayada

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AreaAlertReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val serviceIntent = Intent(context, AreaAlertService::class.java)
        context.startForegroundService(serviceIntent)
    }
}