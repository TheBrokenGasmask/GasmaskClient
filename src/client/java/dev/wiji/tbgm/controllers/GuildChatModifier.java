package dev.wiji.tbgm.controllers;

import dev.wiji.tbgm.enums.Rank;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.text.Normalizer;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GuildChatModifier {

    private static Rank isCustomRank(String name) {
        PlayerManager.PlayerInfo playerInfo = PlayerManager.getPlayerInfo(name);
        if (playerInfo == null || playerInfo.getRank() == null) return null;

        String playerRank = playerInfo.getRank().toLowerCase();

        Optional<Rank> rankOptional = Rank.fromString(playerRank);
        return rankOptional.orElse(null);
    }

    public static Text modifyGuildMessage(Text originalMessage) {
        int siblingCount = originalMessage.getSiblings().size();

        if (siblingCount == 3) {
            Text prefixSibling = originalMessage.getSiblings().getFirst();
            boolean isFlagPole = prefixSibling.getString().contains(DiscordBridge.GUILD_CHAT_PREFIX_FLAGPOLE);

            if (prefixSibling.getStyle().getColor() == null) return originalMessage;
            boolean isAqua = prefixSibling.getStyle().getColor().getRgb() == Formatting.AQUA.getColorValue();

            if (!isFlagPole || !isAqua) return originalMessage;
            Text modifiedContent = modifyTextColor(originalMessage.getSiblings().get(2));
            return Text.empty()
                    .append(prefixSibling)
                    .append(modifiedContent);
        } else if (siblingCount < 3) return originalMessage;

        String username = getUsername(originalMessage);
        if (username == null) return originalMessage;

        Rank customRank = isCustomRank(username);
        if (customRank != null) return modifyCustomRankChat(originalMessage, customRank);

        String rankText = originalMessage.getSiblings().get(2).getString();

        Rank matchedRank = null;
        for (Rank rank : Rank.values()) {
            if (rank.getBackgroundText().equals(rankText)) {
                matchedRank = rank;
                break;
            }
        }

        if (matchedRank == null) return originalMessage;

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
                            if (hoverMatcher.find()) extractedName = hoverMatcher.group(2);

                            Pattern nicknamePattern = Pattern.compile("^(.*?)'?s nickname is (.*)$");
                            Matcher nicknameMatcher = nicknamePattern.matcher(hoverString);
                            if (nicknameMatcher.find()) extractedName = nicknameMatcher.group(1);
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
        return modifyMessageWithRank(originalMessage, customRank, true);
    }

    private static Text modifyChat(Text originalMessage, Rank rank) {
        return modifyMessageWithRank(originalMessage, rank, false);
    }

    private static Text modifyMessageWithRank(Text originalMessage, Rank rank, boolean useCustomText) {
        int rankColorValue = rank.getRankColor();
        int nameColorValue = rank.getNameColor();
        String rankBackgroundText = useCustomText ? rank.getBackgroundText() : null;
        String rankForegroundText = useCustomText ? rank.getForegroundText() : null;

        MutableText modifiedMessage = Text.empty().setStyle(originalMessage.getStyle());

        for (int i = 0; i < originalMessage.getSiblings().size(); i++) {
            Text sibling = originalMessage.getSiblings().get(i);
            modifiedMessage.append(processSibling(sibling, i, rankColorValue, nameColorValue, rankBackgroundText, rankForegroundText));
        }
        return modifiedMessage;
    }

    private static Text processSibling(Text sibling, int index, int rankColorValue, int nameColorValue, String rankBackgroundText, String rankForegroundText) {
        if (index == 0) {
            return processChatPrefix(sibling);
        } else if (index == 2) {
            return createStyledText(rankBackgroundText != null ? rankBackgroundText : sibling.getString(), sibling.getStyle(), 0x242424);
        } else if (index == 3) {
            return createStyledText(rankForegroundText != null ? rankForegroundText : sibling.getString(), sibling.getStyle(), rankColorValue);
        } else if (isName(sibling)) {
            return createStyledText(sibling.getString(), sibling.getStyle(), nameColorValue);
        } else {
            return modifyTextColor(sibling);
        }
    }

    private static Text processChatPrefix(Text sibling) {
        String chatPrefix = sibling.getString();
        if (chatPrefix.contains(DiscordBridge.GUILD_CHAT_PREFIX_FLAG) && SocketMessageHandler.lastMessageIsGuildChat) {
            return Text.literal(DiscordBridge.GUILD_CHAT_PREFIX_FLAGPOLE).setStyle(sibling.getStyle());
        }
        return sibling;
    }

    private static MutableText createStyledText(String text, Style originalStyle, int color) {
        Style newStyle = originalStyle.withColor(TextColor.fromRgb(color));
        return Text.literal(text).setStyle(newStyle);
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

        if (style.isUnderlined() && style.getClickEvent() == null) {
            return false;
        }
        return true;
    }

    private static boolean isName(Text sibling) {
        TextColor color = sibling.getStyle().getColor();
        if(color == null) return false;

        boolean hasNameColor = "#00AAAA".equalsIgnoreCase(color.getName());

        if (!hasNameColor) return false;

        return !sibling.getStyle().isUnderlined();
    }
}
