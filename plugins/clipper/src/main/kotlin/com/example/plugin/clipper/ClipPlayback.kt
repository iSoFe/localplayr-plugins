package com.example.plugin.clipper

import androidx.media3.common.Player
import com.example.plugins.PluginHost
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Opens the full video player at the clip's start and pauses when the clip's end is reached. */
class ClipPlayback(private val host: PluginHost, private val scope: CoroutineScope) {
    private var job: Job? = null

    fun play(clip: Clip) {
        val video = host.libraryVideos().firstOrNull { it.id == clip.videoId }
        if (video == null) {
            host.toast(tr("The original video was not found in the library.", "لم يُعثر على الفيديو الأصلي في المكتبة."))
            return
        }
        job?.cancel()
        host.playVideo(video)
        job = scope.launch {
            // Wait (up to ~8 s) until the player really shows this video.
            var waited = 0
            while (waited < 8000) {
                val player = host.currentPlayer()
                if (host.currentVideo()?.id == clip.videoId && player != null &&
                    player.playbackState == Player.STATE_READY
                ) break
                delay(100)
                waited += 100
            }
            val player = host.currentPlayer() ?: return@launch
            if (host.currentVideo()?.id != clip.videoId) return@launch
            player.seekTo(clip.startMs)
            player.play()
            // Watch the position; stop at the end of the clip unless the user moved on to another video.
            while (true) {
                delay(250)
                val current = host.currentPlayer() ?: break
                if (host.currentVideo()?.id != clip.videoId) break
                if (current.currentPosition >= clip.endMs) {
                    current.pause()
                    host.toast(tr("Clip finished", "انتهى المقطع"))
                    break
                }
            }
        }
    }

    fun cancel() {
        job?.cancel()
    }
}
