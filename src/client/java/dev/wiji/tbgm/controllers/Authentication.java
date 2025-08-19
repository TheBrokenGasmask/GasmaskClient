package dev.wiji.tbgm.controllers;

import com.mojang.authlib.exceptions.AuthenticationException;
import dev.wiji.tbgm.GasmaskMain;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class Authentication {
	public static String token = null;
	private static final WebSocket webSocket = new WebSocket();

	private static final AtomicLong CALL_SEQ = new AtomicLong();

	private static final AtomicBoolean INIT_STARTED = new AtomicBoolean(false);
	private static final AtomicReference<CompletableFuture<String>> AUTH_IN_FLIGHT = new AtomicReference<>(null);
	private static final AtomicReference<CompletableFuture<Void>> WS_CONNECT_FUTURE = new AtomicReference<>(null);

	private static final ScheduledExecutorService AUTH_EXEC =
			Executors.newSingleThreadScheduledExecutor(r -> {
				Thread t = new Thread(r, "Wynntracker-Auth");
				t.setDaemon(true);
				return t;
			});

	public static void authInit() {
		if (!INIT_STARTED.compareAndSet(false, true)) {
			System.out.println("Wynntracker authInit already started; skipping");
			return;
		}

		ensureAuthenticated()
				.thenCompose(tok -> connectWebSocketDebounced())
				.exceptionally(ex -> {
					var mc = MinecraftClient.getInstance();
					if (mc.player != null) mc.player.sendMessage(Text.literal("§cWynntracker Authentication/Socket error: " + ex.getMessage()), false);
					return null;
				});

		AUTH_EXEC.scheduleAtFixedRate(Authentication::checkForAuthentication, 60, 60, TimeUnit.SECONDS);
	}

	public static String getToken(String uuid) {
		String token = "";
		try {
			String baseUrl = GasmaskMain.getApiUrl();
			if(baseUrl.endsWith("/")) baseUrl = baseUrl.substring(0, baseUrl.length() - 1);

			URL url = URI.create(baseUrl + "/api/authenticate?uuid=" + uuid).toURL();

			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");

			try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
				String inputLine;
				StringBuilder response = new StringBuilder();
				while ((inputLine = in.readLine()) != null) {
					response.append(inputLine);
				}
				token = response.toString();
			}
			conn.disconnect();
		} catch (Exception e) {
			e.printStackTrace();

			MinecraftClient mc = MinecraftClient.getInstance();
			if(mc.player != null) {
				mc.player.sendMessage(Text.literal("§cFailed to get token from server. Please try entering a valid URL."), false);
			}
			return null;
		}
		return token;
	}

	private static CompletableFuture<String> ensureAuthenticated() {
		if (token != null && !token.isEmpty()) {
			return CompletableFuture.completedFuture(token);
		}

		for (;;) {
			CompletableFuture<String> inFlight = AUTH_IN_FLIGHT.get();
			if (inFlight != null && !inFlight.isDone()) {
				return inFlight;
			}

			CompletableFuture<String> attempt = CompletableFuture.supplyAsync(() -> {
				UUID selectedProfile = MinecraftClient.getInstance().getGameProfile().getId();
				String accessToken = MinecraftClient.getInstance().getSession().getAccessToken();
				String newToken = getToken(selectedProfile.toString());

				if(newToken == null || newToken.isEmpty()) {
					System.out.println("Failed to get token from server.");
					throw new IllegalStateException("Empty token from auth server");
				}

				System.out.println("Sending auth request to server: " + newToken + " with token: " + accessToken + " and profile: " + selectedProfile);

				try {
					MinecraftClient.getInstance().getSessionService().joinServer(selectedProfile, accessToken, newToken);
				} catch(AuthenticationException e) {
					System.out.println("Failed to authenticate with server: " + newToken);

					MinecraftClient mc = MinecraftClient.getInstance();
					if(mc.player != null) {
						mc.player.sendMessage(Text.literal("§cFailed to authenticate with server. Please try entering a valid URL."), false);
					}
					throw new CompletionException(e);
				}

				System.out.println("Successfully authenticated with server: " + newToken);

				MinecraftClient mc = MinecraftClient.getInstance();
				if(mc.player != null) mc.player.sendMessage(Text.literal("§aSuccessfully authenticated with server."), false);

				return newToken;
			}, AUTH_EXEC).whenComplete((tok, ex) -> {
				try {
					if (ex == null && tok != null && !tok.isBlank()) {
						token = tok;
					}
				} finally {
					AUTH_IN_FLIGHT.compareAndSet(AUTH_IN_FLIGHT.get(), null);
				}
			});

			if (AUTH_IN_FLIGHT.compareAndSet(inFlight, attempt)) {
				return attempt;
			}
		}
	}

	public static void sendAuthRequest() {
		ensureAuthenticated()
				.thenCompose(tok -> connectWebSocketDebounced())
				.exceptionally(ex -> {
					var mc = MinecraftClient.getInstance();
					if (mc.player != null) mc.player.sendMessage(Text.literal("§cWynntracker Authentication/Socket error: " + ex.getMessage()), false);
					return null;
				});
	}

	public static void checkForAuthentication() {
		UUID uuid = MinecraftClient.getInstance().getGameProfile().getId();

		try {
			String baseUrl = GasmaskMain.getApiUrl();
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

	private static CompletableFuture<Void> connectWebSocketDebounced() {
		if (webSocket.isConnected()) {
			return CompletableFuture.completedFuture(null);
		}

		for (;;) {
			CompletableFuture<Void> existing = WS_CONNECT_FUTURE.get();

			if (existing != null && !existing.isDone()) return existing;

			if (existing != null && existing.isDone() && !existing.isCompletedExceptionally()) {
				return existing;
			}

			CompletableFuture<Void> cf = new CompletableFuture<>();
			if (WS_CONNECT_FUTURE.compareAndSet(existing, cf)) {
				AUTH_EXEC.schedule(() -> {
					try {
						System.out.println("Waiting 3 seconds before connecting to WebSocket...");
						System.out.println("Attempting to connect to WebSocket...");

						webSocket.connectWithRetry().whenComplete((v, ex) -> {
							if (ex != null) {
								WS_CONNECT_FUTURE.compareAndSet(cf, null);
								System.err.println("Failed to connect WebSocket: " + ex.getMessage());
								var mc = MinecraftClient.getInstance();
								if (mc.player != null) {
									mc.player.sendMessage(Text.literal("§cFailed to connect WebSocket."), false);
								}
								cf.completeExceptionally(ex);
							} else {
								System.out.println("WebSocket connected successfully");
								var mc = MinecraftClient.getInstance();
								if (mc.player != null) {
									mc.player.sendMessage(Text.literal("§aWebSocket connected."), false);
								}
								cf.complete(null);
							}
						});
					} catch (Throwable t) {
						WS_CONNECT_FUTURE.compareAndSet(cf, null);
						cf.completeExceptionally(t);
					}
				}, 3, TimeUnit.SECONDS);
				return cf;
			}
		}
	}

	public static WebSocket getWebSocketManager() {
		return webSocket;
	}
}
