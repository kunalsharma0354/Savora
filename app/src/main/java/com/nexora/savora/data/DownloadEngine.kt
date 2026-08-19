package com.nexora.savora.data

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import com.nexora.savora.model.MediaMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

enum class PostProcess { NONE, VIDEO_ONLY, AUDIO_ONLY }

data class MediaInfo(
    val url: String,
    val mime: String,
    val qualityLabel: String,
    val audioUrl: String? = null,
    val qualities: List<String> = emptyList(),
    val process: PostProcess = PostProcess.NONE,
    val title: String? = null,
    val sizeBytes: Long = 0
)

sealed interface FetchResult {
    data class Success(val media: MediaInfo) : FetchResult
    data class Error(val message: String) : FetchResult
}

sealed interface SaveResult {
    data class Success(val fileName: String) : SaveResult
    data class Error(val message: String) : SaveResult
}

object DownloadEngine {

    private const val CONNECT_TIMEOUT = 20_000
    private const val READ_TIMEOUT = 60_000

    // Instagram ke liye cobalt instances (v11). Koi instance auth maange toh agli try,
    // sab blocked rahe toh honest error.
    private val instances = listOf(
        "https://api.cobalt.tools",
        "https://cobalt-backend.canine.tools",
        "https://cobalt-api.kwiatekmiki.com",
        "https://cobalt-api.cirne.xyz"
    )

    private fun qualityParam(videoQuality: String): String =
        videoQuality.filter { it.isDigit() }.ifEmpty { "720" }

    /** 1) Link resolve: YouTube -> Innertube (official API, DASH+merge aware), Instagram -> automatic service. */
    suspend fun resolve(
        url: String,
        mode: MediaMode,
        videoQuality: String,
        audioBitrate: String
    ): FetchResult {
        val videoId = YouTubeApi.videoIdFromUrl(url)
        if (videoId != null) {
            val stream = YouTubeApi.resolveStream(videoId, mode, videoQuality, audioBitrate)
                ?: return FetchResult.Error("This video cannot be downloaded at this quality")
            val qualities = YouTubeApi.availableQualities(videoId)
            val title = YouTubeApi.videoTitle(videoId)
            return FetchResult.Success(
                MediaInfo(
                    url = stream.url,
                    mime = stream.mime,
                    qualityLabel = stream.qualityLabel,
                    audioUrl = stream.audioUrl,
                    qualities = qualities,
                    title = title,
                    sizeBytes = stream.sizeBytes
                )
            )
        }

        val shortcode = InstagramApi.shortcodeFromUrl(url)
        if (shortcode != null) {
            val auto = InstagramApi.resolveReelAuto(shortcode)
            if (auto != null) {
                return FetchResult.Success(
                    MediaInfo(
                        url = auto.url,
                        mime = when (mode) {
                            MediaMode.AUDIO_ONLY -> "audio/mp4"
                            MediaMode.VIDEO_ONLY -> "video/mp4"
                            else -> auto.mime
                        },
                        qualityLabel = when (mode) {
                            MediaMode.AUDIO_ONLY -> "audio"
                            else -> auto.qualityLabel
                        },
                        process = when (mode) {
                            MediaMode.AUDIO_ONLY -> PostProcess.AUDIO_ONLY
                            MediaMode.VIDEO_ONLY -> PostProcess.VIDEO_ONLY
                            else -> PostProcess.NONE
                        }
                    )
                )
            }
            return FetchResult.Error("This reel could not be fetched. Try again in a moment or re-open the app.")
        }

        return cobaltResolve(url, mode, videoQuality)
    }

    private suspend fun cobaltResolve(url: String, mode: MediaMode, videoQuality: String): FetchResult =
        withContext(Dispatchers.IO) {
            val body = JSONObject().apply {
                put("url", url)
                put("videoQuality", qualityParam(videoQuality))
                put("downloadMode", "auto")
                put("audioFormat", "mp3")
                put("audioBitrate", "128")
                put("filenameStyle", "basic")
            }

            var lastError = "Download service unavailable"
            for (instance in instances) {
                try {
                    val json = JSONObject(postJson(instance, body))
                    when (json.optString("status")) {
                        "tunnel", "redirect" -> {
                            val direct = json.optString("url")
                            if (direct.isNotBlank()) {
                                val isAudio = mode == MediaMode.AUDIO_ONLY
                                return@withContext FetchResult.Success(
                                    MediaInfo(
                                        url = direct,
                                        mime = if (isAudio) "audio/mpeg" else "video/mp4",
                                        qualityLabel = if (isAudio) "128 kbps" else videoQuality
                                    )
                                )
                            }
                            lastError = "No download link available for this media"
                        }

                        "error" -> {
                            val err = json.optJSONObject("error")
                            val code = err?.optString("code").orEmpty()
                            lastError = when {
                                code.contains("auth") ->
                                    "Service requires setup (blocked). YouTube downloads still work."
                                else -> err?.optString("message") ?: "Service error"
                            }
                        }

                        else -> lastError = "Unexpected service response"
                    }
                } catch (e: Exception) {
                    lastError = e.message ?: "Network error"
                }
            }
            FetchResult.Error(lastError)
        }

