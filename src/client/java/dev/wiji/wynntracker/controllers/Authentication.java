package dev.wiji.wynntracker.controllers;

import com.mojang.authlib.exceptions.AuthenticationException;
import dev.wiji.wynntracker.WynnTrackerClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.UUID;

public class Authentication {
	public static String token = null;
	private static WebSocket webSocket = new WebSocket();

	public static void authInit() {
		sendAuthRequest();

		new Thread(() -> {
			while(true) {

				try {
					Thread.sleep(1000 * 60);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				checkForAuthentication();
			}
		}).start();
	}

	public static String getToken(String uuid) {
		String token = "";
		try {
			String baseUrl = WynnTrackerClient.config_data.apiUrl;
			if(baseUrl.endsWith("/")) baseUrl = baseUrl.substring(0, baseUrl.length() - 1);

			URL url = URI.create(baseUrl + "/api/authenticate?uuid=" + uuid).toURL();

			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");

			BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			String inputLine;
			StringBuilder response = new StringBuilder();

			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}

			in.close();
			token = response.toString();

			conn.disconnect();
		} catch (Exception e) {
			e.printStackTrace();

			MinecraftClient mc = MinecraftClient.getInstance();
			if(mc.player == null) return null;
			mc.player.sendMessage(Text.literal("§cFailed to get token from server. Please try entering a valid URL."), false);
		}
		return token;
	}


	public static void sendAuthRequest() {
		UUID selectedProfile = MinecraftClient.getInstance().getGameProfile().getId();
		String accessToken = MinecraftClient.getInstance().getSession().getAccessToken();
		token = getToken(selectedProfile.toString());

		if(token == null || token.isEmpty()) {
			System.out.println("Failed to get token from server.");
			return;
		}

		System.out.println("Sending auth request to server: " + token + " with token: " + accessToken + " and profile: " + selectedProfile);

		try {
			MinecraftClient.getInstance().getSessionService().joinServer(selectedProfile, accessToken, token);
		} catch(AuthenticationException e) {
			System.out.println("Failed to authenticate with server: " + token);

			MinecraftClient mc = MinecraftClient.getInstance();
			if(mc.player == null) return;
			mc.player.sendMessage(Text.literal("§cFailed to authenticate with server. Please try entering a valid URL."), false);

			return;
		}

		System.out.println("Successfully authenticated with server: " + token);

		MinecraftClient mc = MinecraftClient.getInstance();
		if(mc.player != null) mc.player.sendMessage(Text.literal("§aSuccessfully authenticated with server."), false);

		connectWebSocket();
	}

	public static void checkForAuthentication() {
		UUID uuid = MinecraftClient.getInstance().getGameProfile().getId();

		try {
			String baseUrl = WynnTrackerClient.config_data.apiUrl;
			if(baseUrl.endsWith("/")) baseUrl = baseUrl.substring(0, baseUrl.length() - 1);

			URL url = URI.create(baseUrl + "/api/is-authenticated?uuid=" + uuid).toURL();

			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");

			int responseCode = conn.getResponseCode();
			conn.disconnect();

			if(responseCode == 200) return;

			sendAuthRequest();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void connectWebSocket() {
		new Thread(() -> {
			try {
				System.out.println("Waiting 3 seconds before connecting to WebSocket...");
				Thread.sleep(3000);
				
				System.out.println("Attempting to connect to WebSocket...");
				webSocket.connectWithRetry()
					.thenRun(() -> {
						System.out.println("WebSocket connected successfully");
						MinecraftClient mc = MinecraftClient.getInstance();
						if(mc.player != null) {
							mc.player.sendMessage(Text.literal("§aWebSocket connected."), false);
						}
					})
					.exceptionally(throwable -> {
						System.err.println("Failed to connect WebSocket: " + throwable.getMessage());
						MinecraftClient mc = MinecraftClient.getInstance();
						if(mc.player != null) {
							mc.player.sendMessage(Text.literal("§cFailed to connect WebSocket."), false);
						}
						return null;
					});
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}).start();
	}

	public static WebSocket getWebSocketManager() {
		return webSocket;
	}
}
