package com.help.Abhayada

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ScreenLockReceiver(private val onScreenLocked: () -> Unit) : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_SCREEN_OFF) {
            onScreenLocked()
        }
    }
}