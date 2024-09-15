package com.help.Abhayada

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent

class ScreenLockReceiver(private val onScreenLocked: () -> Unit) : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_SCREEN_OFF) {
            onScreenLocked()
            context?.startService(Intent(context, ShakeService::class.java))
        }
    }

    private fun scheduleScanJob(context: Context?) {
        val componentName = ComponentName(context!!, ScanJobService::class.java)
        val jobInfo = JobInfo.Builder(1, componentName)
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            .setRequiresCharging(false)
            .setPersisted(true)
            .setPeriodic(15 * 60 * 1000) // 15 minutes interval
            .build()

        val jobScheduler = context?.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        jobScheduler.schedule(jobInfo)
    }
}