package com.example.apptracker.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.app.usage.UsageStatsManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.apptracker.data.AppUsageEntry
import com.example.apptracker.util.AppUsageStorage
import kotlinx.coroutines.*

class AppUsageService : Service() {

    private lateinit var usageStatsManager: UsageStatsManager
    private var lastPackage: String? = null
    private var startTime: Long = 0L
    private val job = CoroutineScope(Dispatchers.IO + Job())

    override fun onCreate() {
        super.onCreate()
        usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        startForeground(1, buildNotification())
        startTracking()
    }

    private fun buildNotification(): Notification {
        val channelId = "usage_tracking_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "App Usage Tracking",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("App Usage Tracking")
            .setContentText("Tracking foreground apps...")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .build()
    }

    private fun startTracking() {
        job.launch {
            while (true) {
                val currentTime = System.currentTimeMillis()
                val usageStats = usageStatsManager.queryUsageStats(
                    UsageStatsManager.INTERVAL_DAILY,
                    currentTime - 10000,
                    currentTime
                )
                val currentApp = usageStats
                    .maxByOrNull { it.lastTimeUsed }
                    ?.packageName

                if (currentApp != null && currentApp != lastPackage) {
                    val endTime = System.currentTimeMillis()
                    if (lastPackage != null) {
                        val entry = AppUsageEntry(
                            packageName = lastPackage!!,
                            appName = lastPackage!!,
                            startTime = startTime,
                            endTime = endTime
                        )
                        AppUsageStorage.saveEntry(this@AppUsageService, entry)
                        Log.d("AppUsageService", "Saved: $entry")
                    }

                    lastPackage = currentApp
                    startTime = endTime
                }

                delay(5000)
            }
        }
    }

    override fun onDestroy() {
        job.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
