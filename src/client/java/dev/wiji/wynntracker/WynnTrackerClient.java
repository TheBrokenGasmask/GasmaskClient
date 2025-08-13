package dev.wiji.wynntracker;

import dev.wiji.wynntracker.commands.HelpCommand;
import dev.wiji.wynntracker.commands.LinkCommand;
import dev.wiji.wynntracker.commands.ToggleAspectsCommand;
import dev.wiji.wynntracker.commands.UnlinkCommand;
import dev.wiji.wynntracker.controllers.Authentication;
import dev.wiji.wynntracker.controllers.Config;
import dev.wiji.wynntracker.controllers.PlayerManager;
import dev.wiji.wynntracker.objects.ClientCommand;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class WynnTrackerClient implements ClientModInitializer {
	public static Config.ConfigData config_data;
	private static final List<ClientCommand> commands = new ArrayList<>();
	private static final Logger LOGGER = LoggerFactory.getLogger("WynnTracker");

	@Override
	public void onInitializeClient() {

		config_data = Config.getConfigData();

		ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
			Authentication.authInit();
		});

		registerCommands();
		PlayerManager.startAutoFetch();

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			for (ClientCommand command : commands) {
				command.register(dispatcher);
			}
		});
	}

	private void registerCommands() {
		commands.add(new ToggleAspectsCommand());
		commands.add(new LinkCommand());
		commands.add(new UnlinkCommand());
		commands.add(new HelpCommand(commands));
	}

	public static List<ClientCommand> getCommands() {
		return commands;
	}

	public static void debug(String message) {
		LOGGER.info("[DEBUG] " + message);
	}
}