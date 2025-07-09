package com.example.apptracker.ui

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.*
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.apptracker.databinding.ActivityMainBinding
import com.example.apptracker.service.AppUsageService
import com.example.apptracker.util.AppUsageStorage

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val adapter = AppUsageAdapter(this)

    private val handler = Handler(Looper.getMainLooper())
    private val refreshRunnable = object : Runnable {
        override fun run() {
            loadData()
            handler.postDelayed(this, 2000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        val serviceIntent = Intent(this, AppUsageService::class.java)

        binding.btnStartService.setOnClickListener {
            if (!hasUsageAccessPermission()) {
                startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            } else {
                ContextCompat.startForegroundService(this, serviceIntent)
                Toast.makeText(this, "📡 Tracking Started", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnStopService.setOnClickListener {
            stopService(serviceIntent)
            Toast.makeText(this, "🛑 Tracking Stopped", Toast.LENGTH_SHORT).show()
        }

        binding.btnClearData.setOnClickListener {
            AppUsageStorage.clearEntries(this)
            adapter.submitList(emptyList())
            binding.barChart.setData(emptyList())
            Toast.makeText(this, "🧹 Data Deleted", Toast.LENGTH_SHORT).show()
        }

        loadData()
    }

    override fun onResume() {
        super.onResume()
        handler.post(refreshRunnable)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(refreshRunnable)
    }

    private fun hasUsageAccessPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun loadData() {
        val entries = AppUsageStorage.loadEntries(this)
        adapter.submitList(entries)

        val durations = entries.groupBy { it.appName }
            .mapValues { list -> list.value.sumOf { it.endTime - it.startTime } }
            .toList()
            .sortedByDescending { it.second }

        binding.barChart.setData(durations)
    }
}
