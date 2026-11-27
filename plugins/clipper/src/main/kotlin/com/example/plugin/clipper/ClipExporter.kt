package com.example.plugin.clipper

import android.content.ContentValues
import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileDescriptor
import java.nio.ByteBuffer

/**
 * Cuts a clip out of its video into a new MP4 in Movies/Clipper WITHOUT re-encoding (fast, no quality loss).
 * Because nothing is re-encoded, the cut starts at the nearest key frame at or before the clip's start,
 * which can be a few seconds earlier.
 */
object ClipExporter {
    suspend fun export(context: Context, clip: Clip): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = context.contentResolver
            val safeName = clip.name.replace(Regex("[^\\p{L}\\p{N} _.-]"), "_").trim().ifBlank { "clip" }
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, "$safeName.mp4")
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/Clipper")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
            val collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val target = resolver.insert(collection, values) ?: error("Could not create the output file")
            try {
                val pfd = resolver.openFileDescriptor(target, "w") ?: error("Could not open the output file")
                pfd.use { cut(context, Uri.parse(clip.videoUri), clip.startMs, clip.endMs, it.fileDescriptor) }
                values.clear()
                values.put(MediaStore.Video.Media.IS_PENDING, 0)
                resolver.update(target, values, null, null)
            } catch (t: Throwable) {
                resolver.delete(target, null, null)
                throw t
            }
            "Movies/Clipper/$safeName.mp4"
        }
    }

    private fun cut(context: Context, source: Uri, startMs: Long, endMs: Long, out: FileDescriptor) {
        val extractor = MediaExtractor()
        extractor.setDataSource(context, source, null)
        val muxer = MediaMuxer(out, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var muxerStarted = false
        try {
            val trackMap = HashMap<Int, Int>()
            var bufferSize = 2 * 1024 * 1024
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("video/") || mime.startsWith("audio/")) {
                    extractor.selectTrack(i)
                    trackMap[i] = muxer.addTrack(format)
                    if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                        bufferSize = maxOf(bufferSize, format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE))
                    }
                }
            }
            require(trackMap.isNotEmpty()) { "The video has no audio or video track to copy." }

            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, source)
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
                    ?.toIntOrNull()?.let { muxer.setOrientationHint(it) }
            } finally {
                retriever.release()
            }

            val endUs = endMs * 1000L
            extractor.seekTo(startMs * 1000L, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
            muxer.start()
            muxerStarted = true

            val buffer = ByteBuffer.allocate(bufferSize)
            val info = MediaCodec.BufferInfo()
            var baseTimeUs = -1L
            while (true) {
                buffer.clear()
                val size = extractor.readSampleData(buffer, 0)
                if (size < 0) break
                val timeUs = extractor.sampleTime
                if (timeUs < 0 || timeUs > endUs) break
                if (baseTimeUs < 0) baseTimeUs = timeUs
                info.set(0, size, (timeUs - baseTimeUs).coerceAtLeast(0L), extractor.sampleFlags)
                val track = trackMap[extractor.sampleTrackIndex]
                if (track != null) muxer.writeSampleData(track, buffer, info)
                extractor.advance()
            }
        } finally {
            if (muxerStarted) runCatching { muxer.stop() }
            runCatching { muxer.release() }
            extractor.release()
        }
    }
}
