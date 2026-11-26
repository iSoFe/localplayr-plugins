package com.example.plugin.hello

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.plugins.LocalPlayerPlugin
import com.example.plugins.PluginHost
import com.example.plugins.PluginPlayerAction
import com.example.plugins.PluginSettingsPage
import com.example.plugins.PluginTab

/** The class named in plugin.json (entryClass). The app creates it and calls onLoad once at start. */
class HelloPlugin : LocalPlayerPlugin() {
    override fun onLoad(host: PluginHost) {
        // 1) A new section in the bottom bar / side rail.
        host.registerTab(
            PluginTab(id = "hello", title = "Hello", icon = Icons.Filled.Star) {
                HelloScreen(host)
            }
        )

        // 2) An extra entry in the player's three-dots menu.
        host.registerPlayerAction(
            PluginPlayerAction(id = "hello-position", title = "Hello: where am I?", icon = Icons.Filled.Star) { ctx ->
                val title = ctx.video?.displayTitle ?: "no video"
                host.toast("$title - ${ctx.positionMs / 1000}s of ${ctx.durationMs / 1000}s")
            }
        )

        // 3) A page under Settings > Plugins > Hello Plugin.
        host.registerSettingsPage(
            PluginSettingsPage(id = "about", title = "About Hello") {
                Text(
                    text = "This page is drawn by the plugin. Data folder: ${host.dataDir.absolutePath}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp)
                )
            }
        )
        host.log("Hello plugin loaded")
    }
}

@Composable
private fun HelloScreen(host: PluginHost) {
    var count by remember { mutableIntStateOf(0) }
    val videos = host.libraryVideos().take(30)
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Hello from a plugin!", style = MaterialTheme.typography.headlineSmall)
        Button(onClick = { count++ }) { Text("Tapped $count times") }
        Text("Tap a video to play it:", style = MaterialTheme.typography.titleMedium)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(videos) { video ->
                Text(
                    text = video.displayTitle,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { host.playVideo(video) }
                        .padding(vertical = 10.dp)
                )
            }
        }
    }
}
