package com.example.plugins

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.media3.exoplayer.ExoPlayer
import com.example.data.Video
import kotlinx.coroutines.flow.StateFlow
import java.io.File

/*
 * THE PLUGIN API (version 2: version 1 + declarative settings).
 *
 * This file is the contract between the app and plugins. A copy of it lives in the plugin template
 * (host-api module) so plugins compile against it. Keep both copies identical, and bump
 * PLUGIN_API_VERSION when something changes in a way that old plugins cannot cope with.
 */

const val PLUGIN_API_VERSION = 2

/** Every plugin has exactly one class extending this; its full name goes in plugin.json (entryClass). */
abstract class LocalPlayerPlugin {
    /** Called once at app start (after the app restarted following the install). Register UI here. */
    abstract fun onLoad(host: PluginHost)

    /** Called if the plugin is unloaded gracefully. Optional. */
    open fun onUnload() {}
}

/** A new top-level section of the app (shown in the bottom bar / side rail). */
class PluginTab(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val content: @Composable () -> Unit
)

/** An extra item in the player's "more" (three dots) menu. */
class PluginPlayerAction(
    val id: String,
    val title: String,
    val icon: ImageVector? = null,
    val onClick: (PluginPlayerContext) -> Unit
)

/** A page the plugin shows inside Settings > Plugins (under the plugin's name). */
class PluginSettingsPage(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val content: @Composable () -> Unit
)

/**
 * One entry of a plugin's settings page. A plugin only DECLARES what it wants (see
 * [PluginHost.declareSettings]); the app draws the controls in its own style, stores the values and
 * shows them under the plugin's Settings button. Read the values with [PluginHost.settings].
 */
sealed class PluginSetting {
    /** A small heading that groups the entries below it. */
    class Header(val title: String) : PluginSetting()

    class Switch(
        val key: String,
        val title: String,
        val summary: String? = null,
        val default: Boolean = false
    ) : PluginSetting()

    /** A number between [min] and [max], moved in steps of [step]; [unit] is shown after the value (e.g. "s"). */
    class Slider(
        val key: String,
        val title: String,
        val summary: String? = null,
        val default: Float,
        val min: Float,
        val max: Float,
        val step: Float = 1f,
        val unit: String = ""
    ) : PluginSetting()

    /** One choice out of a few; [options] are (stored value to shown label) pairs. */
    class Choice(
        val key: String,
        val title: String,
        val summary: String? = null,
        val options: List<Pair<String, String>>,
        val default: String
    ) : PluginSetting()

    class Text(
        val key: String,
        val title: String,
        val summary: String? = null,
        val default: String = "",
        val hint: String = ""
    ) : PluginSetting()
}

/** Reads and writes a plugin's setting values. Unset values fall back to the declared default. */
interface PluginSettings {
    fun getBoolean(key: String): Boolean
    fun getInt(key: String): Int
    fun getFloat(key: String): Float
    fun getString(key: String): String
    fun set(key: String, value: Any)

    /** Goes up every time a value changes; read it inside a Composable to redraw when settings change. */
    val revision: StateFlow<Int>
}

/** What the player is doing at the moment the user triggered a [PluginPlayerAction]. */
class PluginPlayerContext(
    val context: Context,
    val player: ExoPlayer?,
    val video: Video?
) {
    val positionMs: Long get() = player?.currentPosition ?: 0L
    val durationMs: Long
        get() = player?.duration?.takeIf { it > 0L } ?: video?.durationMs ?: 0L
}

/** Everything the app offers to a plugin. Plugins can also use any Android / Compose / Media3 API directly. */
interface PluginHost {
    val pluginId: String
    val appContext: Context

    /** A private folder for this plugin's files (created on demand). */
    val dataDir: File

    /** The app's versionCode, for plugins that need to adapt. */
    val hostVersionCode: Int

    fun registerTab(tab: PluginTab)
    fun registerPlayerAction(action: PluginPlayerAction)
    fun registerSettingsPage(page: PluginSettingsPage)

    /** This plugin's stored setting values (see [declareSettings]). */
    val settings: PluginSettings

    /** Declares the plugin's settings. The app shows them behind the plugin's "Settings" button. */
    fun declareSettings(items: List<PluginSetting>)

    /** The shared ExoPlayer, or null while nothing has been opened yet. Call on the main thread. */
    fun currentPlayer(): ExoPlayer?
    fun currentVideo(): Video?

    /** A snapshot of the videos the library currently shows. */
    fun libraryVideos(): List<Video>

    /** Opens the full-screen player for this video. */
    fun playVideo(video: Video)

    fun log(message: String)
    fun toast(message: String)
}
