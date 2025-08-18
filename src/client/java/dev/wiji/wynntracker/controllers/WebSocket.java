package dev.wiji.wynntracker.controllers;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.wiji.wynntracker.WynnTrackerClient;

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
    private static final int MAX_RECONNECT_ATTEMPTS = 5;
    private static final int RECONNECT_DELAY_MS = 5000;
    private volatile int reconnectAttempts = 0;

    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "Wynntracker-WebSocket");
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
            if (existing != null && !existing.isDone()) return existing;
            if (existing != null && existing.isDone() && !existing.isCompletedExceptionally()) return existing;

            CompletableFuture<Void> cf = new CompletableFuture<>();
            if (connectFutureRef.compareAndSet(existing, cf)) {
                String httpUrl = WynnTrackerClient.getApiUrl();
                String wsUrl = convertHttpToWebSocketUrl(httpUrl);

                if (wsUrl == null) {
                    connectFutureRef.compareAndSet(cf, null);
                    cf.completeExceptionally(new IllegalArgumentException("Invalid URL"));
                    return cf;
                }

                String authToken = Authentication.token;
                if (authToken == null || authToken.isEmpty()) {
                    connectFutureRef.compareAndSet(cf, null);
                    cf.completeExceptionally(new IllegalArgumentException("No auth token available"));
                    return cf;
                }

                try {
                    this.wsUri = URI.create(wsUrl);
                    httpClient.newWebSocketBuilder()
                            .header("token", authToken)
                            .buildAsync(wsUri, new WebSocketListener())
                            .whenComplete((ws, ex) -> {
                                if (ex != null) {
                                    connectFutureRef.compareAndSet(cf, null);
                                    isConnected.set(false);
                                    System.err.println("Failed to connect to WebSocket: " + ex.getMessage());
                                    cf.completeExceptionally(ex);
                                } else {
                                    this.webSocket = ws;
                                    isConnected.set(true);
                                    reconnectAttempts = 0;
                                    System.out.println("WebSocket connected with auth token");
                                    cf.complete(null);
                                }
                            });
                } catch (Exception e) {
                    connectFutureRef.compareAndSet(cf, null);
                    cf.completeExceptionally(e);
                }
                return cf;
            }
        }
    }

    private void scheduleReconnect() {
        if (!shouldReconnect.get()) return;
        if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            System.err.println("Max reconnection attempts reached or reconnection disabled");
            return;
        }
        if (!reconnectScheduled.compareAndSet(false, true)) return;

        reconnectAttempts++;
        System.out.println("Scheduling WebSocket reconnection attempt " + reconnectAttempts + "/" + MAX_RECONNECT_ATTEMPTS + " in " + RECONNECT_DELAY_MS + "ms");

        scheduler.schedule(() -> {
            reconnectScheduled.set(false);
            if (shouldReconnect.get() && !isConnected.get()) {
                connectSingleFlight();
            }
        }, RECONNECT_DELAY_MS, TimeUnit.MILLISECONDS);
    }

    public void disconnect() {
        shouldReconnect.set(false);
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
            ws.sendText(message, true);
        } else {
            System.err.println("WebSocket is not connected. Cannot send message: " + message);
        }
    }

    public boolean isConnected() {
        return isConnected.get();
    }

    public String getWebSocketUrl() {
        return wsUri != null ? wsUri.toString() : null;
    }

    private class WebSocketListener implements Listener {
        @Override
        public void onOpen(java.net.http.WebSocket webSocket) {
            System.out.println("WebSocket connection opened to: " + wsUri);
            System.out.println("WebSocket connection successful");
            Listener.super.onOpen(webSocket);
        }

        @Override
        public CompletionStage<?> onText(java.net.http.WebSocket webSocket, CharSequence data, boolean last) {
            System.out.println("Received WebSocket message: " + data);
            processIncomingMessage(data.toString());
            webSocket.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onBinary(java.net.http.WebSocket webSocket, ByteBuffer data, boolean last) {
            System.out.println("Received WebSocket binary data");
            webSocket.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onClose(java.net.http.WebSocket webSocket, int statusCode, String reason) {
            System.out.println("WebSocket connection closed: " + statusCode + " - " + reason);
            isConnected.set(false);

            // Trigger reconnection if it wasn't a normal client disconnect
            boolean replaced = (reason != null && reason.contains("New connection established"));
            if (shouldReconnect.get() && statusCode != java.net.http.WebSocket.NORMAL_CLOSURE && !replaced) {
                System.out.println("WebSocket connection lost, attempting to reconnect...");
                scheduleReconnect();
            }
            return Listener.super.onClose(webSocket, statusCode, reason);
        }

        @Override
        public void onError(java.net.http.WebSocket webSocket, Throwable error) {
            System.err.println("WebSocket error: " + error.getMessage());
            isConnected.set(false);
            if (shouldReconnect.get()) {
                System.out.println("WebSocket error occurred, attempting to reconnect...");
                scheduleReconnect();
            }
            Listener.super.onError(webSocket, error);
        }
    }

    private void processIncomingMessage(String jsonMessage) {
        try {
            JsonObject json = JsonParser.parseString(jsonMessage).getAsJsonObject();

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
            System.out.println("Sent chat message: " + jsonMessage);
        } else {
            System.err.println("WebSocket is not connected. Cannot send chat message: " + jsonMessage);
        }
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
