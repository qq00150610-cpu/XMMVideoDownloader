package com.xmm.videodownloader

import com.google.gson.JsonParser

object TweetParser {

    fun getTweetId(link: String): String? {
        return Regex("status/(\\d+)").find(link)?.groupValues?.get(1)
    }

    fun apiUrl(tweetId: String): String {
        return "https://cdn.syndication.twimg.com/tweet-result?id=$tweetId&token=1"
    }

    fun parseAmplifyVideo(jsonText: String): String? {
        return try {
            val root = JsonParser.parseString(jsonText).asJsonObject
            val video = root.getAsJsonObject("video") ?: return null
            val variants = video.getAsJsonArray("variants") ?: return null
            val playlist = variants.map { it.asJsonObject }
                .firstOrNull { it.get("content_type")?.asString == "application/x-mpegURL" }
                ?.get("url")?.asString ?: return null
            playlist
                .replace("/pl/", "/vid/avc1/1280x720/")
                .replace(".m3u8", ".mp4?tag=21")
        } catch (e: Exception) {
            null
        }
    }

    fun extractTweetText(jsonText: String): String {
        return try {
            val root = JsonParser.parseString(jsonText).asJsonObject
            root.get("text")?.asString ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    fun extractThumbnail(jsonText: String): String {
        return try {
            val root = JsonParser.parseString(jsonText).asJsonObject
            val mediaDetails = root.getAsJsonArray("mediaDetails")
            if (mediaDetails != null && mediaDetails.size() > 0) {
                val media = mediaDetails[0].asJsonObject
                media.get("media_url_https")?.asString ?: ""
            } else ""
        } catch (e: Exception) {
            ""
        }
    }
}
