package dev.wiji.tbgm.controllers;

import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GetRealName {
    public static void createRealNameMap(Text message, HashMap<String, List<String>> nameMap) {
        // No need to do anything if a nickname can't be found in the message
        if (!messageHasNickHoverDeep(message)) {
            return;
        }
        if (!message.getSiblings().isEmpty()) {
            for (Text siblingMessage : message.getSiblings()) {
                // Don't mess with components without nicknames, to minimize potential errors
                if (messageHasNickHoverDeep(siblingMessage)) {
                    // Chat messages can be deeply nested, so we need to use recursion
                    createRealNameMap(siblingMessage, nameMap);
                    tryToAddRealName(siblingMessage, nameMap);
                }
            }
        }
    }

    private static void tryToAddRealName(Text message, HashMap<String, List<String>> nameMap) {
        if (messageHasNickHover(message)) {
            HoverEvent hover = message.getStyle().getHoverEvent();
            if (hover == null) return;
            if (hover.getValue(hover.getAction()) instanceof Text hoverText) {
                final String regex = "(.*?)'s? real name is (.*)";
                Matcher matcher = Pattern.compile(regex, Pattern.MULTILINE).matcher(hoverText.getString());
                if (!matcher.matches()) return;
                String realName = matcher.group(2);
                String nickname = matcher.group(1);
                if (nameMap.containsKey(nickname)) {
                    nameMap.get(nickname).add(realName);
                } else {
                    nameMap.put(nickname, new ArrayList<>(Collections.singletonList(realName)));
                }
            }
        }
    }

    public static boolean messageHasNickHoverDeep(Text message) {
        boolean hasNick = false;
        if (!message.getSiblings().isEmpty()) {
            for (Text messageSibling : message.getSiblings()) {
                hasNick = hasNick || messageHasNickHoverDeep(messageSibling);
            }
        } else {
            return messageHasNickHover(message);
        }
        return hasNick;
    }
    public static boolean messageHasNickHover(Text message) {
        HoverEvent hover = message.getStyle().getHoverEvent();
        if (hover != null && hover.getValue(hover.getAction()) instanceof Text hoverText) {
            return hoverText.getString().contains("real username");
        }
        return false;
    }
}