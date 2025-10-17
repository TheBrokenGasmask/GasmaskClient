package dev.wiji.tbgm.controllers;

import dev.wiji.tbgm.enums.Rank;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class SocketMessageHandler {
    private static final Pattern URL_PATTERN = Pattern.compile(
			"(http|ftp|https)://([\\w_-]+(?:\\.[\\w_-]+)+)([\\w.,@?^=%&:/~+#-]*[\\w@?^=%&/~+#-])"
    );

    private static final int MAX_MESSAGE_LENGTH = 1000;
    private static final int CHARACTERS_PER_LINE = 50;

    public static boolean lastMessageIsGuildChat = false;

    public static void announceToClient(Boolean guildAlert, String body) {
        if (!isValidClient()) return;
        
        String truncatedBody = truncateMessage(body);
        String[] lines = splitMessageToLines(truncatedBody, CHARACTERS_PER_LINE);
        
        for (int i = 0; i < lines.length; i++) {
            sendAnnouncementLine(guildAlert, lines[i], i == 0);
        }
        lastMessageIsGuildChat = true;
    }

    public static void messageToClient(String name, String rankName, String body) {
        if (!isValidClient()) return;
        
        Optional<Rank> rankOptional = Rank.fromString(rankName);
        if (rankOptional.isEmpty()) return;
        
        String truncatedBody = truncateMessage(body);
        String[] lines = splitMessageToLines(truncatedBody, CHARACTERS_PER_LINE);
        
        for (int i = 0; i < lines.length; i++) {
            sendMessageLine(name, rankOptional.get(), lines[i], i == 0);
        }
        lastMessageIsGuildChat = true;
    }

    private static MutableText parseUrls(String text, int textColor) {
        Matcher matcher = URL_PATTERN.matcher(text);
        MutableText result = Text.literal("");
        int lastEnd = 0;

        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();
            String url = matcher.group();

            if (start > lastEnd) {
                String beforeUrl = text.substring(lastEnd, start);
                result.append(createTextComponent(beforeUrl, textColor));
            }

            result.append(createUrlComponent(url));
            lastEnd = end;
        }

        if (lastEnd < text.length()) {
            String remainingText = text.substring(lastEnd);
            result.append(createTextComponent(remainingText, textColor));
        }

        if (lastEnd == 0) result = createTextComponent(text, textColor);

        return result;
    }
    
    private static MutableText createTextComponent(String text, int color) {
        return Text.literal(text)
                .setStyle(Style.EMPTY
                        .withColor(color)
                        .withFont(Identifier.of("minecraft", "default")));
    }
    
    private static MutableText createUrlComponent(String url) {
        return Text.literal(url)
                .setStyle(Style.EMPTY
                        .withColor(0xabffff)
                        .withUnderline(true)
                        .withFont(Identifier.of("minecraft", "default"))
                        .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                Text.literal("Click to open in browser")
                                        .setStyle(Style.EMPTY.withColor(Formatting.GRAY)))));
    }

    private static boolean isValidClient() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client != null && client.getNetworkHandler() != null && client.player != null;
    }
    
    private static String truncateMessage(String message) {
        if (message.length() <= MAX_MESSAGE_LENGTH) {
            return message;
        }
        return message.substring(0, MAX_MESSAGE_LENGTH - 3) + "...";
    }
    
    private static void sendAnnouncementLine(Boolean guildAlert, String lineText, boolean isFirstLine) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayNetworkHandler networkHandler = client.getNetworkHandler();
        
        int textColor = guildAlert ? 0xff9999 : 0xc9ffff;

        MutableText finalMessage;
        
        if (guildAlert && isFirstLine) {
            MutableText chatPrefix = createChatPrefix();
            finalMessage = createGuildAlertMessage(chatPrefix, lineText, textColor);
        } else if (isFirstLine) {
            MutableText chatPrefix = createChatPrefix();
            MutableText bodyComponent = createBodyComponent(lineText, textColor);
            finalMessage = chatPrefix.append(bodyComponent);
        } else {
            // Continuation lines use flagpole prefix
            MutableText continuationPrefix = Text.literal(DiscordBridge.GUILD_CHAT_PREFIX_FLAGPOLE)
                    .setStyle(Style.EMPTY
                            .withColor(Formatting.AQUA)
                            .withFont(Identifier.of("minecraft", "chat/prefix")));
            MutableText bodyComponent = createBodyComponent(lineText, textColor);
            finalMessage = continuationPrefix.append(bodyComponent);
        }
        
        GameMessageS2CPacket packet = new GameMessageS2CPacket(finalMessage, false);
        client.execute(() -> networkHandler.onGameMessage(packet));
    }
    
    private static void sendMessageLine(String name, Rank rank, String lineText, boolean isFirstLine) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayNetworkHandler networkHandler = client.getNetworkHandler();
        
        // Color logic: regular messages use cyan
        int textColor = 0xc9ffff;
        
        MutableText lineMessage;
        
        if (isFirstLine) {
            lineMessage = createFullMessage(name, rank, lineText, textColor);
        } else {
            lineMessage = createContinuationMessage(lineText, textColor);
        }
        
        GameMessageS2CPacket packet = new GameMessageS2CPacket(lineMessage, false);
        client.execute(() -> networkHandler.onGameMessage(packet));
    }
    
    private static MutableText createChatPrefix() {
        return Text.literal(getChatPrefix())
                .setStyle(Style.EMPTY
                        .withColor(Formatting.AQUA)
                        .withFont(Identifier.of("minecraft", "chat/prefix")));
    }
    
    private static MutableText createBodyComponent(String text, int color) {
        return Text.literal(" " + text)
                .setStyle(Style.EMPTY
                        .withColor(color)
                        .withFont(Identifier.of("minecraft", "default")));
    }
    
    private static MutableText createGuildAlertMessage(MutableText chatPrefix, String body, int textColor) {
        Rank tbgmRank = Rank.TBGM;
        
        MutableText guildAlertComponent = Text.literal(tbgmRank.getBackgroundText())
                .setStyle(Style.EMPTY
                        .withColor(0x242424)
                        .withFont(Identifier.of("minecraft", "banner/pill")));
        
        MutableText guildAlertForegroundComponent = Text.literal(tbgmRank.getForegroundText())
                .setStyle(Style.EMPTY
                        .withColor(tbgmRank.getRankColor())
                        .withFont(Identifier.of("minecraft", "banner/pill"))
                        .withShadowColor(16777215));
        
        MutableText colonComponent = Text.literal(":")
                .setStyle(Style.EMPTY
                        .withColor(tbgmRank.getRankColor())
                        .withFont(Identifier.of("minecraft", "default")));
        
        MutableText bodyComponent = createBodyComponent(body, textColor);
        
        return chatPrefix
                .append(Text.literal(" "))
                .append(guildAlertComponent)
                .append(guildAlertForegroundComponent)
                .append(colonComponent)
                .append(bodyComponent);
    }
    
    private static MutableText createFullMessage(String name, Rank rank, String body, int textColor) {
        MutableText chatPrefix = createChatPrefix();
        
        MutableText discordIcon = Text.literal(" ").append(Text.literal("\uEff1")
                .setStyle(Style.EMPTY
                        .withColor(Formatting.WHITE)
                        .withFont(Identifier.of("tbgm", "decorators"))));

//        MutableText starIcon = Text.literal("\uEFE0\uEff2")
//                .setStyle(Style.EMPTY
//                        .withColor(Formatting.WHITE)
//                        .withFont(Identifier.of("tbgm", "decorators")));
        
        MutableText rankBackgroundComponent = Text.literal("\udaff\udfff\udaff\udfff" + rank.getBackgroundText())
                .setStyle(Style.EMPTY
                        .withColor(0x242424)
                        .withFont(Identifier.of("minecraft", "banner/pill")));
        
        MutableText rankForegroundComponent = Text.literal(rank.getForegroundText())
                .setStyle(Style.EMPTY
                        .withColor(rank.getRankColor())
                        .withFont(Identifier.of("minecraft", "banner/pill"))
                        .withShadowColor(16777215));
        
        MutableText nameComponent = Text.literal(" " + name + ":")
                .setStyle(Style.EMPTY
                        .withColor(rank.getNameColor())
                        .withFont(Identifier.of("minecraft", "default")));
        
        MutableText bodyComponent = Text.literal(" ").append(parseUrls(body, textColor));
        
        return chatPrefix
                .append(discordIcon)
//                .append(starIcon)
                .append(rankBackgroundComponent)
                .append(rankForegroundComponent)
                .append(nameComponent)
                .append(bodyComponent);
    }
    
    private static MutableText createContinuationMessage(String body, int textColor) {
        MutableText continuationPrefix = Text.literal(DiscordBridge.GUILD_CHAT_PREFIX_FLAGPOLE)
                .setStyle(Style.EMPTY
                        .withColor(Formatting.AQUA)
                        .withFont(Identifier.of("minecraft", "chat/prefix")));
        
        MutableText bodyComponent = Text.literal(" ").append(parseUrls(body, textColor));
        return continuationPrefix.append(bodyComponent);
    }
    
    private static String[] splitMessageToLines(String message, int maxWidth) {
        if (message.length() <= maxWidth) {
            return new String[]{message};
        }
        
        List<String> lines = new ArrayList<>();
        String remaining = message;
        
        while (remaining.length() > maxWidth) {
            int breakPoint = maxWidth;
            
            int lastSpace = remaining.lastIndexOf(' ', maxWidth);
            if (lastSpace > maxWidth / 2) {
                breakPoint = lastSpace;
            }
            
            lines.add(remaining.substring(0, breakPoint).trim());
            remaining = remaining.substring(breakPoint).trim();
        }
        
        if (!remaining.isEmpty()) {
            lines.add(remaining);
        }
        
        return lines.toArray(new String[0]);
    }

    private static String getChatPrefix() {
        return lastMessageIsGuildChat ? DiscordBridge.GUILD_CHAT_PREFIX_FLAGPOLE : DiscordBridge.GUILD_CHAT_PREFIX_FLAG;
    }
}
