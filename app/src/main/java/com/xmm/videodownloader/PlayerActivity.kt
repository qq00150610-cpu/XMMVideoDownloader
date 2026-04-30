package com.xmm.videodownloader

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.xmm.videodownloader.databinding.ActivityPlayerBinding

class PlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlayerBinding
    private var player: ExoPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var hideOverlayRunnable: Runnable? = null
    private var isLocked = false
    private var isLandscape = true

    private val videoUrls = mutableListOf<String>()
    private val videoTitles = mutableListOf<String>()
    private var currentIndex = 0

    private lateinit var audioManager: AudioManager
    private var maxVolume: Int = 0
    private var gestureDetector: GestureDetector? = null

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageManager.applyLocale(newBase))
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.getSelectedColor(this).let {
            setTheme(resources.getIdentifier("AppTheme_${it.name}", "style", packageName))
        }
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideSystemUI()

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

        val urls = intent.getStringArrayExtra("video_urls") ?: arrayOf()
        val titles = intent.getStringArrayExtra("video_titles") ?: arrayOf()
        currentIndex = intent.getIntExtra("current_index", 0)

        videoUrls.addAll(urls)
        videoTitles.addAll(titles)

        if (videoUrls.isEmpty()) {
            Toast.makeText(this, getString(R.string.no_video_url), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initPlayer()
        initControls()
        initGestures()
    }

    private fun hideSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, binding.root)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    private fun initPlayer() {
        player = ExoPlayer.Builder(this).build().also { exo ->
            binding.playerView.player = exo
            binding.playerView.useController = false

            exo.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) {
                        binding.progressBar.visibility = View.GONE
                    } else if (playbackState == Player.STATE_BUFFERING) {
                        binding.progressBar.visibility = View.VISIBLE
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    Toast.makeText(
                        this@PlayerActivity,
                        getString(R.string.playback_error),
                        Toast.LENGTH_SHORT
                    ).show()
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    updateTitle()
                }
            })

            loadCurrentVideo()
        }
    }

    private fun loadCurrentVideo() {
        binding.progressBar.visibility = View.VISIBLE
        player?.let { exo ->
            exo.setMediaItem(MediaItem.fromUri(Uri.parse(videoUrls[currentIndex])))
            exo.prepare()
            exo.playWhenReady = true
        }
        updateTitle()
    }

    private fun updateTitle() {
        binding.tvTitle.text = if (videoTitles.size > currentIndex) {
            videoTitles[currentIndex]
        } else {
            getString(R.string.video_player)
        }
        binding.tvIndex.text = "${currentIndex + 1}/${videoUrls.size}"
    }

    private fun initControls() {
        binding.btnBack.setOnClickListener { finish() }

        binding.btnPlayPause.setOnClickListener {
            player?.let { exo ->
                if (exo.isPlaying) exo.pause() else exo.play()
                updatePlayPauseIcon()
            }
        }

        binding.btnRewind.setOnClickListener {
            player?.let { it.seekTo(maxOf(0, it.currentPosition - 10000)) }
        }

        binding.btnForward.setOnClickListener {
            player?.let { it.seekTo(it.currentPosition + 10000) }
        }

        binding.btnNext.setOnClickListener { playNext() }

        binding.btnPrevious.setOnClickListener { playPrevious() }

        binding.btnLock.setOnClickListener {
            isLocked = !isLocked
            binding.btnLock.setImageResource(
                if (isLocked) android.R.drawable.ic_lock_lock
                else android.R.drawable.ic_lock_idle_lock
            )
            binding.controlsOverlay.visibility = if (isLocked) View.GONE else View.VISIBLE
        }

        binding.btnRotate.setOnClickListener {
            isLandscape = !isLandscape
            requestedOrientation = if (isLandscape) {
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            } else {
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
            }
        }

        binding.playerView.setOnClickListener {
            if (!isLocked) toggleOverlay()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initGestures() {
        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                if (isLocked) return false
                val x = e.x
                val width = binding.playerView.width
                when {
                    x < width / 3 -> {
                        player?.let { it.seekTo(maxOf(0, it.currentPosition - 10000)) }
                        showGestureHint("-10s")
                    }
                    x > width * 2 / 3 -> {
                        player?.let { it.seekTo(it.currentPosition + 10000) }
                        showGestureHint("+10s")
                    }
                }
                return true
            }

            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                if (!isLocked) toggleOverlay()
                return true
            }
        })

        var startY = 0f
        var startVolume = 0
        var startBrightness = 0f
        var isSwipingVolume = false
        var isSwipingBrightness = false

        binding.gestureOverlay.setOnTouchListener { _, event ->
            if (isLocked) return@setOnTouchListener false

            gestureDetector?.onTouchEvent(event)

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startY = event.y
                    startVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                    startBrightness = window.attributes.screenBrightness
                    if (startBrightness < 0) startBrightness = 0.5f
                    isSwipingVolume = event.x > binding.playerView.width / 2
                    isSwipingBrightness = event.x <= binding.playerView.width / 2
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaY = startY - event.y
                    val delta = deltaY / binding.playerView.height
                    if (isSwipingVolume) {
                        val newVolume = (startVolume + delta * maxVolume).toInt().coerceIn(0, maxVolume)
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0)
                        showGestureHint("${getString(R.string.volume)}: ${(newVolume * 100 / maxVolume)}%")
                    } else if (isSwipingBrightness) {
                        val newBrightness = (startBrightness + delta).coerceIn(0.01f, 1f)
                        val lp = window.attributes
                        lp.screenBrightness = newBrightness
                        window.attributes = lp
                        showGestureHint("${getString(R.string.brightness)}: ${(newBrightness * 100).toInt()}%")
                    }
                }
            }
            true
        }
    }

    private fun showGestureHint(text: String) {
        binding.tvGestureHint.text = text
        binding.tvGestureHint.visibility = View.VISIBLE
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({ binding.tvGestureHint.visibility = View.GONE }, 800)
    }

    private fun toggleOverlay() {
        if (binding.controlsOverlay.visibility == View.VISIBLE) {
            hideOverlayDelayed()
        } else {
            binding.controlsOverlay.visibility = View.VISIBLE
            updatePlayPauseIcon()
            hideOverlayDelayed()
        }
    }

    private fun hideOverlayDelayed() {
        hideOverlayRunnable?.let { handler.removeCallbacks(it) }
        hideOverlayRunnable = Runnable {
            binding.controlsOverlay.visibility = View.GONE
        }
        handler.postDelayed(hideOverlayRunnable!!, 4000)
    }

    private fun updatePlayPauseIcon() {
        player?.let {
            binding.btnPlayPause.setImageResource(
                if (it.isPlaying) android.R.drawable.ic_media_pause
                else android.R.drawable.ic_media_play
            )
        }
    }

    private fun playNext() {
        if (currentIndex < videoUrls.size - 1) {
            currentIndex++
            loadCurrentVideo()
        } else {
            Toast.makeText(this, getString(R.string.no_more_videos), Toast.LENGTH_SHORT).show()
        }
    }

    private fun playPrevious() {
        if (currentIndex > 0) {
            currentIndex--
            loadCurrentVideo()
        } else {
            Toast.makeText(this, getString(R.string.first_video), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onPause() {
        super.onPause()
        player?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        player?.release()
        player = null
    }
}
