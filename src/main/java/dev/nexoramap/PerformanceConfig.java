package dev.nexoramap;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Startup performance profile. The selected limits are consumed by the map's actual
 * snapshot, worker and GPU tile queues after the upstream source is prepared.
 */
public final class PerformanceConfig {
    public enum Profile {
        ULTRA(30, 1, 0.55, 0.50, 1024, 96, 4, 1),
        BALANCED(60, 2, 0.75, 0.75, 2048, 160, 8, 2),
        QUALITY(120, 4, 1.00, 1.00, 4096, 256, 24, 4);

        final int targetFps;
        final int maxJobsPerFrame;
        final double detailScale;
        final double updateScale;
        final int maxChunks;
        final int maxGpuTiles;
        final int snapshotsPerTick;
        final int maxBakers;

        Profile(int targetFps, int maxJobsPerFrame, double detailScale, double updateScale,
                int maxChunks, int maxGpuTiles, int snapshotsPerTick, int maxBakers) {
            this.targetFps = targetFps;
            this.maxJobsPerFrame = maxJobsPerFrame;
            this.detailScale = detailScale;
            this.updateScale = updateScale;
            this.maxChunks = maxChunks;
            this.maxGpuTiles = maxGpuTiles;
            this.snapshotsPerTick = snapshotsPerTick;
            this.maxBakers = maxBakers;
        }
    }

    private static final String FILE_NAME = "nexoramap.properties";
    private final Profile profile;
    private final boolean adaptive;
    private final int targetFrameTimeMs;

    private PerformanceConfig(Profile profile, boolean adaptive, int targetFrameTimeMs) {
        this.profile = profile;
        this.adaptive = adaptive;
        this.targetFrameTimeMs = Math.max(8, Math.min(100, targetFrameTimeMs));
    }

    public static PerformanceConfig defaults() {
        return new PerformanceConfig(Profile.BALANCED, true, 16);
    }

    /** Returns the initialized config, or safe defaults during early class initialization. */
    public static PerformanceConfig active() {
        return NexoraMap.CONFIG == null ? defaults() : NexoraMap.CONFIG;
    }

    public static PerformanceConfig load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
        Properties properties = new Properties();
        if (Files.isRegularFile(path)) {
            try (Reader reader = Files.newBufferedReader(path)) {
                properties.load(reader);
            } catch (IOException exception) {
                NexoraMap.LOGGER.warn("Could not read NexoraMap config; using defaults.", exception);
            }
        }

        PerformanceConfig defaults = defaults();
        Profile profile;
        try {
            profile = Profile.valueOf(properties.getProperty("profile", defaults.profile.name()).trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            profile = defaults.profile;
        }
        boolean adaptive = Boolean.parseBoolean(properties.getProperty("adaptive", Boolean.toString(defaults.adaptive)));
        int frameTime;
        try {
            frameTime = Integer.parseInt(properties.getProperty("targetFrameTimeMs", Integer.toString(defaults.targetFrameTimeMs)));
        } catch (NumberFormatException exception) {
            frameTime = defaults.targetFrameTimeMs;
        }

        PerformanceConfig config = new PerformanceConfig(profile, adaptive, frameTime);
        config.save(path);
        return config;
    }

    private void save(Path path) {
        Properties properties = new Properties();
        properties.setProperty("profile", profile.name());
        properties.setProperty("adaptive", Boolean.toString(adaptive));
        properties.setProperty("targetFrameTimeMs", Integer.toString(targetFrameTimeMs));
        properties.setProperty("comment", "Profiles: ULTRA, BALANCED, QUALITY. Restart Minecraft after editing.");
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path)) {
                properties.store(writer, "NexoraMap performance settings");
            }
        } catch (IOException exception) {
            NexoraMap.LOGGER.warn("Could not save NexoraMap config.", exception);
        }
    }

    public Profile profile() { return profile; }
    public boolean adaptive() { return adaptive; }
    public int targetFrameTimeMs() { return targetFrameTimeMs; }
    public int maxChunks() { return profile.maxChunks; }
    public int maxGpuTiles() { return profile.maxGpuTiles; }
    public int snapshotsPerTick() { return profile.snapshotsPerTick; }

    /** Never reserves more workers than one per three logical processors. */
    public int bakerCount() {
        int cpuLimit = Math.max(1, Runtime.getRuntime().availableProcessors() / 3);
        return Math.max(1, Math.min(profile.maxBakers, cpuLimit));
    }
}
