package dev.wiji.tbgm.commands;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import dev.wiji.tbgm.GasmaskClient;
import dev.wiji.tbgm.GasmaskMain;
import dev.wiji.tbgm.controllers.Authentication;
import dev.wiji.tbgm.objects.AbstractClientCommand;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

public class UnlinkCommand extends AbstractClientCommand {
	public UnlinkCommand() {
		super(
				"unlink",
				"Removes the link between your discord account to your minecraft account",
				"/tbgm unlink"
		);
	}

	@Override
	public void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
		dispatcher.register(ClientCommandManager.literal(PREFIX)
				.then(ClientCommandManager.literal(name)
						.executes(this::executeUnlink)));
	}

	public int executeUnlink(CommandContext<FabricClientCommandSource> source) {
		sendUnlinkRequest(source.getSource(), Authentication.token);
		return 1;
	}

	private void sendUnlinkRequest(FabricClientCommandSource source, String code) {
		new Thread(() -> {
			StringBuilder urlBuilder = new StringBuilder();
			String baseUrl = GasmaskClient.getApiUrl();
			if(baseUrl.endsWith("/")) baseUrl = baseUrl.substring(0, baseUrl.length() - 1);

			urlBuilder.append(baseUrl).append("/api/unlink-minecraft?");
			urlBuilder.append("&token=").append(Authentication.token);
			urlBuilder.append("&uuid=").append(source.getPlayer().getGameProfile().getId());

			try {
				URL url = URI.create(urlBuilder.toString()).toURL();
				System.out.println("Sending discord unlink request to: " + urlBuilder);

				HttpURLConnection conn = (HttpURLConnection) url.openConnection();
				conn.setRequestMethod("GET");

				int responseCode = conn.getResponseCode();
				String responseMessage = conn.getResponseMessage();

				System.out.println("Sending discord unlink response code: " + responseCode);
				System.out.println("Sending discord unlink  response message: " + responseMessage);

				if (responseCode < 200 || responseCode > 299) {
					BufferedReader in = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
					String inputLine;
					StringBuilder response = new StringBuilder();

					while ((inputLine = in.readLine()) != null) response.append(inputLine);
					in.close();

					Gson gson = new Gson();
					JsonObject jsonResponse = gson.fromJson(response.toString(), JsonObject.class);
					String errorMessage = jsonResponse.has("error") ? jsonResponse.get("error").getAsString() : "Unknown error";

					sendErrorMessage(source, "Failed to unlink discord account: " + errorMessage);
				} else {
					sendSuccessMessage(source, "Successfully unlinked discord account");
				}

				conn.disconnect();
			} catch (Exception e) {
				sendErrorMessage(source, "Failed to unlink discord account: " + e.getMessage());
			}
		}).start();
	}
}
