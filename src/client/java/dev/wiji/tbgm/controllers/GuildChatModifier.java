package dev.wiji.tbgm.controllers;

import dev.wiji.tbgm.enums.Rank;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import org.spongepowered.asm.mixin.Mutable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GuildChatModifier {
    private static boolean isRankColorEnabled(){
        return Config.getConfigData().customGuildRankColors;
    }

    private static boolean isChatColorEnabled(){
        return Config.getConfigData().customGuildChatColors;
    }

    private static Pair<Rank, Boolean> isCustomRank(String name) {
        PlayerManager.PlayerInfo playerInfo = PlayerManager.getPlayerInfo(name);
        if (playerInfo == null || playerInfo.getRank() == null) return null;

        String playerRank = playerInfo.getRank().toLowerCase();

        Optional<Rank> rankOptional = Rank.fromString(playerRank);
        boolean multipleRanks = playerInfo.hasMultipleRanks();

        return new Pair<>(rankOptional.orElse(null), multipleRanks);
    }

    public static Text modifyGuildMessage(Text originalMessage) {
        int siblingCount = originalMessage.getSiblings().size();

        if (siblingCount < 6) return originalMessage;

        String username = getUsername(originalMessage);
        if (username == null) return originalMessage;

        Pair<Rank, Boolean> customRank = isCustomRank(username);
        if (customRank != null) return modifyCustomRankChat(originalMessage, customRank);

        String rankText = originalMessage.getSiblings().get(2).getString();

        Rank matchedRank = null;
        for (Rank rank : Rank.values()) {
            if (rankText.contains(rank.getBackgroundText())) {
                matchedRank = rank;

                break;
            }
        }

        if (matchedRank == null) return originalMessage;

        return modifyChat(originalMessage, new Pair<>(matchedRank, false));
    }

    private static String getUsername(Text text) {
        final String COLOR = "#00AAAA";
        final String[] username = {null};

        text.visit((style, content) -> {
            if (style.getColor() != null && !content.isEmpty()) {
                String colorName = style.getColor().getHexCode();

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

    private static Text modifyCustomRankChat(Text originalMessage, Pair<Rank, Boolean> customRank) {
        return modifyMessageWithRank(originalMessage, customRank, true);
    }

    private static Text modifyChat(Text originalMessage, Pair<Rank, Boolean> rank) {
        return modifyMessageWithRank(originalMessage, rank, false);
    }

    private static Text modifyMessageWithRank(Text originalMessage, Pair<Rank, Boolean> rank, boolean useCustomText) {
        int rankColorValue;
        int nameColorValue;
        int rankColorBackgroundValue;

        if (isRankColorEnabled()) {
            rankColorValue = rank.getLeft().getRankColor();
            nameColorValue = rank.getLeft().getNameColor();
            rankColorBackgroundValue = 0x242424;
        } else {
            rankColorValue = 0x000000;
            nameColorValue = 0x00AAAA;
            rankColorBackgroundValue = 0x55FFFF;
        }

        String starIcon = useCustomText && rank.getRight() ? "\uEff2" : null;

        String rankBackgroundText = useCustomText ? rank.getLeft().getBackgroundText() : null;
        String rankForegroundText = useCustomText ? rank.getLeft().getForegroundText() : null;

        MutableText modifiedMessage = Text.empty().setStyle(originalMessage.getStyle());

        int siblingCount = originalMessage.getSiblings().size() + (starIcon != null ? 1 : 0);

        for (int i = 0; i < siblingCount; i++) {
            Text sibling;

            try {
                sibling = originalMessage.getSiblings().get(i);
            } catch(IndexOutOfBoundsException e) {
                sibling = Text.empty();
            }

            modifiedMessage.append(processSibling(sibling, i, rankColorValue, nameColorValue, starIcon, rankBackgroundText, rankForegroundText, rankColorBackgroundValue));
        }
        return modifiedMessage;
    }

    private static Text processSibling(Text sibling, int index, int rankColorValue, int nameColorValue, String starText, String rankBackgroundText, String rankForegroundText, int rankColorBackgroundValue) {
        if (index == 0) {
            return Text.literal("").append(processChatPrefix(sibling)).append(Text.literal(" "));
        } else if (index == 1 && starText != null) {
			return Text.literal(starText)
			.setStyle(Style.EMPTY
					.withColor(Formatting.WHITE)
					.withFont(Identifier.of("tbgm", "decorators"))
			);
        } else if (index == 2) {
            return createStyledPillText(rankBackgroundText != null ? (starText != null ? "\udaff\udffe" : "\udaff\udffc") + rankBackgroundText: sibling.getString(), sibling.getStyle(), rankColorBackgroundValue);
        } else if (index == 3) {
            return createStyledPillText(rankForegroundText != null ? rankForegroundText + " " : sibling.getString(), sibling.getStyle(), rankColorValue);
        } else if (isName(sibling)) {
            MutableText mutableText = sibling.copy();
            return mutableText.withColor(nameColorValue);
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

    private static MutableText createStyledPillText(String text, Style originalStyle, int color) {
        Style newStyle = originalStyle.withColor(TextColor.fromRgb(color)).withFont(Identifier.of("minecraft", "banner/pill"));
        return Text.literal(text).setStyle(newStyle);
    }

    private static MutableText modifyTextColor(Text sibling) {
        List<Text> components = reconstructSibling(sibling);

        MutableText output = Text.empty();

        components.forEach(component -> {
            if (shouldModifyColor(component) && isChatColorEnabled()) {
                Style originalStyle = component.getStyle();
                Style newStyle = originalStyle.withColor(TextColor.fromRgb(0xC9FFFF));
                component = Text.literal(component.getString()).setStyle(newStyle);
            }

            output.append(component);
        });

        return output;
    }

    private static List<Text> reconstructSibling(Text sibling) {
        String[] parts = sibling.getString().split("\n");
        if(parts.length <= 1) return List.of(sibling);

        List<Text> base = new ArrayList<>();


        for(int i = 0; i < parts.length; i++) {
            String part = parts[i];
            StringBuilder modifiedPart = new StringBuilder();

            String[] subParts = part.split(" ");

            for(int j = 0; j < subParts.length; j++) {
                if (j == 0) {
                    MutableText text = Text.literal(subParts[0]);
                    Style style = Style.EMPTY.withFont(Identifier.of("minecraft", "chat/prefix"));
                    text.setStyle(style);

                    base.add(text);
                }
                else modifiedPart.append(" ").append(subParts[j]);
            }

            Text line = Text.literal(modifiedPart.toString());

            base.add(line);
            if (i != parts.length - 1) base.add(Text.literal("\n"));
        }

        return base;
    }

    private static boolean shouldModifyColor(Text sibling) {
        String content = sibling.getString();
        if (content.contains(DiscordBridge.GUILD_CHAT_PREFIX_FLAGPOLE) || content.contains(DiscordBridge.GUILD_CHAT_PREFIX_FLAG)) {
            return false;
        }

        Style style = sibling.getStyle();

        Identifier font = style.getFont();
        if (font != null) {
            String fontString = font.toString();
            if ("minecraft:chat/prefix".equals(fontString)) {
                return false;
            }
        }

		return !style.isUnderlined() || style.getClickEvent() != null;
	}

    private static boolean isName(Text sibling) {
        TextColor color = sibling.getStyle().getColor();
        if(color == null) return false;

        boolean hasNameColor = "#00AAAA".equalsIgnoreCase(color.getHexCode());

        if (!hasNameColor) return false;

        return !sibling.getStyle().isUnderlined();
    }
}
