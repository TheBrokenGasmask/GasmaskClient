package dev.wiji.wynntracker;

import dev.wiji.wynntracker.commands.HelpCommand;
import dev.wiji.wynntracker.commands.LinkCommand;
import dev.wiji.wynntracker.commands.ToggleAspectsCommand;
import dev.wiji.wynntracker.commands.UnlinkCommand;
import dev.wiji.wynntracker.controllers.Authentication;
import dev.wiji.wynntracker.controllers.Config;
import dev.wiji.wynntracker.controllers.PlayerManager;
import dev.wiji.wynntracker.objects.ClientCommand;
import dev.wiji.wynntracker.controllers.Updater;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class WynnTrackerClient implements ClientModInitializer {
	public static Config.ConfigData configData;
	private static final List<ClientCommand> commands = new ArrayList<>();
	private static final Logger LOGGER = LoggerFactory.getLogger("WynnTracker");

	private static final String API_URL = "https://wynn.wiji.dev";
	private static final String REPO_URL = "https://api.github.com/repos/wagwanbigmon/TBGMModClient/releases/latest";
	private static final String MOD_ID = "wynntracker";

	@Override
	public void onInitializeClient() {

		configData = Config.getConfigData();

		ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
			Authentication.authInit();
			Updater.checkForUpdates();
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

	public static Logger getLogger() {
		return LOGGER;
	}

	public static String getApiUrl() {
		return API_URL;
	}

	public static String getRepoUrl() {
		return REPO_URL;
	}

	public static String getModId() {
		return MOD_ID;
	}
}