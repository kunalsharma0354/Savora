package com.nexora.savora.data

import com.nexora.savora.model.MediaMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

data class ResolvedStream(
    val url: String,
    val mime: String,
    val qualityLabel: String,
    val hasAudio: Boolean,
    val audioUrl: String? = null,
    val sizeBytes: Long = 0
)

data class YtFormat(
    val itag: Int,
    val url: String,
    val mime: String,
    val codec: String,
    val qualityLabel: String,
    val height: Int,
    val bitrate: Int,
    val hasAudio: Boolean,
    val size: Long = 0
)

data class PlayerCache(
    val title: String?,
    val formats: List<YtFormat>
)

object YouTubeApi {

    // Key local.properties se aati hai (YOUTUBE_API_KEY) — repo mein commit nahi hoti.
    private val apiKey: String
        get() = com.nexora.savora.BuildConfig.YOUTUBE_API_KEY

    private const val ENDPOINT = "https://www.youtube.com/youtubei/v1/player"

    // videoId -> player cache (title + formats; URLs expire, ek hi video ke liye cache)
    private var formatsCache: Pair<String, PlayerCache>? = null

    fun videoIdFromUrl(url: String): String? {
        val trimmed = url.trim()
        Regex("[?&]v=([\\w-]{6,})").find(trimmed)?.groupValues?.get(1)?.let { return it }
        Regex("youtu\\.be/([\\w-]{6,})").find(trimmed)?.groupValues?.get(1)?.let { return it }
        return Regex("/(shorts|embed|live|watch)/([\\w-]{6,})").find(trimmed)?.groupValues?.get(2)
    }

    /** Video ka title (Preview card ke liye). */
    suspend fun videoTitle(videoId: String): String? {
        formatsFor(videoId) ?: return null
        return formatsCache?.takeIf { it.first == videoId }?.second?.title
    }

