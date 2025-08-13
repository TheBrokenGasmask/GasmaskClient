package dev.wiji.wynntracker.controllers;

import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class SocketMessageHandler {
    public static final String CHAT_PREFIX = "\udaff\udffc\ue006\udaff\udfff\ue002\udaff\udffe";

    private static final Pattern URL_PATTERN = Pattern.compile(
            "https?://(?:[-\\w.])+(?:[:\\d]+)?(?:/(?:[\\w/_.])*(?:\\?(?:[&\\w._=-])*)?(?:#(?:[\\w._-]*))?)?"
    );
    //static temporary values for styling, should move later
    public static final Map<String, String> RANK = Map.of(
            "owner", "\ue060\udaff\udfff\ue03e\udaff\udfff\ue046\udaff\udfff\ue03d\udaff\udfff\ue034\udaff\udfff\ue041\udaff\udfff\ue062\udaff\udfe0",
            "chief", "\ue060\udaff\udfff\ue032\udaff\udfff\ue037\udaff\udfff\ue038\udaff\udfff\ue034\udaff\udfff\ue035\udaff\udfff\ue062\udaff\udfe2",
            "strategist", "\ue060\udaff\udfff\ue042\udaff\udfff\ue043\udaff\udfff\ue041\udaff\udfff\ue030\udaff\udfff\ue043\udaff\udfff\ue034\udaff\udfff\ue036\udaff\udfff\ue038\udaff\udfff\ue042\udaff\udfff\ue043\udaff\udfff\ue062\udaff\udfc4",
            "captain", "\ue060\udaff\udfff\ue032\udaff\udfff\ue030\udaff\udfff\ue03f\udaff\udfff\ue043\udaff\udfff\ue030\udaff\udfff\ue038\udaff\udfff\ue03d\udaff\udfff\ue062\udaff\udfd6",
            "recruiter", "\ue060\udaff\udfff\ue041\udaff\udfff\ue034\udaff\udfff\ue032\udaff\udfff\ue041\udaff\udfff\ue044\udaff\udfff\ue038\udaff\udfff\ue043\udaff\udfff\ue034\udaff\udfff\ue041\udaff\udfff\ue062\udaff\udfca",
            "recruit", "\ue060\udaff\udfff\ue041\udaff\udfff\ue034\udaff\udfff\ue032\udaff\udfff\ue041\udaff\udfff\ue044\udaff\udfff\ue038\udaff\udfff\ue043\udaff\udfff\ue062\udaff\udfd6",
            "officer", "\uE060\uDAFF\uDFFF\uE03E\uDAFF\uDFFF\uE035\uDAFF\uDFFF\uE035\uDAFF\uDFFF\uE038\uDAFF\uDFFF\uE032\uDAFF\uDFFF\uE034\uDAFF\uDFFF\uE041\uDAFF\uDFFF\uE062\uDAFF\uDFD6",
            "advisor", "\uE060\uDAFF\uDFFF\uE030\uDAFF\uDFFF\uE033\uDAFF\uDFFF\uE045\uDAFF\uDFFF\uE038\uDAFF\uDFFF\uE042\uDAFF\uDFFF\uE03E\uDAFF\uDFFF\uE041\uDAFF\uDFFF\uE062\uDAFF\uDFD1"
    );
    public static final Map<String, String> RANK_FOREGROUND = Map.of(
            "owner","\uE00E\uE016\uE00D\uE004\uE011\uDB00\uDC02",
            "chief","\uE002\uE007\uE008\uE004\uE005\uDB00\uDC02",
            "strategist","\uE012\uE013\uE011\uE000\uE013\uE004\uE006\uE008\uE012\uE013\uDB00\uDC02",
            "captain","\uE002\uE000\uE00F\uE013\uE000\uE008\uE00D\uDB00\uDC02",
            "recruiter","\uE011\uE004\uE002\uE011\uE014\uE008\uE013\uE004\uE011\uDB00\uDC02",
            "recruit","\uE011\uE004\uE002\uE011\uE014\uE008\uE013\uDB00\uDC02",
            "officer", "\uE00E\uE005\uE005\uE008\uE002\uE004\uE011\uDB00\uDC02",
            "advisor", "\uE000\uE003\uE015\uE008\uE012\uE00E\uE011\uDB00\uDC02"
    );
    public static final Map<String, Integer> RANK_COLORS = Map.of(
            "owner", 0xA200ED,
            "chief", 0xA200ED,
            "strategist", 0xED9600,
            "captain", 0xFAE100,
            "recruiter", 0x04D400,
            "recruit", 0x00F0DC,
            "officer", 0x00F0DC,
            "advisor", 0xED9600
    );

    public static final Map<String, Integer> NAME_COLORS = Map.of(
            "owner", 0x7415A6,
            "chief", 0x7415A6,
            "strategist", 0xBE790E,
            "captain", 0xC8B414,
            "recruiter", 0x1E9514,
            "recruit", 0x21A89B,
            "officer", 0x21A89B,
            "advisor", 0xBE790E
    );

    public static void messageToClient(String name, String rank, String body) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayNetworkHandler networkHandler = client.getNetworkHandler();

        if (networkHandler == null || client.player == null) {
            return;
        }

        MutableText chatPrefix = Text.literal(CHAT_PREFIX)
                .setStyle(Style.EMPTY
                        .withColor(Formatting.AQUA)
                        .withFont(Identifier.of("minecraft", "chat/prefix")));
        MutableText discordIcon = Text.literal("\uE000")
                .setStyle(Style.EMPTY
                        .withFont(Identifier.of("wynntracker", "discord")));
        MutableText rankBackgroundComponent;
        MutableText rankForegroundComponent;

        if (RANK.containsKey(rank.toLowerCase())) {
            String rankKey = rank.toLowerCase();
            String rankText = RANK.get(rankKey);
            rankBackgroundComponent = Text.literal(" "+rankText)
                    .setStyle(Style.EMPTY
                            .withColor(0x242424)
                            .withFont(Identifier.of("minecraft", "banner/pill")));

            String rankForegroundText = RANK_FOREGROUND.get(rankKey);
            Integer rankColor = RANK_COLORS.get(rankKey);
            rankForegroundComponent = Text.literal(rankForegroundText)
                    .setStyle(Style.EMPTY
                        .withColor(rankColor)
                        .withFont(Identifier.of("minecraft", "banner/pill"))
                        .withShadowColor(16777215));

        } else {return;}

        Integer nameColor = NAME_COLORS.get(rank.toLowerCase());
        MutableText nameComponent = Text.literal(" "+name+":")
                .setStyle(Style.EMPTY
                        .withColor(nameColor)
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
