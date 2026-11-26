# Local Player plugins

This repository builds the plugins for the Local Player app and publishes the store index the app reads.

## One-time setup

1. Create a **public** GitHub repository named `localplayr-plugins` (the app reads files through
   `raw.githubusercontent.com`, which only works for public repositories) and push this folder to it.
2. In the repository open **Settings > Actions > General** and make sure workflows are allowed to
   run and have **Read and write permissions**.
3. Push to `main`. The *Build plugins* workflow compiles every plugin, creates `plugins.json` and
   publishes everything to the `builds` branch.
4. In the app: **Settings > Plugins**. The store address defaults to
   `https://raw.githubusercontent.com/iSoFe/localplayr-plugins/builds/plugins.json`.
   If your repository has another name or owner, tap **Store address** and change it.

## Day-to-day

* Add or edit a plugin, push, wait for the workflow, then open the store in the app and tap
  **Refresh**. New plugins show **Install**, changed ones show **Update** (only if you raised
  `versionCode` in the plugin's `plugin.json`).
* After installing / updating / removing, the app shows **Restart**. Tap it.

## Layout

```
host-api/            the plugin API exactly as the app defines it (copy, never shipped)
plugins/<name>/      one folder per plugin (an Android application module)
  build.gradle.kts
  src/main/assets/plugin.json      <- the plugin's identity (see PLUGIN_GUIDE.md)
  src/main/kotlin/...              <- the code
tools/make_index.py  builds dist/plugins.json
.github/workflows/   the build + publish workflow
```

To create a plugin, copy `plugins/hello`, rename the folder, and change `plugin.json`, the
`namespace` / `applicationId` in `build.gradle.kts`, and the package and class names.
New folders under `plugins/` are picked up automatically.

See **PLUGIN_GUIDE.md** for everything a plugin can do (and a ready-made brief to give an AI assistant).
