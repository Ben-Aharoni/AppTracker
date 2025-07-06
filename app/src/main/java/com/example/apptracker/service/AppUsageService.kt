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

                if (
                    currentApp != null &&
                    currentApp != lastPackage &&
                    currentApp != myPackageName &&
                    !isLauncher
                ) {
                    val endTime = System.currentTimeMillis()

                    if (lastPackage != null && lastPackage != myPackageName && !isLauncherApp(lastPackage!!)) {
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

    /**
     * מחזירה true אם האפליקציה היא ה־Launcher הראשי של המכשיר
     */
    private fun isLauncherApp(packageName: String): Boolean {
        val pm = packageManager
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
        }
        val resolveInfo = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return resolveInfo?.activityInfo?.packageName == packageName
    }

    override fun onDestroy() {
        job.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
