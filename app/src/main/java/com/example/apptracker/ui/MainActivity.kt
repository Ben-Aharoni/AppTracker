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
    private val adapter = AppUsageAdapter()

    private val handler = Handler(Looper.getMainLooper())
    private val refreshRunnable = object : Runnable {
        override fun run() {
            val updated = AppUsageStorage.loadEntries(this@MainActivity)
            adapter.submitList(updated)
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
            Toast.makeText(this, "🧹 Data Deleted", Toast.LENGTH_SHORT).show()
        }

        adapter.submitList(AppUsageStorage.loadEntries(this))
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
            android.os.Process.myUid(),
            packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }
}