    /** Ek player call — video ke saare available formats (progressive + DASH) parse + cache. */
    suspend fun formatsFor(videoId: String): List<YtFormat>? = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext null
        formatsCache?.takeIf { it.first == videoId }?.second?.formats?.let { return@withContext it }
        try {
            val player = playerJson(videoId) ?: return@withContext null
            val title = player.optJSONObject("videoDetails")?.optString("title")?.takeIf { it.isNotBlank() }
            val streaming = player.optJSONObject("streamingData") ?: return@withContext null
            val out = mutableListOf<YtFormat>()

            fun cleanUrl(raw: String): String = raw.replace("\\u0026", "&")

            fun parse(array: JSONArray?, progressive: Boolean) {
                array ?: return
                for (i in 0 until array.length()) {
                    val f = array.getJSONObject(i)
                    val raw = f.optString("url")
                    if (raw.isEmpty()) continue
                    val mime = f.optString("mimeType").split(";")[0].trim()
                    val codec = f.optString("mimeType")
                        .substringAfter("codecs=\"", "").substringBefore("\"")
                    val label = f.optString("qualityLabel")
                    val height = Regex("(\\d+)p").find(label)
                        ?.groupValues?.get(1)?.toIntOrNull() ?: 0
                    val audio = mime.startsWith("audio/") || progressive
                    out += YtFormat(
                        itag = f.optInt("itag"),
                        url = cleanUrl(raw),
                        mime = mime,
                        codec = codec,
                        qualityLabel = label,
                        height = height,
                        bitrate = f.optInt("bitrate"),
                        hasAudio = audio,
                        size = f.optLong("contentLength")
                    )
                }
            }

            parse(streaming.optJSONArray("formats"), progressive = true)
            parse(streaming.optJSONArray("adaptiveFormats"), progressive = false)

            if (out.isEmpty()) return@withContext null
            formatsCache = videoId to PlayerCache(title, out)
            out
        } catch (e: Exception) {
            null
        }
    }

    /** UI chips ke liye — is video par jo resolutions actually available hain (desc, unique). */
    suspend fun availableQualities(videoId: String): List<String> {
        val formats = formatsFor(videoId) ?: return emptyList()
        return formats
            .filter { !it.hasAudio && it.height > 0 }
            .map { it.height }
            .distinct()
            .sortedDescending()
            .map { "${it}p" }
    }

    /** Mode + requested quality ke hisaab se best stream chunta hai. */
    suspend fun resolveStream(
        videoId: String,
        mode: MediaMode,
        videoQuality: String,
        audioBitrate: String
    ): ResolvedStream? {
        val formats = formatsFor(videoId) ?: return null

        if (mode == MediaMode.AUDIO_ONLY) {
            val reqKbps = audioBitrate.filter { it.isDigit() }.toIntOrNull() ?: 128
            val audios = formats.filter { it.mime.startsWith("audio/") && it.url.isNotEmpty() }
                .sortedWith(
                    compareByDescending<YtFormat> { it.mime.contains("mp4") }
                        .thenByDescending { it.bitrate }
                )
            val best = audios.firstOrNull { (it.bitrate / 1000).coerceAtLeast(1) <= reqKbps }
                ?: audios.firstOrNull() ?: return null
            val kbps = (best.bitrate / 1000).coerceAtLeast(1)
            return ResolvedStream(
                url = best.url,
                mime = best.mime,
                qualityLabel = "$kbps kbps",
                hasAudio = true,
                sizeBytes = best.size
            )
        }

        val requested = videoQuality.filter { it.isDigit() }.toIntOrNull() ?: 720
        val videos = formats.filter { !it.hasAudio && it.mime.startsWith("video/") && it.url.isNotEmpty() }
        val progressives = formats.filter { it.hasAudio && it.url.isNotEmpty() }

        fun bestDash(maxHeight: Int, preferAvc: Boolean): YtFormat? {
            val candidates = videos.filter { it.mime.contains("mp4") && it.height in 1..maxHeight }
            val chosen = if (preferAvc) {
                candidates.filter { it.codec.contains("avc1") }.maxByOrNull { it.height }
                    ?: candidates.maxByOrNull { it.height }
            } else {
                candidates.maxByOrNull { it.height }
            } ?: videos.filter { it.height in 1..maxHeight }.maxByOrNull { it.height }
            return chosen
        }

        fun bestAudioM4a(): YtFormat? = formats
            .filter { it.mime.startsWith("audio/") && it.mime.contains("mp4") && it.url.isNotEmpty() }
            .maxByOrNull { it.bitrate }

        val target = videos.filter { it.height == requested }.maxByOrNull { it.height }

        if (mode == MediaMode.VIDEO_ONLY) {
            val chosen = target ?: bestDash(requested, preferAvc = false) ?: return null
            val quality = if (chosen.height > 0) "${chosen.height}p" else "best"
            return ResolvedStream(
                url = chosen.url,
                mime = chosen.mime,
                qualityLabel = quality,
                hasAudio = false,
                sizeBytes = chosen.size
            )
        }

        // VIDEO_AUDIO: exact requested resolution mile toh merge/single; warna best available
        if (target != null) {
            val prog = progressives.firstOrNull { it.height == requested }
            if (prog != null) {
                return ResolvedStream(
                    url = prog.url,
                    mime = "video/mp4",
                    qualityLabel = "${requested}p",
                    hasAudio = true,
                    sizeBytes = prog.size
                )
            }
            val audio = bestAudioM4a() ?: progressives.firstOrNull() // merge ke liye mp4 audio chahiye
            if (audio != null && audio.mime.contains("mp4") && target.url.isNotEmpty() &&
                target.mime.contains("mp4")
            ) {
                return ResolvedStream(
                    url = target.url,
                    mime = "video/mp4",
                    qualityLabel = "${requested}p",
                    hasAudio = true,
                    audioUrl = audio.url,
                    sizeBytes = target.size + audio.size
                )
            }
        }

        // exact height nahi mila — progressive best (audio ke saath) leni chahiye
        val progBest = progressives.filter { it.height > 0 }.maxByOrNull { it.height }
        if (progBest != null) {
            return ResolvedStream(
                url = progBest.url,
                mime = "video/mp4",
                qualityLabel = "${progBest.height}p",
                hasAudio = true,
                sizeBytes = progBest.size
            )
        }

        // last resort: DASH video + m4a audio merge
        val dash = bestDash(requested.coerceAtLeast(1), preferAvc = true) ?: return null
        val audio = bestAudioM4a() ?: return null
        return ResolvedStream(
            url = dash.url,
            mime = "video/mp4",
            qualityLabel = "${dash.height}p",
            hasAudio = true,
            audioUrl = audio.url,
            sizeBytes = dash.size + audio.size
        )
    }

    private suspend fun playerJson(videoId: String): JSONObject? = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply {
                put(
                    "context",
                    JSONObject().put(
                        "client",
                        JSONObject()
                            .put("clientName", "ANDROID")
                            .put("clientVersion", "20.10.36")
                            .put("androidSdkVersion", 35)
                            .put("hl", "en")
                            .put("gl", "US")
                    )
                )
                put("videoId", videoId)
                put("contentCheckOk", true)
                put("racyCheckOk", true)
                put(
                    "playbackContext",
                    JSONObject().put(
                        "contentPlaybackContext",
                        JSONObject().put("html5Preference", "HTML5_PREF_WANTS")
                    )
                )
            }

            val conn = (URL("$ENDPOINT?key=$apiKey&prettyPrint=false").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 20_000
                readTimeout = 40_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }
            conn.outputStream.use { it.write(body.toString().toByteArray()) }
            if (conn.responseCode !in 200..299) {
                throw IOException("YouTube responded ${conn.responseCode}")
            }
            val response = conn.inputStream.bufferedReader().use { it.readText() }
            val player = JSONObject(response)
            if (player.optString("playabilityStatus").let { it.isEmpty() } ||
                player.getJSONObject("playabilityStatus").optString("status") != "OK"
            ) {
                return@withContext null
            }
            player
        } catch (e: Exception) {
            null
        }
    }
}