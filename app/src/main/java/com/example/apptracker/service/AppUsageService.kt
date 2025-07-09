package com.example.apptracker.service

import android.app.*
import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
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
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

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

                val currentApp = usageStats.maxByOrNull { it.lastTimeUsed }?.packageName

                val isLauncher = currentApp?.let { isLauncherApp(it) } ?: false
                val shouldTrack = currentApp != null &&
                        currentApp != myPackageName &&
                        !isLauncher

                if (shouldTrack) {
                    val safeApp = currentApp ?: "unknown.app"

                    // Try get UID safely
                    val uid = try {
                        packageManager.getApplicationInfo(safeApp, 0).uid
                    } catch (e: Exception) {
                        Log.w("AppUsageService", "Failed to get UID for $safeApp: ${e.message}")
                        -1
                    }

                    val mobileBytes = if (uid != -1)
                        getDataUsage(uid, ConnectivityManager.TYPE_MOBILE) else -1L
                    val wifiBytes = if (uid != -1)
                        getDataUsage(uid, ConnectivityManager.TYPE_WIFI) else -1L

                    if (activeEntry == null) {
                        // Start new session
                        activeEntry = AppUsageEntry(
                            packageName = safeApp,
                            appName = safeApp,
                            startTime = currentTime,
                            endTime = currentTime,
                            networkType = getNetworkType(),
                            wifiBytes = wifiBytes,
                            mobileBytes = mobileBytes
                        )
                    } else if (safeApp == activeEntry!!.packageName) {
                        // Still same app
                        activeEntry!!.endTime = currentTime
                    } else {
                        // App switched → save and start new
                        AppUsageStorage.saveEntry(this@AppUsageService, activeEntry!!)
                        Log.d("AppUsageService", "Saved entry: $activeEntry")

                        activeEntry = AppUsageEntry(
                            packageName = safeApp,
                            appName = safeApp,
                            startTime = currentTime,
                            endTime = currentTime,
                            networkType = getNetworkType(),
                            wifiBytes = wifiBytes,
                            mobileBytes = mobileBytes
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

    private fun getNetworkType(): String {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return "Offline"
        val capabilities = cm.getNetworkCapabilities(network) ?: return "Offline"

        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile Data"
            else -> "Other"
        }
    }

    private fun getDataUsage(uid: Int, networkType: Int): Long {
        return try {
            val statsManager = getSystemService(Context.NETWORK_STATS_SERVICE) as NetworkStatsManager
            val now = System.currentTimeMillis()
            val oneHourAgo = now - (1000 * 60 * 60) // לדוגמה: שעה אחרונה

            val stats = statsManager.queryDetailsForUid(
                networkType,
                null,
                oneHourAgo,
                now,
                uid
            )

            var totalBytes = 0L
            val bucket = NetworkStats.Bucket()

            while (stats.hasNextBucket()) {
                stats.getNextBucket(bucket)
                totalBytes += bucket.rxBytes + bucket.txBytes
            }

            stats.close()
            totalBytes
        } catch (e: Exception) {
            e.printStackTrace()
            -1
        }
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
