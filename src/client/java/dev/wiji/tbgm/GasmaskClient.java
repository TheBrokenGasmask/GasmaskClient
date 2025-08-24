package dev.wiji.tbgm;

import dev.wiji.tbgm.commands.*;
import dev.wiji.tbgm.controllers.Authentication;
import dev.wiji.tbgm.controllers.Config;
import dev.wiji.tbgm.controllers.PlayerManager;
import dev.wiji.tbgm.objects.ClientCommand;
import dev.wiji.tbgm.controllers.Updater;
import dev.wiji.tbgm.misc.WynntilsConfig;
//import dev.wiji.tbgm.raid.RaidTracker;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class GasmaskClient implements ClientModInitializer {
	public static Config.ConfigData configData;
	private static final List<ClientCommand> commands = new ArrayList<>();
	private static final Logger LOGGER = LoggerFactory.getLogger("Gasmask");

		private static final String API_URL = dev.wiji.tbgm.BuildConstants.API_URL;
	private static final String REPO_URL = dev.wiji.tbgm.BuildConstants.REPO_URL;
	private static final String MOD_ID = "gasmask";

	@Override
	public void onInitializeClient() {

		configData = Config.getConfigData();

		ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
			Authentication.authInit();
			Updater.checkForUpdates();
		});

		registerCommands();
		PlayerManager.startAutoFetch();
		WynntilsConfig.modifyWynntilsConfig();
		//RaidTracker.initialize();

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
		commands.add(new ReconnectCommand());
		//commands.add(new RaidStatusCommand());
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