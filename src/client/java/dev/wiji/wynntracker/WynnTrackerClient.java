package dev.wiji.wynntracker;

import dev.wiji.wynntracker.controllers.Authentication;
import dev.wiji.wynntracker.controllers.Config;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.minecraft.network.message.MessageType;

public class WynnTrackerClient implements ClientModInitializer {
	public static Config.ConfigData config_data;

	@Override
	public void onInitializeClient() {
		config_data = Config.getConfigData();

		ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
			Authentication.authInit();
		});
	}
}