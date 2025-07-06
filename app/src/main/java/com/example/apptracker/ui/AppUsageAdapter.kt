package com.example.apptracker.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.apptracker.data.AppUsageEntry
import com.example.apptracker.databinding.ItemAppUsageBinding
import java.text.SimpleDateFormat
import java.util.*

class AppUsageAdapter : RecyclerView.Adapter<AppUsageAdapter.UsageViewHolder>() {

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
            val format = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            val durationSec = (entry.endTime - entry.startTime) / 1000

            binding.txtAppName.text = "📱 ${entry.appName}"
            binding.txtStart.text = "🟢 Start: ${format.format(Date(entry.startTime))}"
            binding.txtEnd.text = "🔴 End: ${format.format(Date(entry.endTime))}"
            binding.txtDuration.text = "⏱ Duration: $durationSec sec"
        }
    }
}
