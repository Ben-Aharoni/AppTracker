

package com.example.apptracker.util

import android.content.Context
import com.example.apptracker.data.AppUsageEntry
import com.google.gson.reflect.TypeToken
import com.google.gson.Gson
import java.io.File

object AppUsageStorage {
    private const val FILE_NAME = "usage_data.json"

    fun saveEntry(context: Context, entry: AppUsageEntry) {
        val entries = loadEntries(context).toMutableList()
        entries.add(0, entry)
        val json = Gson().toJson(entries)
        File(context.filesDir, FILE_NAME).writeText(json)
    }

    fun loadEntries(context: Context): List<AppUsageEntry> {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists()) return emptyList()
        val json = file.readText()
        val type = object : TypeToken<List<AppUsageEntry>>() {}.type
        return Gson().fromJson(json, type) ?: emptyList()
    }

    fun clearEntries(context: Context) {
        val file = File(context.filesDir, FILE_NAME)
        if (file.exists()) {
            file.delete()
        }
    }

}
