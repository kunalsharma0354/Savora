package com.nexora.savora.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object InstagramApi {

    fun shortcodeFromUrl(url: String): String? =
        Regex("/(reel|p|tv|reels)/([A-Za-z0-9_-]{5,})").find(url.trim())?.groupValues?.get(2)

    /** Fully automatic (bina session ke): public service se direct CDN video link uthata hai. */
    suspend fun resolveReelAuto(shortcode: String): ResolvedStream? = withContext(Dispatchers.IO) {
        try {
            val reelUrl = "https://www.instagram.com/reel/$shortcode/"
            val conn = (URL("https://api.instasave.website/media").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 20_000
                readTimeout = 45_000
                doOutput = true
                setRequestProperty(
                    "Content-Type",
                    "application/x-www-form-urlencoded; charset=UTF-8"
                )
                setRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36"
                )
                setRequestProperty("Origin", "https://instasave.website")
                setRequestProperty("Referer", "https://instasave.website/")
            }
            conn.outputStream.use {
                it.write(("url=" + URLEncoder.encode(reelUrl, "UTF-8")).toByteArray())
            }
            if (conn.responseCode !in 200..299) return@withContext null
            val body = conn.inputStream.bufferedReader().use { it.readText() }

            // Response JS mein "Download" button ka href: https://cdn.instasave.website/?token=<JWT>
            val hrefPattern = Regex(
                """href=\\x22(https://cdn\.instasave\.website/\?token=[A-Za-z0-9_.\-]+)\\x22\\x20class=\\x22abutton\\x20is-success"""
            )
            // Fallback: raw response mein last CDN link (video thumb ke baad aata hai)
            val tokenPattern = Regex("""https://cdn\.instasave\.website/\?token=[A-Za-z0-9_.\-]+""")
            val videoUrl = hrefPattern.find(body)?.groupValues?.get(1)
                ?: tokenPattern.findAll(body).lastOrNull()?.value
                ?: return@withContext null

            ResolvedStream(
                url = videoUrl,
                mime = "video/mp4",
                qualityLabel = "best",
                hasAudio = true
            )
        } catch (e: IOException) {
            null
        }
    }
}