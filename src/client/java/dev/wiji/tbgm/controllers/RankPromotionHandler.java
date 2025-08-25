package dev.wiji.tbgm.controllers;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.wiji.tbgm.enums.Rank;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import com.wynntils.core.components.Models;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RankPromotionHandler {

    private static final Pattern RANK_PROMOTION_PATTERN = Pattern.compile("(.+?) has set (.+?) guild rank from (.+?) to (.+)");

    private static final ConcurrentHashMap<String, String> pendingRankPromotions = new ConcurrentHashMap<>();
    private static final long RANK_PROMOTION_TIMEOUT_MS = 10000; // 10 seconds

    public static void handleChatMessage(Text message) {
        String plainText = message.getString();
        Matcher matcher = RANK_PROMOTION_PATTERN.matcher(plainText);

        //System.out.println("Processing chat message for rank promotion: " + plainText);

        if (matcher.find()) {
            System.out.println("Rank promotion detected in chat: " + plainText);
            String clientPlayerName = matcher.group(1);
            String playerName = matcher.group(2).toLowerCase();
            String oldRank = matcher.group(3);
            String newRank = matcher.group(4);

            sendRankPromotionSuccess(playerName, newRank);
        }
    }

    public static void handleRankPromotionRequest(JsonObject json) {
        JsonObject data = json.getAsJsonObject("data");
        if (data != null && data.has("targetUsername") && data.has("newRank")) {
            String targetUsername = data.get("targetUsername").getAsString().toLowerCase();
            int newRankInt = data.get("newRank").getAsInt();
            String requestId = data.get("requestId").getAsString();

            if (!Models.Guild.getGuildName().isEmpty()){
                Rank newRank = Rank.fromInt(newRankInt).orElse(Rank.RECRUIT);
                String rankName = newRank.getRankPlainText();
                if (isUserAlreadyPromoted(targetUsername, rankName, Models.Guild.getGuildName())) {

                    sendRankPromotionSuccess(targetUsername, rankName );
                    return;
                }
            }

            Rank.fromInt(newRankInt).ifPresent(newRank -> {
                pendingRankPromotions.put(targetUsername, requestId);
                Authentication.getWebSocketManager().getScheduler().schedule(() -> {
                    pendingRankPromotions.remove(targetUsername, requestId);
                }, RANK_PROMOTION_TIMEOUT_MS, TimeUnit.MILLISECONDS);

                try {
                    MinecraftClient.getInstance().execute(() -> {
                        MinecraftClient.getInstance().player.networkHandler.sendChatCommand("guild rank " + targetUsername + " " + newRank.getRankPlainText());
                    });
                } catch (Exception e) {
                    sendRankPromotionResponse(requestId, false, e.getMessage(), targetUsername, newRank);
                }
            });
        }
    }

    public static void sendRankPromotionSuccess(String playerName, String newRank) {
        System.out.println("Sending rank promotion success for " + playerName + " to " + newRank);
        String requestId = pendingRankPromotions.remove(playerName);
        if (requestId != null) {
            Rank.fromString(newRank).ifPresent(rank -> {
                sendRankPromotionResponse(requestId, true, null, playerName, rank);
            });
        } else {
            System.out.println("No pending rank promotion found for " + playerName);
        }
    }

    public static void sendRankPromotionResponse(String requestId, boolean success, String error, String targetUsername, Rank newRank) {
        Gson gson = new Gson();

        RankPromotionResponsePacket responsePacket = new RankPromotionResponsePacket(requestId, success, error, targetUsername, newRank);
        String jsonMessage = gson.toJson(responsePacket);

        System.out.println("Sending rank promotion response: " + jsonMessage);
        Authentication.getWebSocketManager().sendMessage(jsonMessage);
    }

    public static boolean isUserAlreadyPromoted(String username, String rank, String guild) {
        String encodedGuildName = URLEncoder.encode(guild, StandardCharsets.UTF_8);
        String wynnUrl = String.format("https://api.wynncraft.com/v3/guild/%s?identifier=username", encodedGuildName);
        try {
            URL url = URI.create(wynnUrl).toURL();

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            int responseCode = conn.getResponseCode();

            if (responseCode < 200){
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                JsonObject jsonResponse = JsonParser.parseString(response.toString()).getAsJsonObject();

                if (jsonResponse.has("members")) {
                    JsonObject members = jsonResponse.getAsJsonObject("members");

                    for (String memberRank : members.keySet()) {
                        if (memberRank.equalsIgnoreCase(rank)) {
                            JsonObject rankMembers = members.getAsJsonObject(memberRank);

                            for (String memberUsername : rankMembers.keySet()) {
                                if (memberUsername.equalsIgnoreCase(username)) {
                                    return true;
                                }
                            }
                            break;
                        }
                    }
                } else {
                    System.out.println("Wynncraft API Error: " + responseCode);
                    return false;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return false;
    }

    public static class RankPromotionResponsePacket {
        public String type;
        public Data data;

        public RankPromotionResponsePacket(String requestId, boolean success, String error, String targetUsername, Rank newRank) {
            this.type = "rank_promotion_response";
            this.data = new Data(requestId, success, error, targetUsername, newRank.getRankInt());
        }

        public static class Data {
            public String requestId;
            public boolean success;
            public String error;
            public String targetUsername;
            public int newRank;

            public Data(String requestId, boolean success, String error, String targetUsername, int newRank) {
                this.requestId = requestId;
                this.success = success;
                this.error = error;
                this.targetUsername = targetUsername;
                this.newRank = newRank;
            }
        }
    }
}
