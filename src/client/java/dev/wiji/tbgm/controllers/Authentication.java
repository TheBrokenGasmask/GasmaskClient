package dev.wiji.tbgm.controllers;

import com.mojang.authlib.exceptions.AuthenticationException;
import dev.wiji.tbgm.GasmaskMain;
import dev.wiji.tbgm.misc.Misc;
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
import java.util.concurrent.atomic.AtomicReference;

public class Authentication {
	public static volatile String token = null;
	private static final WebSocket webSocket = new WebSocket();

	private static final AtomicBoolean INIT_STARTED = new AtomicBoolean(false);
	private static final AtomicReference<CompletableFuture<String>> AUTH_IN_FLIGHT = new AtomicReference<>(null);
	public static final AtomicReference<CompletableFuture<Void>> WS_CONNECT_FUTURE = new AtomicReference<>(null);
	private static final AtomicBoolean TOKEN_VALIDATED = new AtomicBoolean(false);

	private static final ScheduledExecutorService AUTH_EXEC =
			Executors.newSingleThreadScheduledExecutor(r -> {
				Thread t = new Thread(r, "TBGM-Auth");
				t.setDaemon(true);
				return t;
			});

	public static void authInit() {
		if (!INIT_STARTED.compareAndSet(false, true)) {
			return;
		}

		ensureAuthenticated()
				.thenCompose(tok -> connectWebSocketDebounced())
				.exceptionally(ex -> {
					var mc = MinecraftClient.getInstance();
					if (mc.player != null) Misc.sendTbgmErrorMessage("Gasmask Authentication/Socket error: " + ex.getMessage());
					return null;
				});

		AUTH_EXEC.scheduleAtFixedRate(Authentication::checkForAuthentication, 60, 120, TimeUnit.SECONDS);
		AUTH_EXEC.scheduleAtFixedRate(Authentication::refreshTokenIfNeeded, 30, 30, TimeUnit.MINUTES);
	}

