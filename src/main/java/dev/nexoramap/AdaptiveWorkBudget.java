package dev.nexoramap;

/**
 * Frame-time-aware work limiter for map baking/update queues.
 *
 * This class deliberately does not create threads or touch Minecraft world state.
 * Call onFrame from the client render loop and consumeAllowance before scheduling
 * background-safe map work. World/chunk reads must remain on their required thread.
 */
public final class AdaptiveWorkBudget {
    private static final double EWMA_ALPHA = 0.12;
    private final PerformanceConfig config;
    private double smoothedFrameNanos = 16_666_667.0;
    private int allowance;

    public AdaptiveWorkBudget(PerformanceConfig config) {
        this.config = config;
        this.allowance = config.profile().maxJobsPerFrame;
    }

    public synchronized void onFrame(long frameNanos) {
        if (frameNanos <= 0) return;
        smoothedFrameNanos += (frameNanos - smoothedFrameNanos) * EWMA_ALPHA;

        int profileLimit = config.profile().maxJobsPerFrame;
        if (!config.adaptive()) {
            allowance = profileLimit;
            return;
        }

        double targetNanos = config.targetFrameTimeMs() * 1_000_000.0;
        double load = smoothedFrameNanos / targetNanos;

        // Hysteresis keeps the budget from oscillating on individual slow frames.
        if (load > 1.18) {
            allowance = Math.max(1, allowance - 1);
        } else if (load < 0.78 && allowance < profileLimit) {
            allowance++;
        } else if (allowance > profileLimit) {
            allowance = profileLimit;
        }
    }

    /** Returns and consumes the number of jobs permitted for the current frame. */
    public synchronized int consumeAllowance() {
        int granted = allowance;
        allowance = 0;
        return granted;
    }

    /** Refill the per-frame allowance after the render loop has sampled a new frame. */
    public synchronized void beginFrame() {
        if (allowance == 0) {
            allowance = Math.max(1, Math.min(config.profile().maxJobsPerFrame, allowance));
        }
    }

    public synchronized double averageFrameTimeMs() {
        return smoothedFrameNanos / 1_000_000.0;
    }

    public synchronized int currentAllowance() {
        return allowance;
    }
}
