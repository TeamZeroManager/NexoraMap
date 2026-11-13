#!/usr/bin/env python3
"""Prepare the pinned Diorama source tree as a rebranded NexoraMap build."""
from __future__ import annotations

import json
import shutil
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
UPSTREAM = Path(sys.argv[1]).resolve() if len(sys.argv) > 1 else ROOT / "upstream"

def replace_text_files(base: Path) -> None:
    text_suffixes = {
        ".java", ".json", ".kts", ".properties", ".gradle", ".toml",
        ".glsl", ".vsh", ".fsh", ".mcmeta", ".txt", ".md", ".xml",
        ".accesswidener", ".cfg", ".yml", ".yaml"
    }
    for path in list(base.rglob("*")):
        if not path.is_file() or path.suffix.lower() not in text_suffixes:
            continue
        try:
            content = path.read_text(encoding="utf-8")
        except UnicodeDecodeError:
            continue
        updated = content.replace("diorama", "nexoramap")
        if path.suffix.lower() == ".java":
            import re
            updated = re.sub(
                r'"(?:\\.|[^"\\])*"',
                lambda match: match.group(0).replace("Diorama", "NexoraMap"),
                updated
            )
        else:
            updated = updated.replace("Diorama", "NexoraMap")
        updated = updated.replace("DIORAMA_", "NEXORAMAP_")
        if updated != content:
            path.write_text(updated, encoding="utf-8")

def rename_paths(base: Path) -> None:
    # Rename deepest paths first so parent directories can be moved safely.
    paths = sorted(base.rglob("*"), key=lambda p: len(p.parts), reverse=True)
    for path in paths:
        if not path.exists():
            continue
        name = path.name.replace("diorama", "nexoramap")
        if name != path.name:
            target = path.with_name(name)
            if not target.exists():
                path.rename(target)

    old_package = base / "src/main/java/dev/diorama"
    new_package = base / "src/main/java/dev/nexoramap"
    if old_package.exists() and not new_package.exists():
        old_package.rename(new_package)

def tune_source(base: Path) -> None:
    map_level = base / "src/main/java/dev/nexoramap/render/MapLevel.java"
    content = map_level.read_text(encoding="utf-8")
    replacements = {
        "private static final int MAX_CHUNKS = 4096;":
            "private static final int MAX_CHUNKS = dev.nexoramap.PerformanceConfig.active().maxChunks();",
        "private static final int MAX_GPU_TILES = 256;":
            "private static final int MAX_GPU_TILES = dev.nexoramap.PerformanceConfig.active().maxGpuTiles();",
        "static final int BAKER_COUNT = Math.max(1, Math.min(4, Runtime.getRuntime().availableProcessors() / 3));":
            "static final int BAKER_COUNT = dev.nexoramap.PerformanceConfig.active().bakerCount();",
    }
    for old, new in replacements.items():
        if old not in content:
            raise SystemExit(f"Expected performance hook not found in MapLevel.java: {old}")
        content = content.replace(old, new, 1)
    map_level.write_text(content, encoding="utf-8")

    session = base / "src/main/java/dev/nexoramap/MapSession.java"
    content = session.read_text(encoding="utf-8")
    old = "private static final int SNAPSHOTS_PER_TICK = 24;"
    new = "private static final int SNAPSHOTS_PER_TICK = PerformanceConfig.active().snapshotsPerTick();"
    if old not in content:
        raise SystemExit("Expected SNAPSHOTS_PER_TICK hook not found in MapSession.java")
    content = content.replace(old, new, 1)
    session.write_text(content, encoding="utf-8")

    # Bound render-thread uploads and concurrent tile work. This prevents a burst of
    # completed tiles from all being uploaded in one client tick.
    map_level = base / "src/main/java/dev/nexoramap/render/MapLevel.java"
    content = map_level.read_text(encoding="utf-8")
    old = "int limit = 6;"
    new = "int limit = dev.nexoramap.PerformanceConfig.active().maxConcurrentBakes();"
    if old not in content:
        raise SystemExit("Expected concurrent bake limit not found in MapLevel.java")
    content = content.replace(old, new, 1)
    old = "this.takeFinished(Integer.MAX_VALUE);"
    new = "this.takeFinished(dev.nexoramap.PerformanceConfig.active().finishedPerTick());"
    if old not in content:
        raise SystemExit("Expected finished tile drain not found in MapLevel.java")
    content = content.replace(old, new, 1)
    map_level.write_text(content, encoding="utf-8")

    # Give NexoraMap a distinct, lower-cost default presentation without removing
    # the player's ability to switch back to the original visual options.
    settings = base / "src/main/java/dev/nexoramap/Settings.java"
    content = settings.read_text(encoding="utf-8")
    visual_defaults = {
        "public Quality quality = Quality.FANCY;": "public Quality quality = Quality.FAST;",
        "public Frame minimapFrame = Frame.CLASSIC;": "public Frame minimapFrame = Frame.MINECRAFT;",
        "public Shape minimapShape = Shape.SQUARE;": "public Shape minimapShape = Shape.ROUND;",
        "public RadarStyle radarStyle = RadarStyle.HEADS;": "public RadarStyle radarStyle = RadarStyle.DOTS;",
    }
    for old, new in visual_defaults.items():
        if old not in content:
            raise SystemExit(f"Expected visual default not found in Settings.java: {old}")
        content = content.replace(old, new, 1)
    settings.write_text(content, encoding="utf-8")

