package dev.wiji.tbgm.enums;

import java.util.Optional;

public enum Rank {
    OWNER("owner",
            "\ue060\udaff\udfff\ue03e\udaff\udfff\ue046\udaff\udfff\ue03d\udaff\udfff\ue034\udaff\udfff\ue041\udaff\udfff\ue062\udaff\udfe0",
            "\uE00E\uE016\uE00D\uE004\uE011\uDB00\uDC02",
            0xff2121,
            0xff3636, 6),

    CHIEF("chief",
            "\ue060\udaff\udfff\ue032\udaff\udfff\ue037\udaff\udfff\ue038\udaff\udfff\ue034\udaff\udfff\ue035\udaff\udfff\ue062\udaff\udfe2",
            "\uE002\uE007\uE008\uE004\uE005\uDB00\uDC02",
            0xff2121,
            0xff3636, 5),

    STRATEGIST("strategist",
            "\ue060\udaff\udfff\ue042\udaff\udfff\ue043\udaff\udfff\ue041\udaff\udfff\ue030\udaff\udfff\ue043\udaff\udfff\ue034\udaff\udfff\ue036\udaff\udfff\ue038\udaff\udfff\ue042\udaff\udfff\ue043\udaff\udfff\ue062\udaff\udfc4",
            "\uE012\uE013\uE011\uE000\uE013\uE004\uE006\uE008\uE012\uE013\uDB00\uDC02",
            0xff960d,
            0xffa836, 4),

    CAPTAIN("captain",
            "\ue060\udaff\udfff\ue032\udaff\udfff\ue030\udaff\udfff\ue03f\udaff\udfff\ue043\udaff\udfff\ue030\udaff\udfff\ue038\udaff\udfff\ue03d\udaff\udfff\ue062\udaff\udfd6",
            "\uE002\uE000\uE00F\uE013\uE000\uE008\uE00D\uDB00\uDC02",
            0xffe30f,
            0xffea4d, 3),

    RECRUITER("recruiter",
            "\ue060\udaff\udfff\ue041\udaff\udfff\ue034\udaff\udfff\ue032\udaff\udfff\ue041\udaff\udfff\ue044\udaff\udfff\ue038\udaff\udfff\ue043\udaff\udfff\ue034\udaff\udfff\ue041\udaff\udfff\ue062\udaff\udfca",
            "\uE011\uE004\uE002\uE011\uE014\uE008\uE013\uE004\uE011\uDB00\uDC02",
            0x0ac956,
            0x4EF28F, 2),

    RECRUIT("recruit",
            "\ue060\udaff\udfff\ue041\udaff\udfff\ue034\udaff\udfff\ue032\udaff\udfff\ue041\udaff\udfff\ue044\udaff\udfff\ue038\udaff\udfff\ue043\udaff\udfff\ue062\udaff\udfd6",
            "\uE011\uE004\uE002\uE011\uE014\uE008\uE013\uDB00\uDC02",
            0x34EBD8,
            0x79F2E6, 1),

    OFFICER("officer",
            "\uE060\uDAFF\uDFFF\uE03E\uDAFF\uDFFF\uE035\uDAFF\uDFFF\uE035\uDAFF\uDFFF\uE038\uDAFF\uDFFF\uE032\uDAFF\uDFFF\uE034\uDAFF\uDFFF\uE041\uDAFF\uDFFF\uE062\uDAFF\uDFD6",
            "\uE00E\uE005\uE005\uE008\uE002\uE004\uE011\uDB00\uDC02",
            0xff82f5,
            0xfcacf6, -1),

    ADVISOR("advisor",
            "\uE060\uDAFF\uDFFF\uE030\uDAFF\uDFFF\uE033\uDAFF\uDFFF\uE045\uDAFF\uDFFF\uE038\uDAFF\uDFFF\uE042\uDAFF\uDFFF\uE03E\uDAFF\uDFFF\uE041\uDAFF\uDFFF\uE062\uDAFF\uDFD6",
            "\uE000\uE003\uE015\uE008\uE012\uE00E\uE011\uDB00\uDC02",
            0x9f73ff,
            0xb694ff, -1),

    COUNCIL("council",
            "\uE060\uDAFF\uDFFF\uE032\uDAFF\uDFFF\uE03E\uDAFF\uDFFF\uE044\uDAFF\uDFFF\uE03D\uDAFF\uDFFF\uE032\uDAFF\uDFFF\uE038\uDAFF\uDFFF\uE03B\uDAFF\uDFFF\uE062\uDAFF\uDFD6",
            "\uE002\uE00E\uE014\uE00D\uE002\uE008\uE00B\uDB00\uDC02",
            0xff2121,
            0xff3636, -1),

    TBGM("tbgm",
            "\uE060\uDAFF\uDFFF\uE043\uDAFF\uDFFF\uE031\uDAFF\uDFFF\uE036\uDAFF\uDFFF\uE03C\uDAFF\uDFFF\uE062\udaff\udfef\udaff\udff7",
            "\uE013\uE001\uE006\uE00C\uDB00\uDC02",
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
}