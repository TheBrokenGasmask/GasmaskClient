package dev.wiji.wynntracker.controllers;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.wiji.wynntracker.WynnTrackerClient;
import dev.wiji.wynntracker.badge.BadgeManager;
import dev.wiji.wynntracker.badge.CustomBadge;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PlayerManager {
    
    private static final Map<UUID, PlayerInfo> playerInfoMap = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static boolean autoFetchEnabled = false;
    private static final int FETCH_INTERVAL_MINUTES = 5;
    
    /**
     * Player information structure for local storage
     */
    public static class PlayerInfo {
        private final UUID uuid;
        private final String username;
        private final String guild;
        private final String rank; // Optional guild rank
        private final int needsAspects;
        private final List<CustomBadge> badges;
        private final boolean hasDiscordLink;
        private final long lastUpdated;
        
        public PlayerInfo(UUID uuid, String username, String guild, String rank, 
                         int needsAspects, List<CustomBadge> badges, boolean hasDiscordLink) {
            this.uuid = uuid;
            this.username = username;
            this.guild = guild;
            this.rank = rank;
            this.needsAspects = needsAspects;
            this.badges = badges != null ? new ArrayList<>(badges) : new ArrayList<>();
            this.hasDiscordLink = hasDiscordLink;
            this.lastUpdated = System.currentTimeMillis();
        }
        
        // Getters
        public UUID getUuid() { return uuid; }
        public String getUsername() { return username; }
        public String getGuild() { return guild; }
        public String getRank() { return rank; }
        public int getNeedsAspects() { return needsAspects; }
        public List<CustomBadge> getBadges() { return new ArrayList<>(badges); }
        public boolean hasDiscordLink() { return hasDiscordLink; }
        public long getLastUpdated() { return lastUpdated; }
    }
    
    /**
     * Start automatic fetching of player data
     */
    public static void startAutoFetch() {
        if (autoFetchEnabled) {
            System.out.println("Auto-fetch already enabled");
            return;
        }
        
        autoFetchEnabled = true;
        System.out.println("Starting automatic player data fetching every " + FETCH_INTERVAL_MINUTES + " minutes");
        
        // Initial fetch
        fetchPlayerData();
        
        // Schedule periodic fetching
        scheduler.scheduleAtFixedRate(() -> {
            try {
                fetchPlayerData();
            } catch (Exception e) {
                System.err.println("Error during scheduled player data fetch: " + e.getMessage());
            }
        }, FETCH_INTERVAL_MINUTES, FETCH_INTERVAL_MINUTES, TimeUnit.MINUTES);
    }
    
    /**
     * Stop automatic fetching
     */
    public static void stopAutoFetch() {
        if (!autoFetchEnabled) {
            System.out.println("Auto-fetch not currently enabled");
            return;
        }
        
        autoFetchEnabled = false;
        scheduler.shutdownNow();
        System.out.println("Stopped automatic player data fetching");
    }
    
    /**
     * Check if auto-fetch is enabled
     */
    public static boolean isAutoFetchEnabled() {
        return autoFetchEnabled;
    }
    
    /**
     * Fetch player data from the server
     */
    public static void fetchPlayerData() {
        new Thread(() -> {
            try {
                StringBuilder urlBuilder = new StringBuilder();
                String baseUrl = WynnTrackerClient.config_data.apiUrl;
                if(baseUrl.endsWith("/")) baseUrl = baseUrl.substring(0, baseUrl.length() - 1);

                urlBuilder.append(baseUrl).append("/api/players");
                
                URL url = URI.create(urlBuilder.toString()).toURL();
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                int responseCode = conn.getResponseCode();
                if (responseCode < 200 || responseCode > 299) {
                    System.err.println("Failed to fetch player data: HTTP " + responseCode);
                    return;
                }

                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                conn.disconnect();

                // Parse the response
                parseAndStorePlayerData(response.toString());
                System.out.println("Successfully updated player data for " + playerInfoMap.size() + " players");
                
            } catch (Exception e) {
                System.err.println("Failed to fetch player data: " + e.getMessage());
            }
        }).start();
    }
    
    /**
     * Parse and store player data from JSON
     */
    public static void parseAndStorePlayerData(String jsonData) {
        try {
            Gson gson = new Gson();
            JsonArray playersArray = gson.fromJson(jsonData, JsonArray.class);
            
            int updatedCount = 0;
            for (JsonElement element : playersArray) {
                JsonObject playerObj = element.getAsJsonObject();
                PlayerInfo playerInfo = parsePlayerObject(playerObj);
                if (playerInfo != null) {
                    updatedCount++;
                }
            }
            
            System.out.println("Parsed and stored " + updatedCount + " player records");
        } catch (Exception e) {
            System.err.println("Failed to parse player data: " + e.getMessage());
        }
    }
    
    /**
     * Parse a single player object from JSON and store it
     */
    public static PlayerInfo parsePlayerObject(JsonObject playerObj) {
        try {
            UUID uuid = UUID.fromString(playerObj.get("uuid").getAsString());
            String username = playerObj.get("username").getAsString();
            String guild = playerObj.get("guild").getAsString();
            
            // Handle optional rank field
            String rank = null;
            if (playerObj.has("rank") && !playerObj.get("rank").isJsonNull()) {
                rank = playerObj.get("rank").getAsString();
            }
            
            int needsAspects = playerObj.get("needs_aspects").getAsInt();
            boolean hasDiscordLink = playerObj.get("has_discord_link").getAsBoolean();
            
            // Parse badges array
            List<CustomBadge> badges = new ArrayList<>();
            JsonArray badgesArray = playerObj.getAsJsonArray("badges");
            if (badgesArray != null) {
                for (JsonElement badgeElement : badgesArray) {
                    JsonObject badgeObj = badgeElement.getAsJsonObject();
                    String badgeId = badgeObj.get("id").getAsString();
                    
                    // Convert badge ID to CustomBadge enum
                    try {
                        CustomBadge badge = CustomBadge.valueOf(badgeId);
                        badges.add(badge);
                    } catch (IllegalArgumentException e) {
                        System.err.println("Unknown badge ID: " + badgeId);
                    }
                }
            }
            
            // Create and store player info
            PlayerInfo playerInfo = new PlayerInfo(uuid, username, guild, rank, 
                                                  needsAspects, badges, hasDiscordLink);
            playerInfoMap.put(uuid, playerInfo);
            
            // Apply badges to the player via BadgeManager
            if (!badges.isEmpty()) {
                BadgeManager.replaceBadges(uuid, badges);
            } else {
                // Clear badges if player has none
                BadgeManager.clearAllBadges(uuid);
            }
            
            return playerInfo;
        } catch (Exception e) {
            System.err.println("Failed to parse player object: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Get stored player info by UUID
     */
    public static PlayerInfo getPlayerInfo(UUID uuid) {
        return playerInfoMap.get(uuid);
    }
    
    /**
     * Get stored player info by username
     */
    public static PlayerInfo getPlayerInfo(String username) {
        return playerInfoMap.values().stream()
                .filter(info -> info.getUsername().equalsIgnoreCase(username))
                .findFirst()
                .orElse(null);
    }
}