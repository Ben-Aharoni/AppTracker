package com.example.apptracker.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.app.usage.UsageStatsManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.apptracker.data.AppUsageEntry
import com.example.apptracker.util.AppUsageStorage
import kotlinx.coroutines.*
import android.content.pm.PackageManager

class AppUsageService : Service() {

    private lateinit var usageStatsManager: UsageStatsManager
    private var activeEntry: AppUsageEntry? = null
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
            val myPackageName = packageName

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

                val isLauncher = currentApp?.let { isLauncherApp(it) } ?: false
                val shouldTrack = currentApp != null &&
                        currentApp != myPackageName &&
                        !isLauncher

                if (shouldTrack) {
                    if (activeEntry == null) {
                        activeEntry = AppUsageEntry(
                            packageName = currentApp!!,
                            appName = currentApp,
                            startTime = currentTime,
                            endTime = currentTime
                        )
                    } else if (currentApp == activeEntry!!.packageName) {
                        activeEntry!!.endTime = currentTime
                    } else {
                        AppUsageStorage.saveEntry(this@AppUsageService, activeEntry!!)
                        Log.d("AppUsageService", "Saved: $activeEntry")

                        activeEntry = AppUsageEntry(
                            packageName = currentApp!!,
                            appName = currentApp,
                            startTime = currentTime,
                            endTime = currentTime
                        )
                    }
                }

                delay(2000)
            }
        }
    }

    private fun isLauncherApp(packageName: String): Boolean {
        val pm = packageManager
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
        }
        val resolveInfo = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return resolveInfo?.activityInfo?.packageName == packageName
    }

    override fun onDestroy() {
        activeEntry?.let {
            AppUsageStorage.saveEntry(this, it)
            Log.d("AppUsageService", "Saved onDestroy: $it")
        }
        job.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
