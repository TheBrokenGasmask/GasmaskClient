package dev.wiji.wynntracker.enums;

public enum RankIcon {
        DEFAULT("\uE100", "Default"),
        RECRUIT("\uE101", "Recruit"),
        RECRUITER("\uE102", "Recruiter"),
        CAPTAIN("\uE103", "Captain"),
        OFFICER("\uE104", "Officer"),
        STRATEGIST("\uE105", "Strategist"),
        ADVISOR("\uE106", "Advisor"),
        CHIEF("\uE107", "Chief"),
        COUNCIL("\uE108", "Council");
        
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