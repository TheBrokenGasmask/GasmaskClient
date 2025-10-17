package dev.wiji.tbgm.enums;

import java.util.Optional;

public enum Rank {
    OWNER("owner",
            makeBackgroundText("owner"),
            makeForegroundText("owner"),
            0x00FF6D,
            0x32FF84, 6),

    CHIEF("chief",
            makeBackgroundText("chief"),
            makeForegroundText("chief"),
            0xF2201D,
            0xF24441, 5),

    STRATEGIST("strategist",
            makeBackgroundText("strategist"),
            makeForegroundText("strategist"),
            0xFF7B00,
            0xFF8B26, 4),

    CAPTAIN("captain",
            makeBackgroundText("captain"),
            makeForegroundText("captain"),
            0xFFD704,
            0xFFDB2B, 3),

    RECRUITER("recruiter",
            makeBackgroundText("recruiter"),
            makeForegroundText("recruiter"),
            0x00C24D,
            0x1DC15C, 2),

    RECRUIT("recruit",
            makeBackgroundText("recruit"),
            makeForegroundText("recruit"),
            0x2197FF,
            0x47A9FF, 1),

    OFFICER("officer",
            makeBackgroundText("officer"),
            makeForegroundText("officer"),
            0xFF42FB,
            0xFF6BFC, -1),
    COMMANDER("commander",
            makeBackgroundText("commander"),
            makeForegroundText("commander"),
            0x9D55D4,
            0xAD80CF, -1),
    ADVISOR("advisor",
            makeBackgroundText("advisor"),
            makeForegroundText("advisor"),
            0x2DD2D6,
            0x4ED1D3, -1),

    COUNCIL("council",
            makeBackgroundText("council"),
            makeForegroundText("council"),
            0x28E299,
            0x4AE0A4, -1),

    TBGM("tbgm",
            makeBackgroundText("tbgm"),
            makeForegroundText("tbgm"),
            0xf70a0e,
            0xfc3d40, -1);

    private final String rankPlainText;
    private final String backgroundText;
    private final String foregroundText;
    private final int rankColor;
    private final int nameColor;
    private final int rankInt;

    Rank(String displayName, String rankText, String foregroundText, int rankColor, int nameColor, int rankInt) {
        this.rankPlainText = displayName;
        this.backgroundText = rankText;
        this.foregroundText = foregroundText;
        this.rankColor = rankColor;
        this.nameColor = nameColor;
        this.rankInt = rankInt;
    }

    // Getters
    public String getRankPlainText() {
        return rankPlainText;
    }

    public String getBackgroundText() {
        return backgroundText;
    }

    public String getForegroundText() {
        return foregroundText;
    }

    public int getRankColor() {
        return rankColor;
    }

    public int getNameColor() {
        return nameColor;
    }

    public int getRankInt() {
        return rankInt;
    }

    // Utility methods
    public static Optional<Rank> fromString(String rankName) {
        if (rankName == null) {
            return Optional.empty();
        }

        String normalizedRankName = rankName.toLowerCase().trim();
        for (Rank rank : values()) {
            if (rank.rankPlainText.equals(normalizedRankName)) {
                return Optional.of(rank);
            }
        }
        return Optional.empty();
    }

    public static Optional<Rank> fromInt(int rankInt) {
        for (Rank rank : values()) {
            if (rank.rankInt == rankInt) {
                return Optional.of(rank);
            }
        }
        return Optional.empty();
    }

    public static boolean isValidRank(String rankName) {
        return fromString(rankName).isPresent();
    }

    @Override
    public String toString() {
        return rankPlainText;
    }

    public static String makeBackgroundText(String word) {
        StringBuilder background = new StringBuilder();

        // Pixel offset is 6 per character (by default), except for 'i' which is 4 pixels + 2 pixels base
        int pixelOffset = 2;

        for(char c : word.toCharArray()) {
            pixelOffset += (c == 'i' || c == 'I' ? 4 : 6);
        }

        // Ensure the minimum offset is used for single/short words to prevent errors
        if (pixelOffset < 10) {
            pixelOffset = 10;
        }

        // start cap \uE060\uDAFF\uDFFF
        background.append('\uE060').append('\uDAFF').append('\uDFFF');

        // 2. Append character codes
        for (char c : word.toCharArray()) {
            if (Character.isLetter(c)) {
                // Letter code \uE030 to \uE049 (for 'a' to 'z')
                int code = 0xE030 + (Character.toLowerCase(c) - 'a');
                background.append((char) code).append('\uDAFF').append('\uDFFF');
            } else {
                // fallback for non-letter chars: \uE03F + \uDAFF\uDFFF
                background.append('\uE03F').append('\uDAFF').append('\uDFFF');
            }
        }

        // end cap \uE062
        background.append('\uE062');

        // 3. Encode the pixel offset into the surrogate pair (B = 256 - P)
        int lowByteValue = 0x100 - (pixelOffset & 0xFF);

        char lowSurrogate = (char) (0xDF00 | lowByteValue);
        background.append('\uDAFF').append(lowSurrogate);

        return background.toString();
    }

    // Corrected makeForegroundText
    public static String makeForegroundText(String word) {
        StringBuilder foreground = new StringBuilder();

        for (char c : word.toCharArray()) {
            if (Character.isLetter(c)) {
                // Letter code \uE000 to \uE019 (for 'a' to 'z')
                int code = 0xE000 + (Character.toLowerCase(c) - 'a');
                foreground.append((char) code);
            } else {
                // fallback for non-letter chars: \uE00F
                foreground.append('\uE00F');
            }
        }

        // terminator marker \uDB00\uDC02 (a single character represented by a surrogate pair)
        foreground.append('\uDB00').append('\uDC02');
        return foreground.toString();
    }
}