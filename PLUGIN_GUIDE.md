# Writing a plugin for Local Player

## How it works (short)

A plugin is a small Android module that Gradle builds into an APK-shaped file. The Local Player
app downloads it from the store, verifies its SHA-256, keeps it in its private storage and, at
every start, loads its code into the app's own process. The plugin then registers what it adds
(a tab, a player menu action, a settings page) and can use any Android, Compose, Material3, Media3
or coroutines API, exactly like the app's own code.

Because loaded code cannot be unloaded, **installing, updating, disabling and removing a plugin
all take effect after an app restart**. The app offers a Restart button for that.

## The files of a plugin

### `src/main/assets/plugin.json`

```json
{
  "id": "clipper",
  "name": "Clipper",
  "versionCode": 3,
  "versionName": "1.2.0",
  "apiVersion": 1,
  "entryClass": "com.example.plugin.clipper.ClipperPlugin",
  "minAppVersionCode": 14,
  "author": "You",
  "description": "Save a part of a video as a clip."
}
```

* `id`: lowercase letters, digits, `.`, `_`, `-`; 2-64 characters; never changes.
* `versionCode`: whole number. **Raise it on every release** or the store will not offer an update.
* `apiVersion`: the plugin API this was written for (currently `1`).
* `entryClass`: the full name of your class that extends `LocalPlayerPlugin`.
* `minAppVersionCode`: the oldest app build the plugin works with.

### The entry class

```kotlin
class ClipperPlugin : LocalPlayerPlugin() {
    override fun onLoad(host: PluginHost) {
        host.registerTab(PluginTab("clips", "Clips", Icons.Filled.ContentCut) { ClipsScreen(host) })
        host.registerPlayerAction(PluginPlayerAction("clip-here", "Save clip", Icons.Filled.ContentCut) { ctx ->
            // ctx.positionMs, ctx.durationMs, ctx.video, ctx.player, ctx.context
        })
        host.registerSettingsPage(PluginSettingsPage("settings", "Clipper settings") { SettingsUi() })
    }
}
```

## The API (version 1) - `com.example.plugins`

| What | Description |
|---|---|
| `PluginHost.registerTab(PluginTab)` | A new section in the bottom bar / side rail. `content` is a Composable shown in the main area. |
| `PluginHost.registerPlayerAction(PluginPlayerAction)` | An item in the player's three-dots menu. `onClick` receives a `PluginPlayerContext`. |
| `PluginHost.registerSettingsPage(PluginSettingsPage)` | A page under Settings > Plugins > your plugin. |
| `PluginHost.currentPlayer()` | The shared Media3 `ExoPlayer` (or null). Main thread only. |
| `PluginHost.currentVideo()` | The `Video` now playing (or null). |
| `PluginHost.libraryVideos()` | The videos the library shows right now. |
| `PluginHost.playVideo(video)` | Opens the full-screen player for a video. |
| `PluginHost.appContext` / `dataDir` | Application context and a private folder for your files. |
| `PluginHost.toast(text)` / `log(text)` | Quick feedback / logcat (`Plugin:<id>`). |
| `PluginPlayerContext` | `context`, `player`, `video`, `positionMs`, `durationMs`. |

`Video` (package `com.example.data`) has `id`, `uri`, `name`, `displayTitle`, `folder`, `durationMs`,
`sizeBytes`, `width`, `height`, `mimeType` and more; see `host-api/.../Video.kt`.

## Rules that keep a plugin working

1. **No Android resources.** Do not use `R.string`, `R.drawable`, XML layouts, etc. Use Compose,
   `Icons.*`, and plain strings in code. (A plugin's resource ids do not exist inside the app.)
2. **No manifest components.** A plugin cannot declare Activities, Services, receivers or
   permissions. It lives inside the app, so it can only use permissions the app already has
   (INTERNET and media access are there). Ask for a new app permission by updating the app.
3. **Everything shared is `compileOnly`.** Compose, Material3, Media3, coroutines and Core KTX come from the
   app. Libraries you add with `implementation(...)` are packaged inside the plugin.
4. **Keep versions in step.** `gradle/libs.versions.toml` here is a copy of the app's. When the app updates
   Compose or Kotlin, copy its catalog over and rebuild the plugins.
5. **Stay on the main thread for UI and the player**; use `Dispatchers.IO` for files and network.
6. **Be defensive.** A crash while a plugin starts switches it off automatically on the next launch, but a
   crash inside its UI closes the app. Catch exceptions in click handlers and background work.
7. Keep your data under `host.dataDir`.

## Build, publish, install

1. Push to GitHub. The workflow builds `gradle assembleDebug`, runs `tools/make_index.py`, and publishes
   `plugins.json` + the plugin files to the `builds` branch.
2. In the app: Settings > Plugins > refresh > Install > Restart.

## Brief to give an AI assistant

> I have an Android video player app ("Local Player", Kotlin, Jetpack Compose, Media3) with a plugin
> system. Write a plugin for it. Use the plugin API in `host-api/src/main/kotlin/com/example/plugins/PluginApi.kt`
> and follow `PLUGIN_GUIDE.md`. Start from the `plugins/hello` module: copy it, give it a new id,
> package and class, and put the identity in `src/main/assets/plugin.json`. Use only Jetpack Compose
> (no XML, no R), Material3, and the APIs the app ships. Register everything inside `onLoad`.
> Wrap risky work in try/catch. Tell me every file you changed and what to put in `plugin.json`.
>
> The plugin should: <describe what you want>.
