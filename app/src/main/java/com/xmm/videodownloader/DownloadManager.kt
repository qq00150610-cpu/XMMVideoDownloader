package com.xmm.videodownloader

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Environment
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class DownloadManager(private val context: Context) {

    companion object {
        private const val CHANNEL_ID = "download_channel"
        private const val NOTIFICATION_ID = 1001
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.download_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.download_channel_desc)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun getDownloadDir(): File {
        val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_MOVIES), "XMMDownloader")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    suspend fun downloadVideo(
        videoUrl: String,
        fileName: String,
        onComplete: (File?) -> Unit,
        onProgress: ((Int) -> Unit)? = null
    ) = withContext(Dispatchers.IO) {
        try {
            val sanitized = fileName.replace(Regex("[^a-zA-Z0-9._\\-]"), "_").take(80)
            val outputFile = File(getDownloadDir(), "${sanitized}.mp4")

            val request = Request.Builder().url(videoUrl).build()
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                withContext(Dispatchers.Main) { onComplete(null) }
                return@withContext
            }

            val body = response.body ?: run {
                withContext(Dispatchers.Main) { onComplete(null) }
                return@withContext
            }

            val totalBytes = body.contentLength()
            var downloadedBytes = 0L

            showProgressNotification(0)

            body.byteStream().use { input ->
                FileOutputStream(outputFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                        if (totalBytes > 0) {
                            val progress = (downloadedBytes * 100 / totalBytes).toInt()
                            withContext(Dispatchers.Main) {
                                onProgress?.invoke(progress)
                                showProgressNotification(progress)
                            }
                        }
                    }
                }
            }

            showCompleteNotification(fileName)
            withContext(Dispatchers.Main) { onComplete(outputFile) }
        } catch (e: Exception) {
            e.printStackTrace()
            showErrorNotification(fileName)
            withContext(Dispatchers.Main) { onComplete(null) }
        }
    }

    private fun showProgressNotification(progress: Int) {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(context.getString(R.string.downloading))
            .setContentText("$progress%")
            .setProgress(100, progress, false)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }

    private fun showCompleteNotification(fileName: String) {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(context.getString(R.string.download_complete))
            .setContentText(fileName)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        notificationManager.notify(NOTIFICATION_ID + 1, builder.build())
        notificationManager.cancel(NOTIFICATION_ID)
    }

    private fun showErrorNotification(fileName: String) {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle(context.getString(R.string.download_failed))
            .setContentText(fileName)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        notificationManager.notify(NOTIFICATION_ID + 2, builder.build())
        notificationManager.cancel(NOTIFICATION_ID)
    }

    fun getDownloadedVideos(): List<VideoItem> {
        val dir = getDownloadDir()
        if (!dir.exists()) return emptyList()
        return dir.listFiles()?.filter { it.extension == "mp4" }
            ?.sortedByDescending { it.lastModified() }
            ?.map { file ->
                VideoItem(
                    title = file.nameWithoutExtension,
                    videoUrl = file.absolutePath,
                    localPath = file.absolutePath,
                    fileSize = file.length(),
                    downloadDate = file.lastModified()
                )
            } ?: emptyList()
    }

    fun deleteVideo(file: File): Boolean {
        return file.delete()
    }
}
