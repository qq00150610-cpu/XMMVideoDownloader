package com.xmm.videodownloader

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.xmm.videodownloader.databinding.FragmentParseBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class ParseFragment : Fragment() {

    private var _binding: FragmentParseBinding? = null
    private val binding get() = _binding!!

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private var currentVideoUrl: String? = null
    private var currentTweetText: String? = null
    private var downloadManager: DownloadManager? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentParseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        downloadManager = DownloadManager(requireContext())

        binding.btnParse.setOnClickListener {
            val link = binding.etLink.text.toString().trim()
            if (link.isEmpty()) {
                Toast.makeText(requireContext(), getString(R.string.please_enter_link), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            parseLink(link)
        }

        binding.btnPlay.setOnClickListener {
            currentVideoUrl?.let { url ->
                val intent = Intent(requireContext(), PlayerActivity::class.java)
                intent.putExtra("video_urls", arrayOf(url))
                intent.putExtra("video_titles", arrayOf(currentTweetText ?: getString(R.string.video_player)))
                startActivity(intent)
            }
        }

        binding.btnDownload.setOnClickListener {
            currentVideoUrl?.let { url ->
                startDownload(url)
            }
        }

        binding.btnPaste.setOnClickListener {
            val clipboard = requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE)
                    as android.content.ClipboardManager
            val clip = clipboard.primaryClip
            if (clip != null && clip.itemCount > 0) {
                binding.etLink.setText(clip.getItemAt(0).text)
            }
        }
    }

    private fun parseLink(link: String) {
        val tweetId = TweetParser.getTweetId(link)
        if (tweetId == null) {
            Toast.makeText(requireContext(), getString(R.string.invalid_link), Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.cardPreview.visibility = View.GONE
        binding.btnParse.isEnabled = false

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val apiUrl = TweetParser.apiUrl(tweetId)
                val request = Request.Builder().url(apiUrl).build()
                val response = client.newCall(request).execute()
                val json = response.body?.string() ?: ""

                val videoUrl = TweetParser.parseAmplifyVideo(json)
                val tweetText = TweetParser.extractTweetText(json)

                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    binding.btnParse.isEnabled = true

                    if (videoUrl != null) {
                        currentVideoUrl = videoUrl
                        currentTweetText = tweetText
                        binding.tvTweetText.text = tweetText.ifEmpty { getString(R.string.video_found) }
                        binding.tvVideoUrl.text = videoUrl
                        binding.cardPreview.visibility = View.VISIBLE
                    } else {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.no_video_found),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    binding.btnParse.isEnabled = true
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.network_error),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun startDownload(url: String) {
        val title = currentTweetText?.take(50) ?: "video_${System.currentTimeMillis()}"
        binding.btnDownload.isEnabled = false
        binding.btnDownload.text = getString(R.string.downloading)

        CoroutineScope(Dispatchers.IO).launch {
            downloadManager?.downloadVideo(
                videoUrl = url,
                fileName = title,
                onComplete = { file ->
                    if (!isAdded) return@downloadVideo
                    activity?.runOnUiThread {
                        binding.btnDownload.isEnabled = true
                        binding.btnDownload.text = getString(R.string.download)
                        if (file != null) {
                            Toast.makeText(
                                requireContext(),
                                getString(R.string.download_complete),
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(
                                requireContext(),
                                getString(R.string.download_failed),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                },
                onProgress = { progress ->
                    if (!isAdded) return@downloadVideo
                    activity?.runOnUiThread {
                        binding.btnDownload.text = "${getString(R.string.downloading)} $progress%"
                    }
                }
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
