package dev.wiji.wynntracker.controllers;

import net.minecraft.text.Text;
import net.minecraft.text.Style;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static dev.wiji.wynntracker.misc.Misc.getUnformattedString;

public class DiscordBridge {
    public static final String GUILD_CHAT_PREFIX_FLAG = "\udaff\udffc\ue006\udaff\udfff\ue002\udaff\udffe";
    public static final String GUILD_CHAT_PREFIX_FLAGPOLE = "\udaff\udffc\ue001\udb00\udc06";
    public static final String DISCORD_MESSAGE_SEQUENCE = "\uEff1";

    public static final String COLOR = "aqua";

    public static String parseChatMessage(Text message) {
        String messageString = message.getString();
        boolean hasChatPrefix = messageString.startsWith(GUILD_CHAT_PREFIX_FLAG) || messageString.startsWith(GUILD_CHAT_PREFIX_FLAGPOLE);

        if (!hasChatPrefix) {
            return null;
        }

        final boolean[] hasCorrectColor = {false};

        message.visit((style, content) -> {
            if (!content.isEmpty() && (content.startsWith(GUILD_CHAT_PREFIX_FLAG) || content.startsWith(GUILD_CHAT_PREFIX_FLAGPOLE))) {
                if (style.getColor() != null) {
                    String colorName = style.getColor().getName();
                    if (COLOR.equalsIgnoreCase(colorName)) {
                        hasCorrectColor[0] = true;
                    }
                }
                return Optional.of(true);
            }
            return Optional.empty();
        }, Style.EMPTY);

        if (!hasCorrectColor[0]) {
            return null;
        }

        String username = getUsername(message);
        if (username == null) {
            return null;
        }

        String messageBody = getMessageBody(message);
        if (messageBody == null) {
            return null;
        }

        System.out.println("wynnTracker username: " + username);
        System.out.println("wynnTracker body: " + messageBody);
        System.out.println("wynnTracker full: " + messageString);

        // Send the message to the Discord bridge
        Authentication.getWebSocketManager().sendChatMessage(username, messageBody);
        return username;
    }

    private static String getUsername(Text text) {
        final String COLOR = "dark_aqua";
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

    private static String getMessageBody(Text text) {
        StringBuilder fullText = new StringBuilder();
        text.visit((content) -> {
            fullText.append(content);
            return Optional.empty();
        });

        String completeMessage = fullText.toString();
        int bodyIndex = completeMessage.indexOf(":");

        if (bodyIndex != -1) {
            int bodyStartIndex = bodyIndex + 1;
            String messageBody = completeMessage.substring(bodyStartIndex);

            if (messageBody.startsWith(" ")) {
                messageBody = messageBody.substring(1);
            }
            messageBody = getUnformattedString(messageBody);

            return messageBody.isEmpty() ? null : messageBody;
        }
        return null;
    }
}
