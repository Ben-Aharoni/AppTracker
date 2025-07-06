package com.example.apptracker.data

data class AppUsageEntry(
    val packageName: String,
    val appName: String,
    val startTime: Long,
    var endTime: Long
)
