package dev.wiji.wynntracker.controllers;

import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GuildChatModifier {
    public static final Map<String, String> RANK = Map.of(
            "owner", "\ue060\udaff\udfff\ue03e\udaff\udfff\ue046\udaff\udfff\ue03d\udaff\udfff\ue034\udaff\udfff\ue041\udaff\udfff\ue062\udaff\udfe0",
            "chief", "\ue060\udaff\udfff\ue032\udaff\udfff\ue037\udaff\udfff\ue038\udaff\udfff\ue034\udaff\udfff\ue035\udaff\udfff\ue062\udaff\udfe2",
            "strategist", "\ue060\udaff\udfff\ue042\udaff\udfff\ue043\udaff\udfff\ue041\udaff\udfff\ue030\udaff\udfff\ue043\udaff\udfff\ue034\udaff\udfff\ue036\udaff\udfff\ue038\udaff\udfff\ue042\udaff\udfff\ue043\udaff\udfff\ue062\udaff\udfc4",
            "captain", "\ue060\udaff\udfff\ue032\udaff\udfff\ue030\udaff\udfff\ue03f\udaff\udfff\ue043\udaff\udfff\ue030\udaff\udfff\ue038\udaff\udfff\ue03d\udaff\udfff\ue062\udaff\udfd6",
            "recruiter", "\ue060\udaff\udfff\ue041\udaff\udfff\ue034\udaff\udfff\ue032\udaff\udfff\ue041\udaff\udfff\ue044\udaff\udfff\ue038\udaff\udfff\ue043\udaff\udfff\ue034\udaff\udfff\ue041\udaff\udfff\ue062\udaff\udfca",
            "recruit", "\ue060\udaff\udfff\ue041\udaff\udfff\ue034\udaff\udfff\ue032\udaff\udfff\ue041\udaff\udfff\ue044\udaff\udfff\ue038\udaff\udfff\ue043\udaff\udfff\ue062\udaff\udfd6"
    );

    public static final Map<String, Integer> RANK_COLORS = Map.of(
            "owner", 0xA200ED,
            "chief", 0xA200ED,
            "strategist", 0xED9600,
            "captain", 0xFAE100,
            "recruiter", 0x04D400,
            "recruit", 0x00F0DC
    );

    public static final Map<String, Integer> NAME_COLORS = Map.of(
            "owner", 0x7415A6,
            "chief", 0x7415A6,
            "strategist", 0xBE790E,
            "captain", 0xC8B414,
            "recruiter", 0x1E9514,
            "recruit", 0x21A89B
    );

    public static Text modifyGuildMessage(Text originalMessage) {
        if (originalMessage.getSiblings().size() < 3) {
            return originalMessage;
        }

        String rankText = originalMessage.getSiblings().get(2).getString();

        String matchedRank = null;
        for (Map.Entry<String, String> entry : RANK.entrySet()) {
            if (entry.getValue().equals(rankText)) {
                matchedRank = entry.getKey();
                break;
            }
        }

        if (matchedRank == null) {
            return originalMessage;
        }

        String username = getUsername(originalMessage);
        if (username == null) {
            return originalMessage;
        }

        return modifyChat(originalMessage, matchedRank);
    }

    private static String getUsername(Text text) {
        final String COLOR = "#00AAAA";
        final String[] username = {null};

        text.visit((style, content) -> {
            if (style.getColor() != null && !content.isEmpty()) {
                String colorName = style.getColor().getName();

                if (COLOR.equalsIgnoreCase(colorName)) {
                    String extractedName = null;

                    if (style.getHoverEvent() != null) {
                        Object hoverValue = style.getHoverEvent().getValue(style.getHoverEvent().getAction());
                        if (hoverValue instanceof Text hoverText) {
                            String hoverString = hoverText.getString();
                            Pattern hoverPattern = Pattern.compile("(.*?)'s? real name is (.*)");
                            Matcher hoverMatcher = hoverPattern.matcher(hoverString);
                            if (hoverMatcher.find()) {
                                extractedName = hoverMatcher.group(2);
                            }
                        }
                    }

                    if (extractedName == null) {
                        String cleanedName = content.trim();
                        if (cleanedName.endsWith(":")) {
                            extractedName = cleanedName.substring(0, cleanedName.length() - 1);
                        } else {
                            int colonIndex = cleanedName.indexOf(":");
                            if (colonIndex > 0) {
                                extractedName = cleanedName.substring(0, colonIndex);
                            }
                        }
                    }
                    username[0] = extractedName;
                    return Optional.of(true);
                }
            }
            return Optional.empty();
        }, Style.EMPTY);

        return username[0];
    }

    private static Text modifyChat(Text originalMessage, String rank) {
        Integer rankColorValue = RANK_COLORS.get(rank);
        Integer nameColorValue = NAME_COLORS.get(rank);

        MutableText modifiedMessage = Text.empty().setStyle(originalMessage.getStyle());

        for (int i = 0; i < originalMessage.getSiblings().size(); i++) {
            Text sibling = originalMessage.getSiblings().get(i);

            if (i == 2) {
                Style originalStyle = sibling.getStyle();
                Style newStyle = originalStyle.withColor(TextColor.fromRgb(rankColorValue));
                MutableText modifiedSibling = Text.literal(sibling.getString()).setStyle(newStyle);
                modifiedMessage.append(modifiedSibling);

            } else if (isName(sibling)) {
                Style originalStyle = sibling.getStyle();
                Style newStyle = originalStyle.withColor(TextColor.fromRgb(nameColorValue));
                MutableText modifiedSibling = Text.literal(sibling.getString()).setStyle(newStyle);
                modifiedMessage.append(modifiedSibling);

            } else {
                modifiedMessage.append(sibling);

            }
        }
        return modifiedMessage;
    }

    private static boolean isName(Text sibling) {
        String color = sibling.getStyle().getColor().getName();
        boolean hasNameColor = "#00AAAA".equalsIgnoreCase(color);

        if (!hasNameColor) {
            return false;
        }
        return !sibling.getStyle().isUnderlined();
    }
}
