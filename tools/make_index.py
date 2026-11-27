#!/usr/bin/env python3
"""Collects the built plugin files and writes dist/plugins.json (the store index the app reads).

Usage: python3 tools/make_index.py <base-url>
  <base-url> is where the files in dist/ will be served from, e.g.
  https://raw.githubusercontent.com/<you>/localplayr-plugins/builds
"""
import hashlib
import json
import shutil
import sys
from pathlib import Path

root = Path(__file__).resolve().parent.parent
base_url = sys.argv[1].rstrip("/") if len(sys.argv) > 1 else ""
dist = root / "dist"
if dist.exists():
    shutil.rmtree(dist)
dist.mkdir()

entries = []
for module in sorted((root / "plugins").iterdir()):
    manifest_file = module / "src" / "main" / "assets" / "plugin.json"
    if not manifest_file.exists():
        continue
    manifest = json.loads(manifest_file.read_text(encoding="utf-8"))
    apks = sorted((module / "build" / "outputs" / "apk" / "debug").glob("*.apk"))
    if not apks:
        sys.exit(f"No built APK found for plugin '{manifest['id']}'. Run gradle assembleDebug first.")
    file_name = f"{manifest['id']}-{manifest['versionCode']}.apk"
    target = dist / file_name
    shutil.copyfile(apks[0], target)
    data = target.read_bytes()
    entries.append({
        "id": manifest["id"],
        "name": manifest.get("name", manifest["id"]),
        "versionCode": manifest["versionCode"],
        "versionName": manifest.get("versionName", str(manifest["versionCode"])),
        "apiVersion": manifest.get("apiVersion", 1),
        "minAppVersionCode": manifest.get("minAppVersionCode", 0),
        "author": manifest.get("author", ""),
        "description": manifest.get("description", ""),
        "color": manifest.get("color", ""),
        "icon": manifest.get("icon", ""),
        "url": f"{base_url}/{file_name}",
        "sha256": hashlib.sha256(data).hexdigest(),
        "sizeBytes": len(data),
    })

(dist / "plugins.json").write_text(json.dumps({"plugins": entries}, indent=2, ensure_ascii=False), encoding="utf-8")
print(f"Wrote {len(entries)} plugin(s) to {dist}")
