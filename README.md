# NexoraMap

NexoraMap is a performance-first Minecraft map and minimap project derived from [Diorama](https://github.com/SunkenInTime/diorama).

## Target
- Minecraft 1.20.1
- Fabric
- Java 17

## Priorities
1. Minimize CPU, GPU, memory, and background workload.
2. Reduce visual detail when the selected performance profile requires it.
3. Preserve map readability and core functionality.
4. Give NexoraMap its own visual identity and interface.

## Planned performance profiles
- **Ultra performance:** lowest map detail and the smallest background-work budget.
- **Balanced:** adaptive compromise between visual detail and frame stability.
- **Quality:** higher detail and a larger work budget when the machine has headroom.

## Current implementation
- Fabric project metadata and Gradle build configuration.
- Dependency-light performance configuration file (`config/nexoramap.properties`).
- Initial frame-time-aware work-budget controller with smoothed frame-time sampling and hysteresis.

The work-budget controller is currently a foundation only. It is not yet connected to Diorama's map tile baking/render pipeline, and no performance improvement or working release JAR is claimed at this stage.

## Next development milestones
1. Bring in the Minecraft 1.20.1 Diorama/Fabric implementation and preserve its existing map functionality.
2. Connect adaptive scheduling to safe map tile/update work.
3. Add configurable map resolution, render range, refresh cadence, and visual effects.
4. Redesign the HUD and settings screens with NexoraMap branding.
5. Build, launch, and benchmark against the original Diorama baseline.

## Upstream attribution and license
Diorama is created by Dara Adedeji and is distributed under the MIT License. NexoraMap retains the upstream copyright and license notice in [LICENSE-DIORAMA](LICENSE-DIORAMA). The original project source is available at https://github.com/SunkenInTime/diorama.