    private fun postJson(instance: String, body: JSONObject): String {
        val conn = (URL("$instance/api/json").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = CONNECT_TIMEOUT
            readTimeout = READ_TIMEOUT
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "Savora/1.0")
        }
        try {
            conn.outputStream.use { it.write(body.toString().toByteArray()) }
            val code = conn.responseCode
            if (code !in 200..299) {
                throw IOException("Service responded $code")
            }
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    /** 2) Direct link se stream download + MediaStore (Downloads folder) mein save. */
    suspend fun downloadAndSave(
        context: Context,
        directUrl: String,
        fileName: String,
        mime: String,
        onProgress: (Int) -> Unit
    ): SaveResult = withContext(Dispatchers.IO) {
        try {
            val conn = (URL(directUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = CONNECT_TIMEOUT
                readTimeout = READ_TIMEOUT
                instanceFollowRedirects = true
                setRequestProperty("Accept", "*/*")
                setRequestProperty("User-Agent", "Savora/1.0")
            }
            conn.connect()
            if (conn.responseCode !in 200..299) {
                return@withContext SaveResult.Error("Server error (${conn.responseCode})")
            }

            val total = conn.contentLengthLong
            val input = BufferedInputStream(conn.inputStream)
            val resolver = context.contentResolver

            // Same naam ka file already hai to silent overwrite mat karo — unique naam banao.
            val base = fileName.substringBeforeLast('.')
            val ext = fileName.substringAfterLast('.', "")
            var finalName = fileName
            var copy = 1
            while (nameExists(resolver, finalName)) {
                finalName = "$base ($copy).$ext"
                copy++
            }

            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, finalName)
                put(MediaStore.Downloads.MIME_TYPE, mime)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Downloads.RELATIVE_PATH, "Download/Savora")
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
            }

            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: return@withContext SaveResult.Error("Could not create file in Downloads")
            val out = resolver.openOutputStream(uri)
                ?: return@withContext SaveResult.Error("Could not open output file")

            try {
                val buf = ByteArray(64 * 1024)
                var written = 0L
                var lastPct = -1
                while (true) {
                    val read = input.read(buf)
                    if (read < 0) break
                    out.write(buf, 0, read)
                    written += read
                    if (total > 0) {
                        val pct = ((written * 100) / total).toInt()
                        if (pct != lastPct) {
                            lastPct = pct
                            onProgress(pct)
                        }
                    }
                }
            } finally {
                try { input.close() } catch (_: IOException) {}
                try { out.close() } catch (_: IOException) {}
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }

            SaveResult.Success(finalName)
        } catch (e: Exception) {
            SaveResult.Error(e.message ?: "Download failed")
        }
    }

    private fun nameExists(resolver: android.content.ContentResolver, name: String): Boolean {
        return try {
            resolver.query(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.Downloads._ID),
                "${MediaStore.Downloads.DISPLAY_NAME}=?",
                arrayOf(name),
                null
            )?.use { it.moveToFirst() } ?: false
        } catch (e: Exception) {
            false
        }
    }

    /** 3.5) Full video download -> runtime strip/extract (video-only / audio-only) -> MediaStore. */
    suspend fun downloadAndSaveProcessed(
        context: Context,
        url: String,
        fileName: String,
        mime: String,
        process: PostProcess,
        onProgress: (Int) -> Unit
    ): SaveResult = withContext(Dispatchers.IO) {
        val dir = java.io.File(context.filesDir, "savora_merge").apply { mkdirs() }
        val ts = System.currentTimeMillis()
        val full = java.io.File(dir, "full_$ts.mp4")
        val out = java.io.File(dir, "out_$ts.mp4")
        try {
            onProgress(0)
            val ok = downloadToFile(url, full) { p -> onProgress((p * 70) / 100) }
            if (!ok) return@withContext SaveResult.Error("Could not download media")

            onProgress(72)
            val processed = when (process) {
                PostProcess.VIDEO_ONLY -> MediaMuxerHelper.stripVideo(full.path, out.path)
                PostProcess.AUDIO_ONLY -> MediaMuxerHelper.extractAudio(full.path, out.path)
                PostProcess.NONE -> true.also { out.copyFrom(full) }
            }
            if (!processed) {
                return@withContext SaveResult.Error(
                    "Processing failed — pick Video + Audio instead"
                )
            }

            onProgress(85)
            val result = saveToMediaStore(context, fileName, mime, out, onProgress = {
                onProgress(85 + (it * 15) / 100)
            })
            full.delete()
            out.delete()
            result
        } catch (e: Exception) {
            SaveResult.Error(e.message ?: "Processing failed")
        } finally {
            try { full.delete() } catch (_: Exception) {}
            try { out.delete() } catch (_: Exception) {}
        }
    }

    private fun java.io.File.copyFrom(src: java.io.File) {
        src.inputStream().use { input ->
            outputStream().use { output -> input.copyTo(output) }
        }
    }

    /** 4) DASH video + audio dono download karke MediaMuxer se merge, phir MediaStore mein save. */
    suspend fun downloadAndSaveMerged(
        context: Context,
        videoUrl: String,
        audioUrl: String,
        fileName: String,
        mime: String,
        onProgress: (Int) -> Unit
    ): SaveResult = withContext(Dispatchers.IO) {
        val dir = java.io.File(context.filesDir, "savora_merge").apply { mkdirs() }
        val ts = System.currentTimeMillis()
        val videoFile = java.io.File(dir, "v_$ts.mp4")
        val audioFile = java.io.File(dir, "a_$ts.m4a")
        val merged = java.io.File(dir, "m_$ts.mp4")
        try {
            onProgress(0)
            val videoOk = downloadToFile(videoUrl, videoFile) { p ->
                onProgress((p * 70) / 100)
            }
            if (!videoOk) return@withContext SaveResult.Error("Could not download video track")
            val audioOk = downloadToFile(audioUrl, audioFile) { p ->
                onProgress(70 + (p * 20) / 100)
            }
            if (!audioOk) return@withContext SaveResult.Error("Could not download audio track")

            onProgress(92)
            val mergedOk = MediaMuxerHelper.merge(videoFile.path, audioFile.path, merged.path)
            if (!mergedOk) {
                return@withContext SaveResult.Error(
                    "Merging failed — pick a lower quality or Video + Audio"
                )
            }

            onProgress(95)
            val result = saveToMediaStore(context, fileName, "video/mp4", merged, onProgress = {
                onProgress(95 + (it * 5) / 100)
            })
            videoFile.delete()
            audioFile.delete()
            merged.delete()
            result
        } catch (e: Exception) {
            SaveResult.Error(e.message ?: "Merge download failed")
        } finally {
            try { videoFile.delete() } catch (_: Exception) {}
            try { audioFile.delete() } catch (_: Exception) {}
            try { merged.delete() } catch (_: Exception) {}
        }
    }

    private fun downloadToFile(url: String, dest: java.io.File, onProgress: (Int) -> Unit): Boolean {
        try {
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = CONNECT_TIMEOUT
                readTimeout = READ_TIMEOUT
                instanceFollowRedirects = true
                setRequestProperty("Accept", "*/*")
                setRequestProperty("User-Agent", "Savora/1.0")
            }
            conn.connect()
            if (conn.responseCode !in 200..299) return false
            val total = conn.contentLengthLong
            val input = BufferedInputStream(conn.inputStream)
            val output = dest.outputStream()
            val buf = ByteArray(64 * 1024)
            var written = 0L
            var last = -1
            try {
                while (true) {
                    val read = input.read(buf)
                    if (read < 0) break
                    output.write(buf, 0, read)
                    written += read
                    if (total > 0) {
                        val pct = ((written * 100) / total).toInt()
                        if (pct != last) {
                            last = pct
                            onProgress(pct)
                        }
                    }
                }
            } finally {
                try { input.close() } catch (_: IOException) {}
                try { output.close() } catch (_: IOException) {}
            }
            return true
        } catch (e: Exception) {
            return false
        }
    }

    private fun saveToMediaStore(
        context: Context,
        fileName: String,
        mime: String,
        file: java.io.File,
        onProgress: (Int) -> Unit = {}
    ): SaveResult {
        try {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, mime)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Downloads.RELATIVE_PATH, "Download/Savora")
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: return SaveResult.Error("Could not create file in Downloads")
            val out = resolver.openOutputStream(uri)
                ?: return SaveResult.Error("Could not open output file")
            val input = file.inputStream()
            val buf = ByteArray(64 * 1024)
            var written = 0L
            val total = file.length()
            try {
                while (true) {
                    val read = input.read(buf)
                    if (read < 0) break
                    out.write(buf, 0, read)
                    written += read
                    if (total > 0) {
                        onProgress(((written * 100) / total).toInt())
                    }
                }
            } finally {
                try { input.close() } catch (_: IOException) {}
                try { out.close() } catch (_: IOException) {}
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            return SaveResult.Success(fileName)
        } catch (e: Exception) {
            return SaveResult.Error(e.message ?: "Save failed")
        }
    }
}