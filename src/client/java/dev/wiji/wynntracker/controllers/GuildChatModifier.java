package dev.wiji.wynntracker.controllers;

import dev.wiji.wynntracker.enums.Rank;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Identifier;

import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GuildChatModifier {

    private static Rank isCustomRank(String name) {
        PlayerManager.PlayerInfo playerInfo = PlayerManager.getPlayerInfo(name);
        if (playerInfo == null || playerInfo.getRank() == null) {
            return null;
        }

        String playerRank = playerInfo.getRank().toLowerCase();

        Set<String> vanillaRanks = Set.of("owner", "chief", "strategist", "captain", "recruiter", "recruit");
        if (!vanillaRanks.contains(playerRank)) {
            Optional<Rank> rankOptional = Rank.fromString(playerRank);
            return rankOptional.orElse(null);
        }

        return null;
    }

    public static Text modifyGuildMessage(Text originalMessage) {
        if (originalMessage.getSiblings().size() < 3) {
            return originalMessage;
        }

        String username = getUsername(originalMessage);
        if (username == null) {
            return originalMessage;
        }

        Rank customRank = isCustomRank(username);
        if (customRank != null) {
            return modifyCustomRankChat(originalMessage, customRank);
        }

        String rankText = originalMessage.getSiblings().get(2).getString();

        Rank matchedRank = null;
        for (Rank rank : Rank.values()) {
            if (rank.getBackgroundText().equals(rankText)) {
                matchedRank = rank;
                break;
            }
        }

        if (matchedRank == null) {
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

    private static Text modifyCustomRankChat(Text originalMessage, Rank customRank) {
        int rankColorValue = customRank.getRankColor();
        int nameColorValue = customRank.getNameColor();
        String customBackground = customRank.getBackgroundText();
        String customForeground = customRank.getForegroundText();

        MutableText modifiedMessage = Text.empty().setStyle(originalMessage.getStyle());

        for (int i = 0; i < originalMessage.getSiblings().size(); i++) {
            Text sibling = originalMessage.getSiblings().get(i);

            if (i == 2) {
                Style originalStyle = sibling.getStyle();
                Style newStyle = originalStyle.withColor(TextColor.fromRgb(0x242424));
                MutableText modifiedSibling = Text.literal(customBackground).setStyle(newStyle);
                modifiedMessage.append(modifiedSibling);

            } else if (i == 3) {
                Style originalStyle = sibling.getStyle();
                Style newStyle = originalStyle.withColor(TextColor.fromRgb(rankColorValue));
                MutableText modifiedSibling = Text.literal(customForeground).setStyle(newStyle);
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

    private static Text modifyChat(Text originalMessage, Rank rank) {
        int rankColorValue = rank.getRankColor();
        int nameColorValue = rank.getNameColor();

        MutableText modifiedMessage = Text.empty().setStyle(originalMessage.getStyle());

        for (int i = 0; i < originalMessage.getSiblings().size(); i++) {
            Text sibling = originalMessage.getSiblings().get(i);

            if (i == 2) {
                Style originalStyle = sibling.getStyle();
                Style newStyle = originalStyle.withColor(TextColor.fromRgb(0x242424));
                MutableText modifiedSibling = Text.literal(sibling.getString()).setStyle(newStyle);
                modifiedMessage.append(modifiedSibling);

            } else if (i == 3) {
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
                modifiedMessage.append(modifyTextColor(sibling));

            }
        }
        return modifiedMessage;
    }

    private static MutableText modifyTextColor(Text sibling) {
        if (shouldModifyColor(sibling)) {
            Style originalStyle = sibling.getStyle();
            Style newStyle = originalStyle.withColor(TextColor.fromRgb(0xC9FFFF));
            return Text.literal(sibling.getString()).setStyle(newStyle);
        }
        return Text.literal(sibling.getString()).setStyle(sibling.getStyle());
    }

    private static boolean shouldModifyColor(Text sibling) {
        Style style = sibling.getStyle();
        if (style.getColor() == null || style.getColor().getRgb() != 0x55FFFF) {
            return false;
        }

        Identifier font = style.getFont();
        if (font != null) {
            String fontString = font.toString();
            if ("minecraft:chat/prefix".equals(fontString)) {
                return false;
            }
        }
        return true;
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
