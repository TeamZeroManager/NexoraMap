package dev.nexoramap;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class NexoraMap implements ModInitializer {
    public static final String MOD_ID = "nexoramap";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static PerformanceConfig CONFIG;
    public static AdaptiveWorkBudget WORK_BUDGET;

    @Override
    public void onInitialize() {
        CONFIG = PerformanceConfig.load();
        WORK_BUDGET = new AdaptiveWorkBudget(CONFIG);
        LOGGER.info("NexoraMap performance core initialized (profile: {}).", CONFIG.profile());
    }
}
