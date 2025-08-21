package dev.wiji.tbgm;

import dev.wiji.tbgm.commands.HelpCommand;
import dev.wiji.tbgm.commands.LinkCommand;
import dev.wiji.tbgm.commands.ToggleAspectsCommand;
import dev.wiji.tbgm.commands.UnlinkCommand;
import dev.wiji.tbgm.controllers.Authentication;
import dev.wiji.tbgm.controllers.Config;
import dev.wiji.tbgm.controllers.PlayerManager;
import dev.wiji.tbgm.objects.ClientCommand;
import dev.wiji.tbgm.controllers.Updater;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class GasmaskMain implements ClientModInitializer {
	public static Config.ConfigData configData;
	private static final List<ClientCommand> commands = new ArrayList<>();
	private static final Logger LOGGER = LoggerFactory.getLogger("Gasmask");

	private static final String API_URL = "http://localhost:3000";
	private static final String REPO_URL = "https://api.github.com/repos/wagwanbigmon/TBGMModClient/releases/latest";
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