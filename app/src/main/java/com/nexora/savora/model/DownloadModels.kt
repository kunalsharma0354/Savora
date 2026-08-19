package com.nexora.savora.model

import androidx.compose.ui.graphics.vector.ImageVector
import com.nexora.savora.data.MediaInfo
import com.nexora.savora.ui.icons.AppIcons

enum class MediaMode(
    val label: String,
    val icon: ImageVector,
    val summary: String
) {
    VIDEO_AUDIO("Video+Audio", AppIcons.Videocam, "MP4 video with sound — recommended"),
    VIDEO_ONLY("Video Only", AppIcons.PlayArrow, "MP4 video without sound"),
    AUDIO_ONLY("Audio Only", AppIcons.MusicNote, "Audio file (M4A / MP3)")
}

enum class DlPhase { Idle, Parsing, Ready, Saving, Saved }
val VideoQualities = listOf("1080p", "720p", "480p", "360p")
val AudioBitrates = listOf("320 kbps", "128 kbps")

fun platformLabel(url: String): String = when {
    url.contains("youtube", ignoreCase = true) || url.contains("youtu.be", ignoreCase = true) -> "YouTube"
    url.contains("instagram", ignoreCase = true) -> "Instagram"
    else -> "Video"
}

fun previewFileName(platform: String, mode: MediaMode, videoQuality: String, audioBitrate: String): String {
    val ext = if (mode == MediaMode.AUDIO_ONLY) "m4a" else "mp4"
    val quality = if (mode == MediaMode.AUDIO_ONLY) {
        audioBitrate.filter { it.isDigit() } + "kbps"
    } else {
        videoQuality
    }
    return "savora_${platform.lowercase()}_$quality.$ext"
}

fun actualFileName(platform: String, media: MediaInfo): String {
    val ext = when {
        media.mime.contains("audio/mpeg") -> "mp3"
        media.mime.contains("audio/mp4") -> "m4a"
        media.mime.contains("audio/webm") -> "webm"
        media.mime.contains("video/webm") -> "webm"
        else -> "mp4"
    }
    val quality = media.qualityLabel.filter {
        it.isDigit() || it in 'a'..'z' || it in 'A'..'Z' || it == '-'
    }
    return "savora_${platform.lowercase()}_$quality.$ext"
}