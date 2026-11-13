# NexoraMap

NexoraMap is a performance-focused Minecraft map and minimap for **Minecraft 1.20.1, Fabric, Java 17**.

## Build

The build is reproducible and pins the upstream MIT-licensed Diorama source to tag `v0.9.0`. GitHub Actions checks out that exact source, applies the NexoraMap namespace/metadata and performance hooks, then builds only the 1.20.1 Fabric target.

- Source preparation: `tools/prepare_upstream.py`
- Upstream source: [SunkenInTime/diorama at v0.9.0](https://github.com/SunkenInTime/diorama/tree/v0.9.0)
- Build workflow: [GitHub Actions](https://github.com/TeamZeroManager/NexoraMap/actions)
- Output: `nexoramap-<version>+1.20.1.jar` as a workflow artifact

The workflow uses JDK 25 to run the pinned upstream Gradle 9.7.1 build. The mod itself is compiled to Java 17 bytecode, matching Minecraft 1.20.1.

## Performance profiles

The selected profile is stored in `.minecraft/config/nexoramap.properties`. Change `profile` and restart Minecraft.

| Profile | Chunk snapshots per tick | Baker threads | GPU tile cache | Snapshot cap |
|---|---:|---:|---:|---:|
| ULTRA | 4 | Up to 1 | 96 tiles | 1,024 chunks |
| BALANCED | 8 | Up to 2 | 160 tiles | 2,048 chunks |
| QUALITY | 24 | Up to 4 | 256 tiles | 4,096 chunks |

The baker thread count is additionally capped to one worker per three available logical processors, with at least one worker. These are workload and cache limits, not guaranteed FPS values. GPU baking remains governed by the mod's own compatibility checks and settings.

## What is changed

- Rebrands the mod id, Java package, resource namespace, mixin config and output JAR as NexoraMap.
- Keeps the upstream map, minimap, waypoint, storage and rendering functionality from the pinned source.
- Makes chunk snapshot throughput, baker concurrency, retained GPU tiles and snapshot memory cap depend on the selected performance profile.
- Retains the upstream MIT license and copyright notice.

## Important status

The source preparation and CI build pipeline are now in place. A successful build and in-game launch still need to be confirmed by GitHub Actions and a Minecraft 1.20.1 test. No FPS or memory improvement is claimed until benchmarked against the original mod on the same machine.

## License

NexoraMap includes and modifies source distributed under the MIT License. The original copyright and license notice are retained in the resulting JAR.
