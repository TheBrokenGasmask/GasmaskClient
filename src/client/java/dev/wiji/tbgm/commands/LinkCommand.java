package dev.wiji.tbgm.commands;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.wiji.tbgm.GasmaskMain;
import dev.wiji.tbgm.controllers.Authentication;
import dev.wiji.tbgm.objects.AbstractClientCommand;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import dev.wiji.tbgm.misc.Misc;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

public class LinkCommand extends AbstractClientCommand {
	public LinkCommand() {
		super(
				"link",
				"Links your discord account to your minecraft account",
				"/tbgm link <code>"
		);
	}

	@Override
	public void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
		dispatcher.register(ClientCommandManager.literal(PREFIX)
				.then(ClientCommandManager.literal(name)
						.executes(this::executeLink)
						.then(ClientCommandManager.argument("code", StringArgumentType.word())
								.executes(this::executeLinkWithCode))));
	}

	public int executeLink(CommandContext<FabricClientCommandSource> source) {
		sendErrorMessage(source.getSource(), "Usage: " + usage);
		return 0;
	}

	public int executeLinkWithCode(CommandContext<FabricClientCommandSource> source) {
		String code = StringArgumentType.getString(source, "code");
		sendLinkRequest(source.getSource(), code);

		return 1;
	}

	private void sendLinkRequest(FabricClientCommandSource source, String code) {
		new Thread(() -> {
			StringBuilder urlBuilder = new StringBuilder();
			String baseUrl = GasmaskMain.getApiUrl();
			if(baseUrl.endsWith("/")) baseUrl = baseUrl.substring(0, baseUrl.length() - 1);

			urlBuilder.append(baseUrl).append("/api/verify-link?");
			urlBuilder.append("code=").append(code);
			urlBuilder.append("&token=").append(Authentication.token);
			urlBuilder.append("&uuid=").append(source.getPlayer().getGameProfile().getId());

			try {
				URL url = URI.create(urlBuilder.toString()).toURL();
				System.out.println("Sending discord link request to: " + urlBuilder);

				HttpURLConnection conn = (HttpURLConnection) url.openConnection();
				conn.setRequestMethod("GET");

				int responseCode = conn.getResponseCode();
				String responseMessage = conn.getResponseMessage();

				System.out.println("Sending discord link response code: " + responseCode);
				System.out.println("Sending discord link  response message: " + responseMessage);

				if (responseCode < 200 || responseCode > 299) {
					BufferedReader in = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
					String inputLine;
					StringBuilder response = new StringBuilder();

					while ((inputLine = in.readLine()) != null) response.append(inputLine);
					in.close();

					Gson gson = new Gson();
					JsonObject jsonResponse = gson.fromJson(response.toString(), JsonObject.class);
					String errorMessage = jsonResponse.has("error") ? jsonResponse.get("error").getAsString() : "Unknown error";

					sendErrorMessage(source, "Failed to link discord account: " + errorMessage);
				} else {
					sendSuccessMessage(source, "Successfully linked discord account");
				}

				conn.disconnect();
			} catch (Exception e) {
				sendErrorMessage(source, "Failed to link discord account: " + e.getMessage());
			}
		}).start();
	}
}
