package com.example.data

import android.net.Uri

// Copy of the app's Video model (keep identical to com.example.data.Video in the app).
data class Video(
    val id: Long,
    val uri: Uri,
    val name: String,
    val folder: String,
    val bucketId: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val dateAddedSec: Long,
    val width: Int,
    val height: Int,
    val mimeType: String,
    val relativePath: String,
    val isTrashed: Boolean = false,
    val isExternal: Boolean = false
) {
    val displayTitle: String
        get() = name.substringBeforeLast('.').ifBlank { name }

    val isPortrait: Boolean
        get() = height > width && width > 0
}