	public static String getToken(String uuid) {
		String token = "";
		try {
			String baseUrl = GasmaskMain.getApiUrl();
			if(baseUrl.endsWith("/")) baseUrl = baseUrl.substring(0, baseUrl.length() - 1);

			URL url = URI.create(baseUrl + "/api/authenticate?uuid=" + uuid).toURL();

			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setConnectTimeout(10000);
			conn.setReadTimeout(10000);

			int responseCode = conn.getResponseCode();
			if (responseCode != 200) {
				conn.disconnect();
				return null;
			}

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
				Misc.sendTbgmErrorMessage("Failed to get token from server. Please try entering a valid URL.");
			}
			return null;
		}
		return token;
	}

	public static boolean validateCurrentToken() {
		if (token == null || token.isEmpty()) {
			return false;
		}

		try {
			UUID uuid = MinecraftClient.getInstance().getGameProfile().getId();
			String baseUrl = GasmaskMain.getApiUrl();
			if(baseUrl.endsWith("/")) baseUrl = baseUrl.substring(0, baseUrl.length() - 1);

			URL url = URI.create(baseUrl + "/api/is-authenticated?uuid=" + uuid).toURL();

			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setConnectTimeout(10000);
			conn.setReadTimeout(10000);

			int responseCode = conn.getResponseCode();
			conn.disconnect();

			boolean isValid = (responseCode == 200);
			TOKEN_VALIDATED.set(isValid);

			if (!isValid) {
				if (responseCode == 401 && webSocket.isConnected()) {
					return true;
				}
			}

			return isValid;
		} catch (Exception e) {
			TOKEN_VALIDATED.set(false);

			if (webSocket.isConnected()) {
				return true;
			}

			return false;
		}
	}

	public static void invalidateToken() {
		token = null;
		TOKEN_VALIDATED.set(false);
	}

	public static String waitForAuthentication() {
		CompletableFuture<String> inFlight = AUTH_IN_FLIGHT.get();
		if (inFlight != null) {
			try {
				return inFlight.get(30, TimeUnit.SECONDS);
			} catch (Exception e) {
				return null;
			}
		}
		return token;
	}

	public static String performAuthentication() {
		UUID selectedProfile = MinecraftClient.getInstance().getGameProfile().getId();
		String accessToken = MinecraftClient.getInstance().getSession().getAccessToken();
		String newToken = getToken(selectedProfile.toString());

		if(newToken == null || newToken.isEmpty()) {
			throw new IllegalStateException("Empty token from auth server");
		}

		try {
			MinecraftClient.getInstance().getSessionService().joinServer(selectedProfile, accessToken, newToken);
		} catch(AuthenticationException e) {
			MinecraftClient mc = MinecraftClient.getInstance();
			if(mc.player != null) {
				Misc.sendTbgmErrorMessage("Failed to authenticate with server. Please try entering a valid URL.");
			}
			throw new CompletionException(e);
		}

		MinecraftClient mc = MinecraftClient.getInstance();
		if(mc.player != null) Misc.sendTbgmSuccessMessage("Successfully authenticated with server.");

		token = newToken;
		TOKEN_VALIDATED.set(true);
		return newToken;
	}

	private static CompletableFuture<String> ensureAuthenticated() {
		if (token != null && !token.isEmpty() && TOKEN_VALIDATED.get()) {
			if (!webSocket.isConnected()) {
				connectWebSocketDebounced();
			}
			return CompletableFuture.completedFuture(token);
		}

		for (;;) {
			CompletableFuture<String> inFlight = AUTH_IN_FLIGHT.get();
			if (inFlight != null && !inFlight.isDone()) {
				return inFlight;
			}

			CompletableFuture<String> attempt = CompletableFuture.supplyAsync(() -> {
				return performAuthentication();
			}, AUTH_EXEC).whenComplete((tok, ex) -> {
				try {
					if (ex == null && tok != null && !tok.isBlank()) {
						token = tok;
						TOKEN_VALIDATED.set(true);

						connectWebSocketDebounced().whenComplete((v, wsEx) -> {
							if (wsEx != null) {
								System.err.println("Failed to connect WebSocket after authentication: " + wsEx.getMessage());
							}
						});
					} else {
						TOKEN_VALIDATED.set(false);
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
					if (mc.player != null) Misc.sendTbgmErrorMessage("Gasmask Authentication/Socket error: " + ex.getMessage());
					return null;
				});
	}

	public static void checkForAuthentication() {
		if (token != null && !token.isEmpty()) {
			if (validateCurrentToken()) {
				if (!webSocket.isConnected()) {
					connectWebSocketDebounced();
				} else {
					return;
				}
				return;
			} else {
				if (!webSocket.isConnected()) {
					invalidateToken();
				} else {
					return;
				}
			}
		}

		UUID uuid = MinecraftClient.getInstance().getGameProfile().getId();

		try {
			String baseUrl = GasmaskMain.getApiUrl();
			if(baseUrl.endsWith("/")) baseUrl = baseUrl.substring(0, baseUrl.length() - 1);

			URL url = URI.create(baseUrl + "/api/is-authenticated?uuid=" + uuid).toURL();

			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setConnectTimeout(5000);
			conn.setReadTimeout(5000);

			int responseCode = conn.getResponseCode();
			conn.disconnect();

			if(responseCode == 200) {
				TOKEN_VALIDATED.set(true);
				if (!webSocket.isConnected()) {
					connectWebSocketDebounced();
				}
				return;
			}

			if (!webSocket.isConnected()) {
				sendAuthRequest();
			}
		} catch (Exception e) {
			if (!webSocket.isConnected()) {
				sendAuthRequest();
			}
		}
	}

	public static String getWebSocketToken() {
		try {
			UUID uuid = MinecraftClient.getInstance().getGameProfile().getId();
			String baseUrl = GasmaskMain.getApiUrl();
			if(baseUrl.endsWith("/")) baseUrl = baseUrl.substring(0, baseUrl.length() - 1);

			URL url = URI.create(baseUrl + "/api/auth/websocket-token?uuid=" + uuid).toURL();

			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setConnectTimeout(5000);
			conn.setReadTimeout(5000);

			if (conn.getResponseCode() == 200) {
				try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
					StringBuilder response = new StringBuilder();
					String inputLine;
					while ((inputLine = in.readLine()) != null) {
						response.append(inputLine);
					}

					String wsToken = response.toString().trim();
					if (!wsToken.isEmpty()) {
						return wsToken;
					}
				}
			}
			conn.disconnect();
		} catch (Exception e) {
			System.err.println("Failed to get WebSocket token: " + e.getMessage());
		}

		return null;
	}

	public static void refreshTokenIfNeeded() {
		if (token == null || token.isEmpty() || !TOKEN_VALIDATED.get()) {
			return;
		}

		if (!webSocket.isConnected()) {
			return;
		}

		try {
			UUID uuid = MinecraftClient.getInstance().getGameProfile().getId();
			String baseUrl = GasmaskMain.getApiUrl();
			if(baseUrl.endsWith("/")) baseUrl = baseUrl.substring(0, baseUrl.length() - 1);

			URL url = URI.create(baseUrl + "/api/auth/refresh-token?uuid=" + uuid).toURL();

			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("POST");
			conn.setConnectTimeout(10000);
			conn.setReadTimeout(10000);

			int responseCode = conn.getResponseCode();
			conn.disconnect();

			if (responseCode == 200) {
				TOKEN_VALIDATED.set(true);
			}

		} catch (Exception e) {
			System.err.println("Token refresh failed: " + e.getMessage());
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
						webSocket.connectWithRetry().whenComplete((v, ex) -> {
							if (ex != null) {
								WS_CONNECT_FUTURE.compareAndSet(cf, null);
								var mc = MinecraftClient.getInstance();
								if (mc.player != null) {
									Misc.sendTbgmErrorMessage("Failed to connect websocket.");
								}
								cf.completeExceptionally(ex);
							} else {
								var mc = MinecraftClient.getInstance();
								if (mc.player != null) {
									Misc.sendTbgmSuccessMessage("Websocket connected.");
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

	public static String getCurrentToken() {
		return token;
	}

	public static boolean isTokenValidated() {
		return TOKEN_VALIDATED.get();
	}

	public static boolean isWebSocketConnected() {
		return webSocket.isConnected();
	}

	public static CompletableFuture<Void> forceReauthentication() {
		invalidateToken();
		return ensureAuthenticated()
				.thenCompose(tok -> connectWebSocketDebounced())
				.exceptionally(ex -> {
					var mc = MinecraftClient.getInstance();
					if (mc.player != null) {
						Misc.sendTbgmErrorMessage("Forced reauthentication failed: " + ex.getMessage());
					}
					return null;
				});
	}

	public static CompletableFuture<Void> forceWebSocketConnection() {
		return connectWebSocketDebounced();
	}
}