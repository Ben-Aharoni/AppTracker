package com.example.apptracker.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.apptracker.data.AppUsageEntry
import com.example.apptracker.databinding.ItemAppUsageBinding
import java.text.SimpleDateFormat
import java.util.*

class AppUsageAdapter(private val context: Context) :
    RecyclerView.Adapter<AppUsageAdapter.UsageViewHolder>() {

    private val data = mutableListOf<AppUsageEntry>()

    fun submitList(list: List<AppUsageEntry>) {
        data.clear()
        data.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UsageViewHolder {
        val binding = ItemAppUsageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UsageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UsageViewHolder, position: Int) {
        holder.bind(data[position])
    }

    override fun getItemCount(): Int = data.size

    inner class UsageViewHolder(private val binding: ItemAppUsageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(entry: AppUsageEntry) {
            val formatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            val duration = formatDuration(entry.endTime - entry.startTime)

            // Bind UI: show app name with emoji
            binding.txtAppName.text = "📱 ${entry.appName}"
            binding.txtStart.text = "Start: ${formatter.format(Date(entry.startTime))}"
            binding.txtEnd.text = "End: ${formatter.format(Date(entry.endTime))}"
            binding.txtDuration.text = "Duration: $duration"
            binding.txtNetworkType.text = "📶 Network: ${entry.networkType}"
            binding.txtWifiData.text = "Wi-Fi: ${formatData(entry.wifiBytes)}"
            binding.txtMobileData.text = "Mobile: ${formatData(entry.mobileBytes)}"


        }

        private fun formatDuration(durationMillis: Long): String {
            val totalSeconds = durationMillis / 1000
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60

            return if (hours > 0)
                String.format("%02d:%02d:%02d", hours, minutes, seconds)
            else
                String.format("%02d:%02d", minutes, seconds)
        }

        private fun formatData(bytes: Long): String {
            val kb = bytes / 1024
            val mb = kb / 1024
            return if (mb > 0) "$mb MB" else "$kb KB"
        }

    }
}
