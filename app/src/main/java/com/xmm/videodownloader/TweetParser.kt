package com.xmm.videodownloader

import com.google.gson.JsonParser

object TweetParser {

    private const val PARSE_API = "https://x-twitter-downloader.com/api/parse-video"

    fun isValidTwitterUrl(link: String): Boolean {
        return try {
            val url = java.net.URL(link)
            listOf("twitter.com", "x.com", "m.twitter.com", "mobile.twitter.com").contains(url.host)
        } catch (e: Exception) {
            false
        }
    }

    fun getTweetId(link: String): String? {
        return Regex("status/(\\d+)").find(link)?.groupValues?.get(1)
    }

    fun buildParseRequestJson(twitterUrl: String): String {
        return """{"url":"$twitterUrl"}"""
    }

    fun getParseApiUrl(): String = PARSE_API

    data class ParseResult(
        val videoUrls: List<VideoQuality>,
        val title: String,
        val thumbnail: String,
        val duration: String,
        val author: String
    )

    data class VideoQuality(
        val url: String,
        val quality: String = "default"
    )

    fun parseResponse(jsonText: String): ParseResult? {
        return try {
            val root = JsonParser.parseString(jsonText).asJsonObject
            val success = root.get("success")?.asBoolean ?: false
            if (!success) return null

            val title = root.get("title")?.asString ?: ""
            val thumbnail = root.get("thumbnail")?.asString ?: ""
            val duration = root.get("duration")?.asString ?: ""
            val author = root.get("author")?.asString ?: ""

            val videosArray = root.getAsJsonArray("videos")
            val videoUrls = mutableListOf<VideoQuality>()

            if (videosArray != null) {
                for (i in 0 until videosArray.size()) {
                    val element = videosArray.get(i)
                    if (element.isJsonObject) {
                        val obj = element.asJsonObject
                        val url = obj.get("url")?.asString ?: obj.get("download_url")?.asString ?: continue
                        val quality = obj.get("quality")?.asString ?: obj.get("label")?.asString ?: "${i}p"
                        videoUrls.add(VideoQuality(url, quality))
                    } else if (element.isJsonPrimitive) {
                        videoUrls.add(VideoQuality(element.asString, "default"))
                    }
                }
            }

            if (videoUrls.isEmpty()) return null

            ParseResult(
                videoUrls = videoUrls,
                title = title,
                thumbnail = thumbnail,
                duration = duration,
                author = author
            )
        } catch (e: Exception) {
            null
        }
    }

    // Legacy methods kept for backward compatibility
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
