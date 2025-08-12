package dev.wiji.wynntracker.badge;

import com.wynntils.core.components.Services;
import com.wynntils.services.leaderboard.LeaderboardService;
import com.wynntils.services.leaderboard.type.LeaderboardBadge;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BadgeManager {
    
    // Core system fields
    private static Field leaderboardField;
    private static final Map<UUID, List<ColoredBadge>> coloredBadges = new ConcurrentHashMap<>();
    private static final Map<String, Integer> badgeColorMap = new ConcurrentHashMap<>();
    private static boolean initialized = false;
    
    static {
        try {
            // Access leaderboard field using Yarn mappings
            leaderboardField = LeaderboardService.class.getDeclaredField("leaderboard");
            leaderboardField.setAccessible(true);
            initialized = true;
        } catch (Exception e) {
            System.err.println("Failed to initialize BadgeManager: " + e.getMessage());
        }
    }
    
    /**
     * Colored badge data structure
     */
    public static class ColoredBadge {
        private final LeaderboardBadge badge;
        private final String name;
        private final int color;
        private final CustomBadge.BaseType baseType;
        
        public ColoredBadge(int uOffset, int vOffset, String name, int color, CustomBadge.BaseType baseType) {
            this.badge = new LeaderboardBadge(uOffset, vOffset);
            this.name = name;
            this.color = color;
            this.baseType = baseType;
        }
        
        // Getters
        public LeaderboardBadge getBadge() { return badge; }
        public String getName() { return name; }
        public int getColor() { return color; }
        public CustomBadge.BaseType getBaseType() { return baseType; }
        public int uOffset() { return badge.uOffset(); }
        public int vOffset() { return badge.vOffset(); }
        
        // Color component extraction for OpenGL (0-1 range)
        public float getRed() { return ((color >> 16) & 0xFF) / 255.0f; }
        public float getGreen() { return ((color >> 8) & 0xFF) / 255.0f; }
        public float getBlue() { return (color & 0xFF) / 255.0f; }
    }
    
    // === CORE SYSTEM METHODS ===
    
    /**
     * Add a colored badge to a player (internal method)
     */
    private static void addColoredBadgeInternal(UUID playerUuid, ColoredBadge badge) {
        if (!initialized) {
            System.err.println("BadgeManager not initialized!");
            return;
        }
        
        coloredBadges.computeIfAbsent(playerUuid, k -> new ArrayList<>()).add(badge);
        
        // Store color mapping for quick lookup
        String key = getBadgeKey(playerUuid, badge.uOffset(), badge.vOffset());
        badgeColorMap.put(key, badge.getColor());
        
        injectBadges();
    }
    
    /**
     * Create unique badge key
     */
    private static String getBadgeKey(UUID playerUuid, int uOffset, int vOffset) {
        return playerUuid.toString() + ":" + uOffset + ":" + vOffset;
    }
    
    /**
     * Inject badges into the leaderboard system using reflection
     */
    @SuppressWarnings("unchecked")
    private static void injectBadges() {
        try {
            Map<UUID, List<LeaderboardBadge>> currentMap = 
                (Map<UUID, List<LeaderboardBadge>>) leaderboardField.get(Services.Leaderboard);
            
            Map<UUID, List<LeaderboardBadge>> newMap = new HashMap<>(currentMap);
            
            for (Map.Entry<UUID, List<ColoredBadge>> entry : coloredBadges.entrySet()) {
                UUID playerId = entry.getKey();
                List<ColoredBadge> playerBadges = entry.getValue();
                
                // Only use our colored badges, don't combine with existing ones to avoid duplicates
                List<LeaderboardBadge> newBadges = new ArrayList<>();
                
                for (ColoredBadge coloredBadge : playerBadges) {
                    // Use the badge's specified base texture (gold/silver/bronze row)
                    // The actual color will be applied through the color mapping system
                    LeaderboardBadge modifiedBadge = new LeaderboardBadge(
                        coloredBadge.uOffset(), 
                        coloredBadge.vOffset()
                    );
                    newBadges.add(modifiedBadge);
                }
                
                newMap.put(playerId, newBadges);
            }
            
            leaderboardField.set(Services.Leaderboard, newMap);
            
        } catch (Exception e) {
            System.err.println("Failed to inject colored badges: " + e.getMessage());
        }
    }
    
    /**
     * Apply a single badge to a player
     */
    public static void applyBadge(UUID playerUuid, CustomBadge badge) {
        addColoredBadgeInternal(playerUuid, badge.createColoredBadge());
    }
    
    /**
     * Apply multiple badges to a player
     */
    public static void applyBadges(UUID playerUuid, CustomBadge... badges) {
        for (CustomBadge badge : badges) {
            addColoredBadgeInternal(playerUuid, badge.createColoredBadge());
        }
    }
    
    /**
     * Apply multiple badges to a player from a list
     */
    public static void applyBadges(UUID playerUuid, List<CustomBadge> badges) {
        applyBadges(playerUuid, badges.toArray(new CustomBadge[0]));
    }
    
    /**
     * Replace all badges for a player (clears existing first)
     */
    public static void replaceBadges(UUID playerUuid, CustomBadge... badges) {
        clearAllBadges(playerUuid);
        applyBadges(playerUuid, badges);
    }
    
    /**
     * Remove all badges from a player
     */
    public static void clearAllBadges(UUID playerUuid) {
        List<ColoredBadge> removed = coloredBadges.remove(playerUuid);
        if (removed != null) {
            // Remove color mappings
            for (ColoredBadge badge : removed) {
                badgeColorMap.remove(getBadgeKey(playerUuid, badge.uOffset(), badge.vOffset()));
            }
        }
        injectBadges();
    }

    /**
     * Get the current badges for a player
     */
    public static List<ColoredBadge> getPlayerBadges(UUID playerUuid) {
        return coloredBadges.getOrDefault(playerUuid, new ArrayList<>());
    }

    /**
     * Get the color for a specific badge position (used by mixin)
     */
    public static int getBadgeColor(UUID playerUuid, int uOffset, int vOffset) {
        return badgeColorMap.getOrDefault(getBadgeKey(playerUuid, uOffset, vOffset), 0);
    }

    /**
     * Check if the system is ready
     */
    public static boolean isInitialized() {
        return initialized;
    }
}