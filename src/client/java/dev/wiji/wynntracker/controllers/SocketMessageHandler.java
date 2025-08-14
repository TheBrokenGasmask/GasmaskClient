package dev.wiji.wynntracker.controllers;

import dev.wiji.wynntracker.enums.Rank;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class SocketMessageHandler {
    public static final String CHAT_PREFIX = "\udaff\udffc\ue006\udaff\udfff\ue002\udaff\udffe";

    private static final Pattern URL_PATTERN = Pattern.compile(
            "https?://(?:[-\\w.])+(?:[:\\d]+)?(?:/(?:[\\w/_.])*(?:\\?(?:[&\\w._=-])*)?(?:#(?:[\\w._-]*))?)?"
    );

    public static void announceToClient(Boolean guildAlert, String body) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayNetworkHandler networkHandler = client.getNetworkHandler();
        MutableText finalMessage;
        if (networkHandler == null || client.player == null) {
            return;
        }

        MutableText chatPrefix = Text.literal(CHAT_PREFIX)
                .setStyle(Style.EMPTY
                        .withColor(Formatting.AQUA)
                        .withFont(Identifier.of("minecraft", "chat/prefix")));

        if (guildAlert) {
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
            //Keep - resets font to default for rest of the message
            MutableText colonComponent = Text.literal(":")
                    .setStyle(Style.EMPTY
                            .withColor(tbgmRank.getRankColor())
                            .withFont(Identifier.of("minecraft", "default")));

            MutableText bodyComponent = Text.literal(" "+body);

            finalMessage = chatPrefix
                    .append(guildAlertComponent)
                    .append(guildAlertForegroundComponent)
                    .append(colonComponent)
                    .append(bodyComponent);
        } else {
            MutableText bodyComponent = Text.literal(" "+body)
                    .setStyle(Style.EMPTY
                            .withColor(0xc9ffff)
                            .withFont(Identifier.of("minecraft", "default")));
            finalMessage = chatPrefix
                    .append(bodyComponent);
        }

        GameMessageS2CPacket packet = new GameMessageS2CPacket(finalMessage, false);

        client.execute(() -> {
            networkHandler.onGameMessage(packet);
        });
    }

    public static void messageToClient(String name, String rankName, String body) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayNetworkHandler networkHandler = client.getNetworkHandler();

        if (networkHandler == null || client.player == null) {
            return;
        }

        Optional<Rank> rankOptional = Rank.fromString(rankName);
        if (rankOptional.isEmpty()) {
            return;
        }
        Rank rank = rankOptional.get();

        MutableText chatPrefix = Text.literal(CHAT_PREFIX)
                .setStyle(Style.EMPTY
                        .withColor(Formatting.AQUA)
                        .withFont(Identifier.of("minecraft", "chat/prefix")));
        MutableText discordIcon = Text.literal("\uE000")
                .setStyle(Style.EMPTY
                        .withFont(Identifier.of("wynntracker", "discord")));

        MutableText rankBackgroundComponent = Text.literal(" "+rank.getBackgroundText())
                .setStyle(Style.EMPTY
                        .withColor(0x242424)
                        .withFont(Identifier.of("minecraft", "banner/pill")));

        MutableText rankForegroundComponent = Text.literal(rank.getForegroundText())
                .setStyle(Style.EMPTY
                    .withColor(rank.getRankColor())
                    .withFont(Identifier.of("minecraft", "banner/pill"))
                    .withShadowColor(16777215));

        MutableText nameComponent = Text.literal(" "+name+":")
                .setStyle(Style.EMPTY
                        .withColor(rank.getNameColor())
                        .withFont(Identifier.of("minecraft", "default")));

        MutableText bodyComponent = Text.literal(" ").append(parseUrls(body));

        MutableText finalMessage = chatPrefix
                .append(discordIcon)
                .append(rankBackgroundComponent)
                .append(rankForegroundComponent)
                .append(nameComponent)
                .append(bodyComponent);

        GameMessageS2CPacket packet = new GameMessageS2CPacket(finalMessage, false);

        client.execute(() -> {
            networkHandler.onGameMessage(packet);
        });
    }

    private static MutableText parseUrls(String text) {
        Matcher matcher = URL_PATTERN.matcher(text);
        MutableText result = Text.literal("");
        int lastEnd = 0;

        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();
            String url = matcher.group();

            if (start > lastEnd) {
                String beforeUrl = text.substring(lastEnd, start);
                result.append(Text.literal(beforeUrl)
                        .setStyle(Style.EMPTY
                                .withColor(0xc9ffff)
                                .withFont(Identifier.of("minecraft", "default"))));
            }

            MutableText urlComponent = Text.literal(url)
                    .setStyle(Style.EMPTY
                            .withColor(0xabffff)
                            .withUnderline(true)
                            .withFont(Identifier.of("minecraft", "default"))
                            .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url))
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                    Text.literal("Click to open in browser")
                                            .setStyle(Style.EMPTY.withColor(Formatting.GRAY)))));

            result.append(urlComponent);
            lastEnd = end;
        }

        if (lastEnd < text.length()) {
            String remainingText = text.substring(lastEnd);
            result.append(Text.literal(remainingText)
                    .setStyle(Style.EMPTY
                            .withColor(0xc9ffff)
                            .withFont(Identifier.of("minecraft", "default"))));
        }

        if (lastEnd == 0) {
            result = Text.literal(text)
                    .setStyle(Style.EMPTY
                            .withColor(0xc9ffff)
                            .withFont(Identifier.of("minecraft", "default")));
        }

        return result;
    }
}
