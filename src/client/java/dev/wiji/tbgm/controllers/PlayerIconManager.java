package dev.wiji.tbgm.controllers;
import dev.wiji.tbgm.enums.RankIcon;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerIconManager {
    private static final Map<UUID, RankIcon> playerIcons = new HashMap<>();

    public static void setPlayerIcon(UUID playerUuid, RankIcon icon) {
        playerIcons.put(playerUuid, icon);
    }

    public static void removePlayerIcon(UUID playerUuid) {
        playerIcons.remove(playerUuid);
    }

    public static RankIcon getPlayerIcon(UUID playerUuid) {
        return playerIcons.getOrDefault(playerUuid, RankIcon.DEFAULT);
    }
}