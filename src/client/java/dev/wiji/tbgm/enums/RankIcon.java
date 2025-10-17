package dev.wiji.tbgm.enums;

public enum RankIcon {
        DEFAULT("\uE100", "Default"),
        RECRUIT("\uE101", "Recruit"),
        RECRUITER("\uE102", "Recruiter"),
        CAPTAIN("\uE103", "Captain"),
        COMMANDER("\uE104", "Commander"),
        OFFICER("\uE105", "Officer"),
        STRATEGIST("\uE106", "Strategist"),
        ADVISOR("\uE107", "Advisor"),
        CHIEF("\uE108", "Chief"),
        COUNCIL("\uE109", "Council"),
        OWNER("\uE110", "Owner");
        
        private final String character;
        private final String displayName;
        
        RankIcon(String character, String displayName) {
            this.character = character;
            this.displayName = displayName;
        }
        
        public String getCharacter() {
            return character;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public static RankIcon fromName(String name) {
            for (RankIcon icon : values()) {
                if (icon.name().equalsIgnoreCase(name) || icon.displayName.equalsIgnoreCase(name)) {
                    return icon;
                }
            }
            return RankIcon.DEFAULT;
        }
    }