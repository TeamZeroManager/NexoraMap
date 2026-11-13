# NexoraMap

NexoraMap is a performance-focused Minecraft map and minimap for **Minecraft 1.20.1, Fabric, Java 17**.

## Build

NexoraMap's own starting version is **0.1.0**; the upstream Diorama source is pinned separately at **v0.9.0**.

The build is reproducible and pins the upstream MIT-licensed Diorama source to tag `v0.9.0`. GitHub Actions checks out that exact source, applies the NexoraMap namespace/metadata and performance hooks, then builds only the 1.20.1 Fabric target.

- Source preparation: `tools/prepare_upstream.py`
- Upstream source: [SunkenInTime/diorama at v0.9.0](https://github.com/SunkenInTime/diorama/tree/v0.9.0)
- Build workflow: [GitHub Actions](https://github.com/TeamZeroManager/NexoraMap/actions)
- Output: `nexoramap-0.1.0+1.20.1.jar` as a workflow artifact

The workflow uses JDK 25 to run the pinned upstream Gradle 9.7.1 build. The mod itself is compiled to Java 17 bytecode, matching Minecraft 1.20.1.

## Install

1. Install Fabric Loader for Minecraft 1.20.1 and Fabric API.
2. Put `nexoramap-0.1.0+1.20.1.jar` in the `mods` folder.
3. Remove the original Diorama JAR to avoid running two map mods at once.
4. Launch the game once. Edit `.minecraft/config/nexoramap.properties` to select `ULTRA`, `BALANCED` or `QUALITY`, then restart.

## Performance profiles

The selected profile is stored in `.minecraft/config/nexoramap.properties`. Change `profile` and restart Minecraft.

| Profile | Chunk snapshots/tick | Baker threads | Concurrent tile jobs | Tile uploads/tick | GPU tile cache | Snapshot cap |
|---|---:|---:|---:|---:|---:|---:|
| ULTRA | 2 | Up to 1 | 1 | 1 | 64 tiles | 768 chunks |
| BALANCED | 6 | Up to 2 | 2 | 2 | 128 tiles | 1,536 chunks |
| QUALITY | 12 | Up to 3 | 4 | 4 | 192 tiles | 3,072 chunks |

The baker thread count is additionally capped to one worker per three available logical processors, with at least one worker. The selected profile also limits how many completed tiles are processed per tick, smoothing bursts of render-thread uploads. These are workload and cache limits, not guaranteed FPS values. GPU baking remains governed by the mod's own compatibility checks and settings.

Fresh NexoraMap settings use a lighter map-baking quality, a rounded Minecraft-style minimap frame and dot-based radar markers. The original visual choices remain available in the settings screen.

## What is changed

- Rebrands the mod id, Java package, resource namespace, mixin config and output JAR as NexoraMap.
- Keeps the upstream map, minimap, waypoint, storage and rendering functionality from the pinned source.
- Makes chunk snapshot throughput, concurrent tile baking, completed-tile processing per tick, retained GPU tiles and snapshot cache cap depend on the selected performance profile.
- Changes fresh-install visual defaults so NexoraMap has a distinct minimap frame/shape, dot radar and lower-cost map quality; existing user choices are preserved.
- Retains the upstream MIT license and copyright notice.

## Important status

The initial 0.1.0 build was launched successfully by the project owner in Minecraft 1.20.1. This updated performance/visual pass still needs a fresh CI build and an in-game regression check. No FPS or memory improvement is claimed until benchmarked against the previous NexoraMap build on the same machine.

## License

NexoraMap includes and modifies source distributed under the MIT License. The original copyright and license notice are retained in the resulting JAR.
