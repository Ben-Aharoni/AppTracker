package com.example.apptracker.data

data class AppUsageEntry(
    val packageName: String,
    val appName: String,
    val startTime: Long,
    var endTime: Long,
    val networkType: String = "Unknown",
    var mobileBytes: Long = 0L,
    var wifiBytes: Long = 0L
)

