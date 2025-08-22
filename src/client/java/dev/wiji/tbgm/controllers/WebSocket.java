package dev.wiji.tbgm.controllers;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.wiji.tbgm.GasmaskClient;
import dev.wiji.tbgm.GasmaskMain;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket.Listener;
import java.nio.ByteBuffer;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class WebSocket {
    private volatile java.net.http.WebSocket webSocket;
    private final HttpClient httpClient;
    private URI wsUri;

    private final AtomicBoolean isConnected = new AtomicBoolean(false);
    private final AtomicBoolean shouldReconnect = new AtomicBoolean(true);
    private final AtomicReference<CompletableFuture<Void>> connectFutureRef = new AtomicReference<>(null);
    private final AtomicBoolean reconnectScheduled = new AtomicBoolean(false);
    private final AtomicBoolean authenticationInProgress = new AtomicBoolean(false);

    private static final int MAX_RECONNECT_ATTEMPTS = 5;
    private static final int RECONNECT_DELAY_MS = 5000;
    private static final int HEARTBEAT_INTERVAL_MS = 30000;
    private static final int HEARTBEAT_TIMEOUT_MS = 45000;

    private volatile int reconnectAttempts = 0;
    private volatile long lastHeartbeatSent = 0;
    private volatile long lastHeartbeatReceived = 0;
    private volatile ScheduledFuture<?> heartbeatTask;

    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "TBGM-Websocket");
                t.setDaemon(true);
                return t;
            });

    public WebSocket() {
        this.httpClient = HttpClient.newHttpClient();
    }

    public String convertHttpToWebSocketUrl(String httpUrl) {
        if (httpUrl == null || httpUrl.isEmpty()) {
            return null;
        }

        String wsUrl = httpUrl.trim();
        while (wsUrl.endsWith("/")) wsUrl = wsUrl.substring(0, wsUrl.length() - 1);

        if (wsUrl.startsWith("http://")) wsUrl = wsUrl.replaceFirst("^http://", "ws://");
        else if (wsUrl.startsWith("https://")) wsUrl = wsUrl.replaceFirst("^https://", "wss://");
        else if (!wsUrl.startsWith("ws://") && !wsUrl.startsWith("wss://")) wsUrl = "ws://" + wsUrl;

        wsUrl += "/ws";
        return wsUrl;
    }

    public CompletableFuture<Void> connectWithRetry() {
        shouldReconnect.set(true);
        reconnectAttempts = 0;
        return connectSingleFlight();
    }

    private CompletableFuture<Void> connectSingleFlight() {
        if (isConnected.get() && webSocket != null && !webSocket.isOutputClosed()) {
            CompletableFuture<Void> existing = connectFutureRef.get();
            return (existing != null) ? existing : CompletableFuture.completedFuture(null);
        }

        for (;;) {
            CompletableFuture<Void> existing = connectFutureRef.get();

            if (existing != null) {
                if (!existing.isDone()) return existing;
            }

            CompletableFuture<Void> cf = new CompletableFuture<>();
            if (connectFutureRef.compareAndSet(existing, cf)) {
                return validateTokenAndConnect(cf);
            }
        }
    }

    private CompletableFuture<Void> validateTokenAndConnect(CompletableFuture<Void> cf) {
        return CompletableFuture.supplyAsync(() -> {
            String currentToken = Authentication.token;

            if (currentToken == null || currentToken.isEmpty() || !Authentication.validateCurrentToken()) {
                if (!authenticationInProgress.compareAndSet(false, true)) {
                    return Authentication.waitForAuthentication();
                }

                try {
                    String newToken = Authentication.performAuthentication();
                    if (newToken.isEmpty()) {
                        throw new IllegalStateException("Failed to obtain valid authentication token");
                    }
                    return newToken;
                } finally {
                    authenticationInProgress.set(false);
                }
            }

            return currentToken;
        }, scheduler).thenCompose(token -> CompletableFuture.supplyAsync(() -> {
			String wsToken = Authentication.getWebSocketToken();
			if (wsToken == null || wsToken.isEmpty()) throw new IllegalStateException("Failed to get websocket token");
			return wsToken;
		}, scheduler)).thenCompose(wsToken -> attemptWebSocketConnection(cf, wsToken)).exceptionally(ex -> {
            connectFutureRef.set(null);
            cf.completeExceptionally(ex);
            return null;
        });
    }

    private CompletableFuture<Void> attemptWebSocketConnection(CompletableFuture<Void> cf, String authToken) {
        String httpUrl = GasmaskClient.getApiUrl();
        String wsUrl = convertHttpToWebSocketUrl(httpUrl);

        if (wsUrl == null) {
            connectFutureRef.set(null);
            cf.completeExceptionally(new IllegalArgumentException("Invalid URL"));
            return cf;
        }

        try {
            this.wsUri = URI.create(wsUrl);
            httpClient.newWebSocketBuilder()
                    .header("token", authToken)
                    .buildAsync(wsUri, new WebSocketListener())
                    .whenComplete((ws, ex) -> {
                        if (ex != null) {
                            connectFutureRef.set(null);
                            isConnected.set(false);

                            if (isAuthenticationError(ex)) {
                                Authentication.invalidateToken();
                                scheduleReconnectWithAuth();
                            } else {
                                scheduleReconnect();
                            }
                            cf.completeExceptionally(ex);
                        } else {
                            this.webSocket = ws;
                            startHeartbeat();
                            cf.complete(null);
                        }
                    });
            return cf;
        } catch (Exception e) {
            connectFutureRef.set(null);
            cf.completeExceptionally(e);
            return cf;
        }
    }

    private boolean isAuthenticationError(Throwable ex) {
        String message = ex.getMessage();
        return message != null && (
                message.contains("401") ||
                        message.contains("403") ||
                        message.contains("Unauthorized") ||
                        message.contains("Invalid authentication") ||
                        message.contains("Authentication failed")
        );
    }

    private void startHeartbeat() {
        stopHeartbeat();

        heartbeatTask = scheduler.scheduleAtFixedRate(() -> {
            if (isConnected.get() && webSocket != null && !webSocket.isOutputClosed()) {
                sendHeartbeat();
                checkHeartbeatTimeout();
            }
        }, HEARTBEAT_INTERVAL_MS, HEARTBEAT_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    private void stopHeartbeat() {
        if (heartbeatTask != null && !heartbeatTask.isCancelled()) {
            heartbeatTask.cancel(false);
        }
    }

    private void sendHeartbeat() {
        try {
            lastHeartbeatSent = System.currentTimeMillis();
            JsonObject heartbeat = new JsonObject();
            heartbeat.addProperty("type", "heartbeat");
            heartbeat.addProperty("timestamp", lastHeartbeatSent);

            if (webSocket != null && !webSocket.isOutputClosed()) {
                webSocket.sendText(heartbeat.toString(), true);
            }
        } catch (Exception e) {
            handleConnectionLoss();
        }
    }

    private void checkHeartbeatTimeout() {
        long now = System.currentTimeMillis();
        if (lastHeartbeatSent > 0 && lastHeartbeatReceived > 0) {
            long timeSinceLastResponse = now - lastHeartbeatReceived;
            long timeSinceLastSent = now - lastHeartbeatSent;

            if (timeSinceLastSent < HEARTBEAT_TIMEOUT_MS && timeSinceLastResponse > HEARTBEAT_TIMEOUT_MS) {
                handleConnectionLoss();
            }
        }
    }

    private void handleConnectionLoss() {
        if (isConnected.compareAndSet(true, false)) {
            stopHeartbeat();
            connectFutureRef.set(null);
            scheduleReconnect();
        }
    }

    private void scheduleReconnect() {
        if (!shouldReconnect.get()) return;
        if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            return;
        }
        if (!reconnectScheduled.compareAndSet(false, true)) return;

        reconnectAttempts++;

        scheduler.schedule(() -> {
            reconnectScheduled.set(false);
            if (shouldReconnect.get() && !isConnected.get()) {
                connectSingleFlight();
            }
        }, RECONNECT_DELAY_MS, TimeUnit.MILLISECONDS);
    }

    private void scheduleReconnectWithAuth() {
        if (!shouldReconnect.get()) return;
        if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            return;
        }
        if (!reconnectScheduled.compareAndSet(false, true)) return;

        reconnectAttempts++;

        scheduler.schedule(() -> {
            reconnectScheduled.set(false);
            if (shouldReconnect.get() && !isConnected.get()) {
                Authentication.invalidateToken();
                connectSingleFlight();
            }
        }, RECONNECT_DELAY_MS, TimeUnit.MILLISECONDS);
    }

    public void disconnect() {
        shouldReconnect.set(false);
        stopHeartbeat();
        CompletableFuture<Void> cf = connectFutureRef.getAndSet(null);
        if (webSocket != null && isConnected.get()) {
            try {
                webSocket.sendClose(java.net.http.WebSocket.NORMAL_CLOSURE, "Client disconnect").join();
            } catch (Throwable ignored) {}
            isConnected.set(false);
        }
    }

    public void sendMessage(String message) {
        java.net.http.WebSocket ws = this.webSocket;
        if (ws != null && isConnected.get() && !ws.isOutputClosed()) {
            System.out.println("Sending message: " + message);
            ws.sendText(message, true);
        } else {
            if (shouldReconnect.get()) handleConnectionLoss();
        }
    }

    public boolean isConnected() {
        boolean atomicConnected = isConnected.get();
        boolean wsValid = webSocket != null && !webSocket.isOutputClosed();
        return atomicConnected && wsValid;
    }

    public String getWebSocketUrl() {
        return wsUri != null ? wsUri.toString() : null;
    }

    private class WebSocketListener implements Listener {
        @Override
        public void onOpen(java.net.http.WebSocket webSocket) {
            isConnected.set(true);
            lastHeartbeatReceived = System.currentTimeMillis();
            Listener.super.onOpen(webSocket);
        }

        @Override
        public CompletionStage<?> onText(java.net.http.WebSocket webSocket, CharSequence data, boolean last) {
            try {
                JsonObject json = JsonParser.parseString(data.toString()).getAsJsonObject();
                if (json.has("type") && "heartbeat_response".equals(json.get("type").getAsString())) {
                    lastHeartbeatReceived = System.currentTimeMillis();
                } else {
                    processIncomingMessage(data.toString());
                }
            } catch (Exception e) {
                processIncomingMessage(data.toString());
            }

            webSocket.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onBinary(java.net.http.WebSocket webSocket, ByteBuffer data, boolean last) {
            webSocket.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onClose(java.net.http.WebSocket webSocket, int statusCode, String reason) {
            Authentication.WS_CONNECT_FUTURE.set(null);
            isConnected.set(false);
            connectFutureRef.set(null);
            stopHeartbeat();

            boolean authError = (statusCode == 1008) || (reason != null && (
                    reason.contains("Invalid authentication") ||
                            reason.contains("Authentication failed") ||
                            reason.contains("Unauthorized") ||
                            reason.contains("Token not found") ||
                            reason.contains("Missing authentication token")
            ));

            boolean replaced = (reason != null && reason.contains("New connection established"));

            if (shouldReconnect.get() && statusCode != java.net.http.WebSocket.NORMAL_CLOSURE && !replaced) {
                if (authError) {
                    Authentication.invalidateToken();
                    scheduleReconnectWithAuth();
                } else {
                    scheduleReconnect();
                }
            }
            return Listener.super.onClose(webSocket, statusCode, reason);
        }

        @Override
        public void onError(java.net.http.WebSocket webSocket, Throwable error) {
            Authentication.WS_CONNECT_FUTURE.set(null);
            isConnected.set(false);
            connectFutureRef.set(null);
            stopHeartbeat();

            if (shouldReconnect.get()) {
                if (isAuthenticationError(error)) {
                    Authentication.invalidateToken();
                    scheduleReconnectWithAuth();
                } else {
                    scheduleReconnect();
                }
            }
            Listener.super.onError(webSocket, error);
        }
    }

    private void processIncomingMessage(String jsonMessage) {
        try {
            JsonObject json = JsonParser.parseString(jsonMessage).getAsJsonObject();
            System.out.println("Received message: " + jsonMessage);

            if (json.has("type") && "discord_chat_message".equals(json.get("type").getAsString())) {
                JsonObject data = json.getAsJsonObject("data");

                if (data != null && data.has("username") && data.has("message") && data.has("rank")) {
                    String username = data.get("username").getAsString();
                    String content = data.get("message").getAsString();
                    String rank = data.get("rank").getAsString();

                    SocketMessageHandler.messageToClient(username, rank, content);
                }
            } else if (json.has("type") && "chat_announcement".equals(json.get("type").getAsString())) {
                JsonObject data = json.getAsJsonObject("data");
                if (data != null && data.has("guild_alert") && data.has("message")) {
                    Boolean guildAlert = data.get("guild_alert").getAsBoolean();
                    String content = data.get("message").getAsString();

                    SocketMessageHandler.announceToClient(guildAlert, content);
                }
            } else if (json.has("type") && "rank_promotion_request".equals(json.get("type").getAsString())) {
                RankPromotionHandler.handleRankPromotionRequest(json);
            }
        } catch (Exception e) {
            System.err.println("Error processing incoming message: " + e.getMessage());
        }
    }

    public void sendChatMessage(String username, String content) {
        Gson gson = new Gson();

        MessagePacket messagePacket = new MessagePacket(username, content);
        String jsonMessage = gson.toJson(messagePacket);

        java.net.http.WebSocket ws = this.webSocket;
        if (ws != null && isConnected.get() && !ws.isOutputClosed()) {
            ws.sendText(jsonMessage, true);
        } else {
            if (shouldReconnect.get()) handleConnectionLoss();
        }
    }

    public ScheduledExecutorService getScheduler() {
        return scheduler;
    }

    public static class MessagePacket {
        public String type;
        public Data data;

        public MessagePacket(String username, String content) {
            this.type = "chat_message";
            this.data = new Data(username, content);
        }

        public static class Data {
            public String username;
            public String message;

            public Data(String username, String message) {
                this.username = username;
                this.message = message;
            }
        }
    }
}
