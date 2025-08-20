package dev.wiji.tbgm.badge;

import com.wynntils.services.leaderboard.type.LeaderboardType;

public enum CustomBadge {
    
    TNA_TOP_1("TNA Top 1", 0xFFFFFF, LeaderboardType.TNA_COMPLETION, BaseType.GOLD),
    TNA_TOP_2("TNA Top 2", 0xFFFFFF, LeaderboardType.TNA_COMPLETION, BaseType.SILVER),
    TNA_TOP_3("TNA Top 3", 0xFFFFFF, LeaderboardType.TNA_COMPLETION, BaseType.BRONZE),

    TCC_TOP_1("TCC Top 1", 0xFFFFFF, LeaderboardType.TCC_COMPLETION, BaseType.GOLD),
    TCC_TOP_2("TCC Top 2", 0xFFFFFF, LeaderboardType.TCC_COMPLETION, BaseType.SILVER),
    TCC_TOP_3("TCC Top 3", 0xFFFFFF, LeaderboardType.TCC_COMPLETION, BaseType.BRONZE),

    NOL_TOP_1("NOL Top 1", 0xFFFFFF, LeaderboardType.NOL_COMPLETION, BaseType.GOLD),
    NOL_TOP_2("NOL Top 2", 0xFFFFFF, LeaderboardType.NOL_COMPLETION, BaseType.SILVER),
    NOL_TOP_3("NOL Top 3", 0xFFFFFF, LeaderboardType.NOL_COMPLETION, BaseType.BRONZE),

    NOTG_TOP_1("NOTG Top 1", 0xFFFFFF, LeaderboardType.NOG_COMPLETION, BaseType.GOLD),
    NOTG_TOP_2("NOTG Top 2", 0xFFFFFF, LeaderboardType.NOG_COMPLETION, BaseType.SILVER),
    NOTG_TOP_3("NOTG Top 3", 0xFFFFFF, LeaderboardType.NOG_COMPLETION, BaseType.BRONZE),

    ALL_TOP_1("All Top 1", 0xFFFFFF, LeaderboardType.TNA_SCORE, BaseType.GOLD),
    ALL_TOP_2("All Top 2", 0xFFFFFF, LeaderboardType.TCC_SCORE, BaseType.GOLD),
    ALL_TOP_3("All Top 3", 0xFFFFFF, LeaderboardType.NOL_SCORE, BaseType.GOLD)
    ;

    public enum BaseType {
        GOLD(0),  
        SILVER(1),
        BRONZE(2); 
        
        private final int rowIndex;
        
        BaseType(int rowIndex) {
            this.rowIndex = rowIndex;
        }
        
        public int getRowIndex() {
            return rowIndex;
        }
    }

    
    private final String displayName;
    private final int color;
    private final LeaderboardType iconType;
    private final BaseType baseType;
    
    CustomBadge(String displayName, int color, LeaderboardType iconType) {
        this(displayName, color, iconType, BaseType.GOLD); // Default to gold base for best color mixing
    }
    
    CustomBadge(String displayName, int color, LeaderboardType iconType, BaseType baseType) {
        this.displayName = displayName;
        this.color = color;
        this.iconType = iconType;
        this.baseType = baseType;
    }
    
    /**
     * Get the display name of this badge
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Get the color of this badge as a hex integer
     */
    public int getColor() {
        return color;
    }
    
    /**
     * Get the color as a hex string (for display/debugging)
     */
    public String getColorHex() {
        return String.format("0x%06X", color);
    }
    
    /**
     * Get the icon type (determines badge texture coordinates)
     */
    public LeaderboardType getIconType() {
        return iconType;
    }
    
    /**
     * Get the base type for this badge (gold, silver, bronze)
     */
    public BaseType getBaseType() {
        return baseType;
    }
    
    /**
     * Get the U offset for the badge texture
     */
    public int getUOffset() {
        return iconType.ordinal() * 19; // Standard badge width spacing
    }
    
    /**
     * Get the V offset for the badge texture (selects gold/silver/bronze row)
     */
    public int getVOffset() {
        return baseType.getRowIndex() * 18; // Use proper row height spacing (same as width)
    }
    
    /**
     * Create a BadgeManager.ColoredBadge from this enum
     */
    public BadgeManager.ColoredBadge createColoredBadge() {
        return new BadgeManager.ColoredBadge(getUOffset(), getVOffset(), displayName, color, baseType);
    }

    
    @Override
    public String toString() {
        return displayName + " (" + getColorHex() + ")";
    }
}