package dev.wiji.tbgm.raid;

import com.wynntils.core.components.Models;
import com.wynntils.models.raid.RaidModel;
import com.wynntils.models.raid.type.RaidInfo;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class RaidTracker {
    private static RaidTracker instance;
    private String currentRaidName = null;
    private String lastRaidName = null;

    private RaidTracker() {}

    public static RaidTracker getInstance() {
        if (instance == null) {
            instance = new RaidTracker();
        }
        return instance;
    }

    public static void initialize() {
        RaidTracker tracker = getInstance();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null) {
                tracker.updateRaidState();
            }
        });
    }

    private void updateRaidState() {
        try {
            RaidModel raidModel = Models.Raid;
            RaidInfo currentRaid = raidModel.getCurrentRaid();

            String newRaidName = null;
            if (currentRaid != null && currentRaid.getRaidKind() != null) {
                newRaidName = currentRaid.getRaidKind().getRaidName();
            }

            currentRaidName = newRaidName;

            if (currentRaidName != null && !currentRaidName.equals(lastRaidName)) {
                lastRaidName = currentRaidName;
            }

        } catch (Exception e) {
            System.err.println("Error updating raid state: " + e.getMessage());
        }
    }

    public String getCurrentRaid() {
        return currentRaidName;
    }

    public String getLastRaid() {
        return lastRaidName;
    }
}