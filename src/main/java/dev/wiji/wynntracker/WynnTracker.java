package dev.wiji.wynntracker;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WynnTracker implements ModInitializer {
	public static final String MOD_ID = "wynntracker";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Initializing WynnTracker");
	}
}