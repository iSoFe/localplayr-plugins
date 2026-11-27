package com.example.plugin.clipper

import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** One saved part of a video. The video itself is never copied; only where the part starts and ends. */
data class Clip(
    val id: String,
    val name: String,
    val videoId: Long,
    val videoUri: String,
    val videoTitle: String,
    val startMs: Long,
    val endMs: Long,
    val createdAt: Long
) {
    val durationMs: Long get() = (endMs - startMs).coerceAtLeast(0L)
}

/** Keeps the clips in a small JSON file inside the plugin's private folder. */
class ClipStore(private val file: File) {
    private val _clips = MutableStateFlow(load())
    val clips: StateFlow<List<Clip>> = _clips.asStateFlow()

    fun add(clip: Clip) {
        _clips.value = listOf(clip) + _clips.value
        save()
    }

    fun remove(id: String) {
        _clips.value = _clips.value.filterNot { it.id == id }
        save()
    }

    fun rename(id: String, name: String) {
        _clips.value = _clips.value.map { if (it.id == id) it.copy(name = name) else it }
        save()
    }

    private fun load(): List<Clip> {
        if (!file.exists()) return emptyList()
        return runCatching {
            val array = JSONArray(file.readText())
            (0 until array.length()).map { i ->
                val o = array.getJSONObject(i)
                Clip(
                    id = o.getString("id"),
                    name = o.getString("name"),
                    videoId = o.getLong("videoId"),
                    videoUri = o.getString("videoUri"),
                    videoTitle = o.optString("videoTitle", ""),
                    startMs = o.getLong("startMs"),
                    endMs = o.getLong("endMs"),
                    createdAt = o.optLong("createdAt", 0L)
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun save() {
        val array = JSONArray()
        _clips.value.forEach { c ->
            array.put(
                JSONObject()
                    .put("id", c.id)
                    .put("name", c.name)
                    .put("videoId", c.videoId)
                    .put("videoUri", c.videoUri)
                    .put("videoTitle", c.videoTitle)
                    .put("startMs", c.startMs)
                    .put("endMs", c.endMs)
                    .put("createdAt", c.createdAt)
            )
        }
        file.parentFile?.mkdirs()
        file.writeText(array.toString())
    }
}

/** 75000 -> "1:15", 3_725_000 -> "1:02:05". */
fun formatTime(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0L)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s) else String.format(Locale.US, "%d:%02d", m, s)
}

/** Picks the Arabic or English text depending on the app language. */
fun tr(en: String, ar: String): String = if (Locale.getDefault().language == "ar") ar else en
