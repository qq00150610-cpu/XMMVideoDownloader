package com.xmm.videodownloader

data class VideoItem(
    val title: String,
    val videoUrl: String,
    val thumbnailUrl: String = "",
    val tweetId: String = "",
    val localPath: String = "",
    val fileSize: Long = 0L,
    val downloadDate: Long = System.currentTimeMillis(),
    val duration: Long = 0L
)
