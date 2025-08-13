package dev.wiji.wynntracker.controllers;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket.Listener;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CompletableFuture;

public class WebSocket {
    private java.net.http.WebSocket webSocket;
    private final HttpClient httpClient;
    private URI wsUri;
    private boolean isConnected = false;
    private boolean shouldReconnect = true;
    private int reconnectAttempts = 0;
    private static final int MAX_RECONNECT_ATTEMPTS = 5;
    private static final int RECONNECT_DELAY_MS = 5000;

    public WebSocket() {
        this.httpClient = HttpClient.newHttpClient();
    }

    public String convertHttpToWebSocketUrl(String httpUrl) {
        if (httpUrl == null || httpUrl.isEmpty()) {
            return null;
        }
        
        String wsUrl = httpUrl;
        if (wsUrl.startsWith("http://")) wsUrl = wsUrl.replace("http://", "ws://");
        else if (wsUrl.startsWith("https://")) wsUrl = wsUrl.replace("https://", "wss://");
        else if (!wsUrl.startsWith("ws://") && !wsUrl.startsWith("wss://")) wsUrl = "ws://" + wsUrl;
        
        wsUrl += "/ws";
        
        return wsUrl;
    }

    public CompletableFuture<Void> connect() {
        String httpUrl = Config.getConfigData().apiUrl;
        String wsUrl = convertHttpToWebSocketUrl(httpUrl);
        
        if (wsUrl == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Invalid URL"));
        }
        
        String authToken = Authentication.token;
        if (authToken == null || authToken.isEmpty()) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("No auth token available"));
        }
        
        try {
            this.wsUri = URI.create(wsUrl);
            return httpClient.newWebSocketBuilder()
                .header("token", authToken)
                .buildAsync(wsUri, new WebSocketListener())
                .thenAccept(ws -> {
                    this.webSocket = ws;
                    this.isConnected = true;
                    System.out.println("WebSocket connected with auth token");
                })
                .exceptionally(throwable -> {
                    System.err.println("Failed to connect to WebSocket: " + throwable.getMessage());
                    this.isConnected = false;
                    return null;
                });
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    public CompletableFuture<Void> connectWithRetry() {
        shouldReconnect = true;
        reconnectAttempts = 0;
        return attemptConnection();
    }

    private CompletableFuture<Void> attemptConnection() {
        return connect()
            .exceptionally(throwable -> {
                System.err.println("WebSocket connection attempt failed: " + throwable.getMessage());
                if (shouldReconnect && reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
                    scheduleReconnect();
                } else {
                    System.err.println("Max reconnection attempts reached or reconnection disabled");
                }
                return null;
            });
    }

    private void scheduleReconnect() {
        reconnectAttempts++;
        System.out.println("Scheduling WebSocket reconnection attempt " + reconnectAttempts + "/" + MAX_RECONNECT_ATTEMPTS + " in " + RECONNECT_DELAY_MS + "ms");
        
        new Thread(() -> {
            try {
                Thread.sleep(RECONNECT_DELAY_MS);
                if (shouldReconnect && !isConnected) {
                    attemptConnection();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    public void disconnect() {
        shouldReconnect = false;
        if (webSocket != null && isConnected) {
            webSocket.sendClose(java.net.http.WebSocket.NORMAL_CLOSURE, "Client disconnect");
            isConnected = false;
        }
    }

    public void sendMessage(String message) {
        if (webSocket != null && isConnected) {
            webSocket.sendText(message, true);
        } else {
            System.err.println("WebSocket is not connected. Cannot send message: " + message);
        }
    }

    public boolean isConnected() {
        return isConnected;
    }

    public String getWebSocketUrl() {
        return wsUri != null ? wsUri.toString() : null;
    }

    private class WebSocketListener implements Listener {
        @Override
        public void onOpen(java.net.http.WebSocket webSocket) {
            System.out.println("WebSocket connection opened to: " + wsUri);
            reconnectAttempts = 0; // Reset reconnection attempts on successful connection
            System.out.println("WebSocket connection successful");
            Listener.super.onOpen(webSocket);
        }

        @Override
        public CompletionStage<?> onText(java.net.http.WebSocket webSocket, CharSequence data, boolean last) {
            System.out.println("Received WebSocket message: " + data);
            
            // Process Discord chat messages
            processIncomingMessage(data.toString());
            
            return Listener.super.onText(webSocket, data, last);
        }

        @Override
        public CompletionStage<?> onBinary(java.net.http.WebSocket webSocket, ByteBuffer data, boolean last) {
            System.out.println("Received WebSocket binary data");
            return Listener.super.onBinary(webSocket, data, last);
        }

        @Override
        public CompletionStage<?> onClose(java.net.http.WebSocket webSocket, int statusCode, String reason) {
            System.out.println("WebSocket connection closed: " + statusCode + " - " + reason);
            isConnected = false;
            
            // Trigger reconnection if it wasn't a normal client disconnect
            if (shouldReconnect && statusCode != java.net.http.WebSocket.NORMAL_CLOSURE) {
                System.out.println("WebSocket connection lost, attempting to reconnect...");
                scheduleReconnect();
            }
            
            return Listener.super.onClose(webSocket, statusCode, reason);
        }

        @Override
        public void onError(java.net.http.WebSocket webSocket, Throwable error) {
            System.err.println("WebSocket error: " + error.getMessage());
            isConnected = false;
            
            // Trigger reconnection on error
            if (shouldReconnect) {
                System.out.println("WebSocket error occurred, attempting to reconnect...");
                scheduleReconnect();
            }
            
            Listener.super.onError(webSocket, error);
        }
    }

    private void processIncomingMessage(String jsonMessage) {
        try {
            JsonObject json = JsonParser.parseString(jsonMessage).getAsJsonObject();

            // Check if this is a Discord chat message
            if (json.has("type") && "discord_chat_message".equals(json.get("type").getAsString())) {
                JsonObject data = json.getAsJsonObject("data");
                
                if (data != null && data.has("username") && data.has("message") && data.has("rank")) {
                    String username = data.get("username").getAsString();
                    String content = data.get("message").getAsString();
                    String rank = data.get("rank").getAsString();
                    
                    SocketMessageHandler.messageToClient(username, rank, content);
                }
            }
        } catch (Exception e) {
            // Log the error but don't crash - might be a different message type
            System.err.println("Error processing incoming message: " + e.getMessage());
        }
    }

    public void sendChatMessage(String username, String content) {
        Gson gson = new Gson();

        MessagePacket messagePacket = new MessagePacket(username, content);
        String jsonMessage = gson.toJson(messagePacket);

        if (webSocket != null && isConnected) {
            webSocket.sendText(jsonMessage, true);
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