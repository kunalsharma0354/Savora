package com.nexora.savora.data

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import java.nio.ByteBuffer

/** Video track + audio track ko ek MP4 mein merge karta hai (no ffmpeg needed). */
object MediaMuxerHelper {

    fun merge(videoPath: String, audioPath: String, outPath: String): Boolean {
        var videoEx: MediaExtractor? = null
        var audioEx: MediaExtractor? = null
        var muxer: MediaMuxer? = null
        return try {
            videoEx = MediaExtractor()
            videoEx.setDataSource(videoPath)
            val vIdx = trackIndex(videoEx, "video/")
            if (vIdx < 0) return false
            videoEx.selectTrack(vIdx)
            val vFmt = videoEx.getTrackFormat(vIdx)

            audioEx = MediaExtractor()
            audioEx.setDataSource(audioPath)
            val aIdx = trackIndex(audioEx, "audio/")
            if (aIdx < 0) return false
            audioEx.selectTrack(aIdx)
            val aFmt = audioEx.getTrackFormat(aIdx)

            muxer = MediaMuxer(outPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val vTrack = muxer.addTrack(vFmt)
            val aTrack = muxer.addTrack(aFmt)
            muxer.start()

            val buf = ByteBuffer.allocate(4 * 1024 * 1024)
            val info = MediaCodec.BufferInfo()
            while (true) {
                val size = videoEx.readSampleData(buf, 0)
                if (size < 0) break
                info.offset = 0
                info.size = size
                info.presentationTimeUs = videoEx.sampleTime
                info.flags = videoEx.sampleFlags
                muxer.writeSampleData(vTrack, buf, info)
                videoEx.advance()
            }

            while (true) {
                val size = audioEx.readSampleData(buf, 0)
                if (size < 0) break
                info.offset = 0
                info.size = size
                info.presentationTimeUs = audioEx.sampleTime
                info.flags = audioEx.sampleFlags
                muxer.writeSampleData(aTrack, buf, info)
                audioEx.advance()
            }

            muxer.stop()
            muxer.release()
            muxer = null
            videoEx.release()
            videoEx = null
            audioEx.release()
            audioEx = null
            true
        } catch (e: Exception) {
            false
        } finally {
            try { videoEx?.release() } catch (_: Exception) {}
            try { audioEx?.release() } catch (_: Exception) {}
            try { muxer?.release() } catch (_: Exception) {}
        }
    }

    /** Full video file se sirf video track rakhta hai (audio remove). */
    fun stripVideo(srcPath: String, outPath: String): Boolean =
        remuxSingle(srcPath, outPath, "video/")

    /** Full video file se sirf audio track nikalta hai (m4a). */
    fun extractAudio(srcPath: String, outPath: String): Boolean =
        remuxSingle(srcPath, outPath, "audio/")

    private fun remuxSingle(srcPath: String, outPath: String, trackPrefix: String): Boolean {
        var extractor: MediaExtractor? = null
        var muxer: MediaMuxer? = null
        return try {
            extractor = MediaExtractor()
            extractor.setDataSource(srcPath)
            val idx = trackIndex(extractor, trackPrefix)
            if (idx < 0) return false
            extractor.selectTrack(idx)
            val fmt = extractor.getTrackFormat(idx)

            muxer = MediaMuxer(outPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val track = muxer.addTrack(fmt)
            muxer.start()

            val buf = ByteBuffer.allocate(4 * 1024 * 1024)
            val info = MediaCodec.BufferInfo()
            while (true) {
                val size = extractor.readSampleData(buf, 0)
                if (size < 0) break
                info.offset = 0
                info.size = size
                info.presentationTimeUs = extractor.sampleTime
                info.flags = extractor.sampleFlags
                muxer.writeSampleData(track, buf, info)
                extractor.advance()
            }

            muxer.stop()
            muxer.release()
            muxer = null
            extractor.release()
            extractor = null
            true
        } catch (e: Exception) {
            false
        } finally {
            try { extractor?.release() } catch (_: Exception) {}
            try { muxer?.release() } catch (_: Exception) {}
        }
    }

    private fun trackIndex(ex: MediaExtractor, prefix: String): Int {
        for (i in 0 until ex.trackCount) {
            val mime = ex.getTrackFormat(i).getString(MediaFormat.KEY_MIME)
            if (mime?.startsWith(prefix) == true) return i
        }
        return -1
    }
}