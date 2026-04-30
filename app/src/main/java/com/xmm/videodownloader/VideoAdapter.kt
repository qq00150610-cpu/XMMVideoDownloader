package com.xmm.videodownloader

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.xmm.videodownloader.databinding.ItemVideoBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VideoAdapter(
    private val onPlayClick: (VideoItem) -> Unit,
    private val onDeleteClick: ((VideoItem) -> Unit)?,
    private val isLocalMode: Boolean = false
) : ListAdapter<VideoItem, VideoAdapter.VideoViewHolder>(VideoDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val binding = ItemVideoBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return VideoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class VideoViewHolder(
        private val binding: ItemVideoBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: VideoItem) {
            binding.tvTitle.text = item.title

            val sizeText = if (item.fileSize > 0) {
                formatFileSize(item.fileSize)
            } else ""
            val dateText = if (item.downloadDate > 0) {
                SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                    .format(Date(item.downloadDate))
            } else ""
            binding.tvInfo.text = listOfNotNull(sizeText.takeIf { it.isNotEmpty() }, dateText.takeIf { it.isNotEmpty() })
                .joinToString(" · ")

            if (isLocalMode && item.localPath.isNotEmpty()) {
                try {
                    binding.ivThumbnail.setImageURI(null)
                    val uri = Uri.fromFile(File(item.localPath))
                    binding.ivThumbnail.setImageURI(uri)
                } catch (e: Exception) {
                    binding.ivThumbnail.setImageResource(android.R.drawable.ic_media_play)
                }
            } else {
                binding.ivThumbnail.setImageResource(android.R.drawable.ic_media_play)
            }

            binding.btnPlay.setOnClickListener { onPlayClick(item) }

            if (onDeleteClick != null) {
                binding.btnDelete.visibility = View.VISIBLE
                binding.btnDelete.setOnClickListener { onDeleteClick.invoke(item) }
            } else {
                binding.btnDelete.visibility = View.GONE
            }
        }
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
            else -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
        }
    }

    class VideoDiffCallback : DiffUtil.ItemCallback<VideoItem>() {
        override fun areItemsTheSame(oldItem: VideoItem, newItem: VideoItem): Boolean {
            return oldItem.localPath == newItem.localPath && oldItem.videoUrl == newItem.videoUrl
        }

        override fun areContentsTheSame(oldItem: VideoItem, newItem: VideoItem): Boolean {
            return oldItem == newItem
        }
    }
}
