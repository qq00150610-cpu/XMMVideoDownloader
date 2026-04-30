package com.xmm.videodownloader

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.xmm.videodownloader.databinding.FragmentDownloadsBinding

class DownloadsFragment : Fragment() {

    private var _binding: FragmentDownloadsBinding? = null
    private val binding get() = _binding!!
    private var downloadManager: DownloadManager? = null
    private var adapter: VideoAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDownloadsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        downloadManager = DownloadManager(requireContext())

        adapter = VideoAdapter(
            onPlayClick = { item ->
                val intent = Intent(requireContext(), PlayerActivity::class.java)
                intent.putExtra("video_urls", arrayOf(item.localPath))
                intent.putExtra("video_titles", arrayOf(item.title))
                startActivity(intent)
            },
            onDeleteClick = { item ->
                val file = java.io.File(item.localPath)
                downloadManager?.deleteVideo(file)
                loadVideos()
            },
            isLocalMode = true
        )

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        loadVideos()
    }

    private fun loadVideos() {
        val videos = downloadManager?.getDownloadedVideos() ?: emptyList()
        adapter?.submitList(videos)
        binding.tvEmpty.visibility = if (videos.isEmpty()) View.VISIBLE else View.GONE
        binding.recyclerView.visibility = if (videos.isEmpty()) View.GONE else View.VISIBLE
    }

    override fun onResume() {
        super.onResume()
        loadVideos()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
