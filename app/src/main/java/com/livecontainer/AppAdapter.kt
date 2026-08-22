package com.livecontainer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView

class AppAdapter(private var appList: List<AppAdapter.AppItem>) : RecyclerView.Adapter<AppAdapter.AppViewHolder>() {

    class AppViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.appIcon)
        val name: TextView = view.findViewById(R.id.nameTextView)
        val version: TextView = view.findViewById(R.id.versionTextView)
        val bundle: TextView = view.findViewById(R.id.bundleTextView)
        val startBtn: Button = view.findViewById(R.id.startButton)
    }

    fun submitList(newList: List<AppItem>) {
        val diff = DiffCallback(appList, newList)
        submitList(diff)
    }

    inner class DiffCallback(
        private val oldList: List<AppItem>,
        private val newList: List<AppItem>
    ) : DiffUtil.ItemCallback<AppItem>() {
        override fun areItemsTheSame(oldItem: AppItem, newItem: AppItem): Boolean =
            oldItem.packageName == newItem.packageName

        override fun areContentsTheSame(oldItem: AppItem, newItem: AppItem): Boolean =
            oldItem == newItem
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.app_card, parent, false)
        return AppViewHolder(view)
    }

    override fun getItemCount(): Int = appList.size

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val item = appList[position]

        // Set icon - display Bitmap directly
        if (item.icon != null) {
            holder.icon.setImageBitmap(item.icon)
        } else {
            holder.icon.setImageResource(R.drawable.ic_launcher)
        }
        
        holder.name.text = item.name
        holder.version.text = item.version
        holder.bundle.text = item.bundle

        holder.startBtn.setOnClickListener {
            // Handle start button click
        }
    }

    data class AppItem(
        val icon: android.graphics.Bitmap?,   // Changed from iconRes: Int
        val name: String,
        val version: String,
        val bundle: String,
        val packageName: String
    )