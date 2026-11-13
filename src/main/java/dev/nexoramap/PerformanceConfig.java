package dev.nexoramap;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Startup workload profile. Limits are consumed by the chunk snapshot, tile baking,
 * completed-tile upload and GPU tile cache paths.
 */
public final class PerformanceConfig {
    public enum Profile {
        ULTRA(768, 64, 2, 1, 1, 1),
        BALANCED(1536, 128, 6, 2, 2, 2),
        QUALITY(3072, 192, 12, 3, 4, 4);

        final int maxChunks;
        final int maxGpuTiles;
        final int snapshotsPerTick;
        final int maxBakers;
        final int maxConcurrentBakes;
        final int finishedPerTick;

        Profile(int maxChunks, int maxGpuTiles, int snapshotsPerTick, int maxBakers,
                int maxConcurrentBakes, int finishedPerTick) {
            this.maxChunks = maxChunks;
            this.maxGpuTiles = maxGpuTiles;
            this.snapshotsPerTick = snapshotsPerTick;
            this.maxBakers = maxBakers;
            this.maxConcurrentBakes = maxConcurrentBakes;
            this.finishedPerTick = finishedPerTick;
        }
    }

    private static final String FILE_NAME = "nexoramap.properties";
    private final Profile profile;

    private PerformanceConfig(Profile profile) {
        this.profile = profile;
    }

    public static PerformanceConfig defaults() {
        return new PerformanceConfig(Profile.BALANCED);
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
        PerformanceConfig config = new PerformanceConfig(profile);
        config.save(path);
        return config;
    }

    private void save(Path path) {
        Properties properties = new Properties();
        properties.setProperty("profile", profile.name());
        properties.setProperty("comment",
                "Profiles: ULTRA (lowest workload), BALANCED, QUALITY. Restart Minecraft after editing.");
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
    public int maxChunks() { return profile.maxChunks; }
    public int maxGpuTiles() { return profile.maxGpuTiles; }
    public int snapshotsPerTick() { return profile.snapshotsPerTick; }
    public int maxConcurrentBakes() { return profile.maxConcurrentBakes; }
    public int finishedPerTick() { return profile.finishedPerTick; }

    /** Never reserves more workers than one per three logical processors. */
    public int bakerCount() {
        int cpuLimit = Math.max(1, Runtime.getRuntime().availableProcessors() / 3);
        return Math.max(1, Math.min(profile.maxBakers, cpuLimit));
    }
}
