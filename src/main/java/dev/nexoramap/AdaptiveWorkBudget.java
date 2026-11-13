package dev.nexoramap;

/**
 * Frame-time-aware limiter for map baking and refresh queues.
 *
 * It deliberately does not create threads or access Minecraft world state.
 * Integrations should call onFrame once per rendered frame and only submit
 * work that is safe to execute off-thread.
 */
public final class AdaptiveWorkBudget {
    private static final double EWMA_ALPHA = 0.12;

    private final PerformanceConfig config;
    private double smoothedFrameNanos = 16_666_667.0;
    private int currentLimit;
    private int remaining;

    public AdaptiveWorkBudget(PerformanceConfig config) {
        this.config = config;
        this.currentLimit = config.profile().maxJobsPerFrame;
        this.remaining = currentLimit;
    }

    /** Samples the latest frame and prepares this frame's work allowance. */
    public synchronized void onFrame(long frameNanos) {
        if (frameNanos > 0) {
            smoothedFrameNanos += (frameNanos - smoothedFrameNanos) * EWMA_ALPHA;
        }

        int profileLimit = config.profile().maxJobsPerFrame;
        if (!config.adaptive()) {
            currentLimit = profileLimit;
        } else {
            double targetNanos = config.targetFrameTimeMs() * 1_000_000.0;
            double load = smoothedFrameNanos / targetNanos;

            // Hysteresis avoids reacting aggressively to isolated frame spikes.
            if (load > 1.18) {
                currentLimit = Math.max(1, currentLimit - 1);
            } else if (load < 0.78) {
                currentLimit = Math.min(profileLimit, currentLimit + 1);
            }
        }

        remaining = currentLimit;
    }

    /** Returns and consumes the number of jobs currently permitted. */
    public synchronized int consumeAllowance() {
        int granted = remaining;
        remaining = 0;
        return granted;
    }

    public synchronized double averageFrameTimeMs() {
        return smoothedFrameNanos / 1_000_000.0;
    }

    public synchronized int currentLimit() {
        return currentLimit;
    }

    public synchronized int remainingAllowance() {
        return remaining;
    }
}
