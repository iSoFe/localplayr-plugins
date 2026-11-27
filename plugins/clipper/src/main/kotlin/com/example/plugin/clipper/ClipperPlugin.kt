package com.example.plugin.clipper

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCut
import com.example.plugins.LocalPlayerPlugin
import com.example.plugins.PluginHost
import com.example.plugins.PluginPlayerAction
import com.example.plugins.PluginTab
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.io.File

/**
 * Clipper: mark the start and end of a part of a video from the player's menu, keep it as a clip,
 * replay only that part, or save it as its own video file.
 */
class ClipperPlugin : LocalPlayerPlugin() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var pendingStart: Pair<Long, Long>? = null // video id to start position

    override fun onLoad(host: PluginHost) {
        val store = ClipStore(File(host.dataDir, "clips.json"))
        val playback = ClipPlayback(host, scope)

        host.registerPlayerAction(
            PluginPlayerAction(
                id = "clip-start",
                title = tr("Clip: set start", "المقطع: تحديد البداية"),
                icon = Icons.Filled.ContentCut
            ) { ctx ->
                val video = ctx.video
                if (video == null) {
                    host.toast(tr("Open a video first", "افتح فيديو أولًا"))
                } else {
                    pendingStart = video.id to ctx.positionMs
                    host.toast(tr("Start set at ${formatTime(ctx.positionMs)}", "تم تحديد البداية عند ${formatTime(ctx.positionMs)}"))
                }
            }
        )

        host.registerPlayerAction(
            PluginPlayerAction(
                id = "clip-end",
                title = tr("Clip: set end & save", "المقطع: تحديد النهاية وحفظ"),
                icon = Icons.Filled.Check
            ) { ctx ->
                val video = ctx.video
                val start = pendingStart
                when {
                    video == null -> host.toast(tr("Open a video first", "افتح فيديو أولًا"))
                    start == null || start.first != video.id ->
                        host.toast(tr("Set the clip start first", "حدّد بداية المقطع أولًا"))
                    ctx.positionMs <= start.second + 500 ->
                        host.toast(tr("The end must be after the start", "يجب أن تكون النهاية بعد البداية"))
                    else -> {
                        val clip = Clip(
                            id = System.currentTimeMillis().toString(),
                            name = "${video.displayTitle} ${formatTime(start.second)}-${formatTime(ctx.positionMs)}",
                            videoId = video.id,
                            videoUri = video.uri.toString(),
                            videoTitle = video.displayTitle,
                            startMs = start.second,
                            endMs = ctx.positionMs,
                            createdAt = System.currentTimeMillis()
                        )
                        store.add(clip)
                        pendingStart = null
                        host.toast(
                            tr(
                                "Clip saved: ${formatTime(clip.startMs)} - ${formatTime(clip.endMs)}",
                                "تم حفظ المقطع: ${formatTime(clip.startMs)} - ${formatTime(clip.endMs)}"
                            )
                        )
                    }
                }
            }
        )

        host.registerTab(
            PluginTab(
                id = "clips",
                title = tr("Clips", "المقاطع"),
                icon = Icons.Filled.ContentCut
            ) {
                ClipsScreen(host, store, playback, scope)
            }
        )
        host.log("Clipper loaded")
    }

    override fun onUnload() {
        pendingStart = null
    }
}
