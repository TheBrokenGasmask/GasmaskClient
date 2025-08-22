package dev.wiji.tbgm.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import dev.wiji.tbgm.GasmaskClient;
import dev.wiji.tbgm.GasmaskMain;
import dev.wiji.tbgm.controllers.Authentication;
import dev.wiji.tbgm.objects.AbstractClientCommand;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import dev.wiji.tbgm.misc.Misc;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.UUID;

public class ToggleAspectsCommand extends AbstractClientCommand {

	public ToggleAspectsCommand() {
		super(
			"aspects",
			"Toggles your status of needing guild aspects",
			"/tbgm aspects"
		);
	}

	@Override
	public void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
		dispatcher.register(ClientCommandManager.literal(PREFIX)
				.then(ClientCommandManager.literal(name)
						.executes(this::executeToggle)));
	}

	private int executeToggle(CommandContext<FabricClientCommandSource> source) {
		new Thread(() -> {
			UUID reporterID = MinecraftClient.getInstance().getGameProfile().getId();

			StringBuilder urlBuilder = new StringBuilder();
			String baseUrl = GasmaskClient.getApiUrl();
			if(baseUrl.endsWith("/")) baseUrl = baseUrl.substring(0, baseUrl.length() - 1);

			urlBuilder.append(baseUrl).append("/api/toggle-aspects?");
			urlBuilder.append("reporter=").append(reporterID.toString());
			urlBuilder.append("&token=").append(Authentication.token);

			try {
				URL url = URI.create(urlBuilder.toString()).toURL();
				System.out.println("Sending toggle request to: " + urlBuilder);

				HttpURLConnection conn = (HttpURLConnection) url.openConnection();
				conn.setRequestMethod("GET");

				int responseCode = conn.getResponseCode();
				String responseMessage = conn.getResponseMessage();

				System.out.println("Sending toggle response code: " + responseCode);
				System.out.println("Sending toggle response message: " + responseMessage);

				if (responseCode < 200 || responseCode > 299) {
					BufferedReader in = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
					String inputLine;
					StringBuilder response = new StringBuilder();

					while ((inputLine = in.readLine()) != null) response.append(inputLine);
					in.close();

					ClientPlayerEntity player = MinecraftClient.getInstance().player;
					if (player == null) return;
					Misc.sendTbgmErrorMessage("Failed to toggle aspects: " + response);
				} else {
					BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
					String inputLine;
					StringBuilder response = new StringBuilder();

					while ((inputLine = in.readLine()) != null) response.append(inputLine);
					in.close();

					ClientPlayerEntity player = MinecraftClient.getInstance().player;
					if (player == null) return;
					Misc.sendTbgmMessage("Toggled aspects to \"" + getFormattedMessage(response.toString()) + "\"");
				}

				conn.disconnect();
			} catch (Exception e) {
				sendErrorMessage(source.getSource(), "Failed to toggle aspects: " + e.getMessage());
			}

		}).start();

		return 1;
	}

	public String getFormattedMessage(String message) {
		if(message.contains("1")) return "Needs aspects";
		else if(message.contains("0")) return "Does not need aspects";
		else return "Unknown response";
	}
}