def configure_metadata(base: Path) -> None:
    project_properties = base / "gradle.properties"
    project_text = project_properties.read_text(encoding="utf-8")
    project_text = project_text.replace("mod_version=0.9.0", "mod_version=0.1.0")
    project_properties.write_text(project_text, encoding="utf-8")

    metadata_path = base / "src/main/resources/fabric.mod.json"
    metadata = json.loads(metadata_path.read_text(encoding="utf-8"))
    metadata["id"] = "nexoramap"
    metadata["name"] = "NexoraMap"
    metadata["description"] = "A performance-focused angled voxel map and minimap."
    metadata["authors"] = ["Dara Adedeji", "TeamZeroManager"]
    metadata["contact"] = {
        "homepage": "https://github.com/TeamZeroManager/NexoraMap",
        "sources": "https://github.com/TeamZeroManager/NexoraMap",
        "issues": "https://github.com/TeamZeroManager/NexoraMap/issues"
    }
    metadata["icon"] = "assets/nexoramap/icon.png"
    metadata["entrypoints"]["main"] = [
        "dev.nexoramap.NexoraMap",
        "dev.nexoramap.fabric.DioramaFabricCommon"
    ]
    metadata["entrypoints"]["client"] = ["dev.nexoramap.fabric.DioramaFabric"]
    if "modmenu" in metadata["entrypoints"]:
        metadata["entrypoints"]["modmenu"] = ["dev.nexoramap.fabric.ModMenuCompat"]
    metadata["mixins"] = ["nexoramap.mixins.json"]
    metadata.pop("custom", None)
    metadata_path.write_text(json.dumps(metadata, indent=2) + "\n", encoding="utf-8")

def copy_nexora_support(base: Path) -> None:
    source = ROOT / "src/main/java/dev/nexoramap"
    destination = base / "src/main/java/dev/nexoramap"
    if not source.is_dir():
        raise SystemExit("NexoraMap support sources are missing from this repository.")
    destination.mkdir(parents=True, exist_ok=True)
    for file in source.glob("*.java"):
        shutil.copy2(file, destination / file.name)

def main() -> None:
    if not (UPSTREAM / "settings.gradle.kts").is_file():
        raise SystemExit(f"Upstream source checkout not found: {UPSTREAM}")
    replace_text_files(UPSTREAM)
    rename_paths(UPSTREAM)
    tune_source(UPSTREAM)
    configure_metadata(UPSTREAM)
    copy_nexora_support(UPSTREAM)
    print("Prepared Diorama v0.9.0 source as NexoraMap for Minecraft 1.20.1.")

if __name__ == "__main__":
    main()
