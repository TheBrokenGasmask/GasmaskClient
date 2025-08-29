package dev.wiji.tbgm.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.wiji.tbgm.controllers.DiscordBridge;
import dev.wiji.tbgm.enums.Rank;
import dev.wiji.tbgm.objects.AbstractClientCommand;
import dev.wiji.tbgm.objects.ClientCommand;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class HelpCommand extends AbstractClientCommand {
    private static final int COMMAND_COLOR = 0xFFD700;
    private static final int DESCRIPTION_COLOR = 0xcfc7b0;
    private static final int USAGE_COLOR = 0xcfc7b0;
    private static final int SUBTITLE_COLOR = 0xf4da85;

    private final List<ClientCommand> commands;
    private static final int CHARACTERS_PER_LINE = 50;

    public HelpCommand(List<ClientCommand> commands) {
        super(
            "help",
            "Shows available tbgm commands",
            "/tbgm help [command]"
        );
        this.commands = commands;
    }

    @Override
    public void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommandManager.literal(PREFIX)
            .then(ClientCommandManager.literal(name)
                .executes(this::executeHelp)
                .then(ClientCommandManager.argument("command", StringArgumentType.word())
                    .executes(this::executeHelpForCommand))));

        dispatcher.register(ClientCommandManager.literal(PREFIX)
                .executes(this::executeHelp));
    }

    private void sendMessage(Text message, boolean useFlag) {
        MutableText prefix = Text.literal((useFlag ? DiscordBridge.GUILD_CHAT_PREFIX_FLAG : DiscordBridge.GUILD_CHAT_PREFIX_FLAGPOLE) + " ")
                .setStyle(Style.EMPTY.withColor(SUBTITLE_COLOR).withFont(Identifier.of("minecraft", "chat/prefix")));
        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(prefix.append(message));
    }

    private void sendWrappedMessage(Text message, boolean useFlag) {
        List<Text> wrappedLines = wrapTextPreservingStyle(message, CHARACTERS_PER_LINE);
        for (int i = 0; i < wrappedLines.size(); i++) {
            sendMessage(wrappedLines.get(i), i == 0 && useFlag);
        }
    }
    
    private void sendWrappedMessageManual(MutableText part1, MutableText part2, MutableText part3, boolean useFlag) {
        // Combine the parts to get the full text for length calculation
        String fullText = part1.getString() + part2.getString() + part3.getString();
        
        if (fullText.length() <= CHARACTERS_PER_LINE) {
            // No wrapping needed
            sendMessage(part1.append(part2).append(part3), useFlag);
            return;
        }
        
        // Manual wrapping preserving the three styled parts
        List<String> lines = splitIntoLines(fullText, CHARACTERS_PER_LINE);
        
        String part1Text = part1.getString();
        String part2Text = part2.getString();
        String part3Text = part3.getString();
        
        int part1End = part1Text.length();
        int part2End = part1End + part2Text.length();
        
        int currentPos = 0;
        for (int i = 0; i < lines.size(); i++) {
            String lineText = lines.get(i);
            MutableText lineComponent = Text.empty();
            
            int lineStart = currentPos;
            int lineEnd = currentPos + lineText.length();
            
            // Add part1 content if it overlaps this line
            if (lineStart < part1End) {
                int start = Math.max(0, lineStart);
                int end = Math.min(part1End, lineEnd);
                if (end > start) {
                    String content = part1Text.substring(start, end);
                    if (!content.isEmpty()) {
                        lineComponent.append(Text.literal(content).setStyle(part1.getStyle()));
                    }
                }
            }
            
            // Add part2 content if it overlaps this line
            if (lineStart < part2End && lineEnd > part1End) {
                int start = Math.max(part1End, lineStart);
                int end = Math.min(part2End, lineEnd);
                if (end > start) {
                    String content = part2Text.substring(start - part1End, end - part1End);
                    if (!content.isEmpty()) {
                        lineComponent.append(Text.literal(content).setStyle(part2.getStyle()));
                    }
                }
            }
            
            // Add part3 content if it overlaps this line
            if (lineEnd > part2End && lineStart < fullText.length()) {
                int start = Math.max(part2End, lineStart);
                int end = Math.min(fullText.length(), lineEnd);
                if (end > start) {
                    String content = part3Text.substring(start - part2End, end - part2End);
                    if (!content.isEmpty()) {
                        lineComponent.append(Text.literal(content).setStyle(part3.getStyle()));
                    }
                }
            }
            
            sendMessage(lineComponent, i == 0 && useFlag);
            
            // Move to next line position - the splitIntoLines already handles space removal
            currentPos += lineText.length();
            // If there are more lines, we need to account for the space that was removed
            if (i < lines.size() - 1) {
                currentPos++; // Account for the space character that was skipped
            }
        }
    }

    private List<Text> wrapTextPreservingStyle(Text originalText, int maxWidth) {
        List<Text> lines = new ArrayList<>();
        String fullText = originalText.getString();
        
        if (fullText.length() <= maxWidth) {
            lines.add(originalText);
            return lines;
        }
        
        // Collect all text components with their styles
        List<TextComponent> components = new ArrayList<>();
        collectTextComponents(originalText, components);
        
        // Split text while preserving component boundaries
        List<String> textLines = splitIntoLines(fullText, maxWidth);
        
        // Rebuild each line with proper styling
        int globalPos = 0;
        for (String lineText : textLines) {
            MutableText lineComponent = Text.empty();
            int lineStart = globalPos;
            int lineEnd = globalPos + lineText.length();
            
            for (TextComponent comp : components) {
                // Find overlap between this component and current line
                int compStart = comp.start;
                int compEnd = comp.end;
                
                if (compStart < lineEnd && compEnd > lineStart) {
                    // There's an overlap
                    int overlapStart = Math.max(compStart, lineStart);
                    int overlapEnd = Math.min(compEnd, lineEnd);
                    
                    String overlapText = fullText.substring(overlapStart, overlapEnd);
                    if (!overlapText.isEmpty()) {
                        lineComponent.append(Text.literal(overlapText).setStyle(comp.style));
                    }
                }
            }
            
            lines.add(lineComponent);
            globalPos = lineEnd;
            
            // Skip whitespace between lines
            while (globalPos < fullText.length() && Character.isWhitespace(fullText.charAt(globalPos))) {
                globalPos++;
            }
        }
        
        return lines;
    }
    
    private void collectTextComponents(Text text, List<TextComponent> components) {
        collectTextComponentsFlat(text, components);
    }
    
    private void collectTextComponentsFlat(Text text, List<TextComponent> components) {
        // Get the string content for just this Text node
        String textString = text.getString();
        String siblingsString = "";
        
        for (Text sibling : text.getSiblings()) {
            siblingsString += sibling.getString();
        }
        
        // The direct content is the difference
        String directContent;
        if (siblingsString.isEmpty()) {
            directContent = textString;
        } else {
            directContent = textString.substring(0, textString.length() - siblingsString.length());
        }
        
        if (!directContent.isEmpty()) {
            int start = getCurrentPosition(components);
            components.add(new TextComponent(directContent, start, start + directContent.length(), text.getStyle()));
        }
        
        // Process siblings
        for (Text sibling : text.getSiblings()) {
            collectTextComponentsFlat(sibling, components);
        }
    }
    
    private int getCurrentPosition(List<TextComponent> components) {
        return components.stream().mapToInt(c -> c.content.length()).sum();
    }
    
    private List<String> splitIntoLines(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        String remaining = text;
        
        while (remaining.length() > maxWidth) {
            int breakPoint = maxWidth;
            int lastSpace = remaining.lastIndexOf(' ', maxWidth - 1);
            if (lastSpace > maxWidth / 2) {
                breakPoint = lastSpace;
                // Don't include the space in the line
                lines.add(remaining.substring(0, breakPoint));
                remaining = remaining.substring(breakPoint + 1); // Skip the space
            } else {
                // No good break point found, break at maxWidth
                lines.add(remaining.substring(0, breakPoint));
                remaining = remaining.substring(breakPoint);
            }
        }
        
        if (!remaining.isEmpty()) {
            lines.add(remaining);
        }
        
        return lines;
    }
    
    private static class TextComponent {
        final String content;
        final int start;
        final int end;
        final Style style;
        
        TextComponent(String content, int start, int end, Style style) {
            this.content = content;
            this.start = start;
            this.end = end;
            this.style = style;
        }
    }

    private String[] splitMessageToLines(String message, int maxWidth) {
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

    private int executeHelp(CommandContext<FabricClientCommandSource> context) {
        MutableText helpMessage = Text.empty()
                .append(getTbgmComponent())
                .append(Text.literal("Available commands").setStyle(Style.EMPTY.withColor(SUBTITLE_COLOR).withFont(Identifier.of("minecraft", "default"))));
        sendMessage(helpMessage, true);

        for (ClientCommand command : commands) {
            MutableText commandName = Text.literal(command.getUsage()).setStyle(Style.EMPTY.withColor(COMMAND_COLOR).withFont(Identifier.of("minecraft", "default")));
            MutableText separator = Text.literal(" - ").setStyle(Style.EMPTY.withColor(SUBTITLE_COLOR).withFont(Identifier.of("minecraft", "default")));
            MutableText description = Text.literal(command.getDescription()).setStyle(Style.EMPTY.withColor(DESCRIPTION_COLOR).withFont(Identifier.of("minecraft", "default")));
            sendWrappedMessageManual(commandName, separator, description, false);
        }

        sendWrappedMessage(Text.literal("Use '/tbgm help <command>' for more information about a specific command.").setStyle(Style.EMPTY.withColor(SUBTITLE_COLOR).withFont(Identifier.of("minecraft", "default"))), false);

        return 1;
    }

    private int executeHelpForCommand(CommandContext<FabricClientCommandSource> context) {
        FabricClientCommandSource source = context.getSource();
        String commandName = StringArgumentType.getString(context, "command");

        Optional<ClientCommand> command = commands.stream()
            .filter(cmd -> cmd.getName().equals(commandName))
            .findFirst();

        if (command.isPresent()) {
            ClientCommand cmd = command.get();
            MutableText commandMessage = Text.empty()
                    .append(getTbgmComponent())
                    .append(Text.literal("Command: ").setStyle(Style.EMPTY.withColor(SUBTITLE_COLOR).withFont(Identifier.of("minecraft", "default"))))
                    .append(Text.literal("/tbgm " + cmd.getName()).setStyle(Style.EMPTY.withColor(COMMAND_COLOR).withFont(Identifier.of("minecraft", "default"))));
            sendMessage(commandMessage, true);

            sendWrappedMessage(Text.literal("Description:").setStyle(Style.EMPTY.withColor(SUBTITLE_COLOR).withFont(Identifier.of("minecraft", "default"))), false);
            sendWrappedMessage(Text.literal(cmd.getDescription()).setStyle(Style.EMPTY.withColor(DESCRIPTION_COLOR).withFont(Identifier.of("minecraft", "default"))), false);

            sendWrappedMessage(Text.literal("Usage:").setStyle(Style.EMPTY.withColor(SUBTITLE_COLOR).withFont(Identifier.of("minecraft", "default"))), false);
            sendWrappedMessage(Text.literal(cmd.getUsage()).setStyle(Style.EMPTY.withColor(USAGE_COLOR).withFont(Identifier.of("minecraft", "default"))), false);

        } else {
            sendErrorMessage(source, "Command not found: " + commandName);
        }

        return 1;
    }

    private MutableText getTbgmComponent() {
        Rank tbgmRank = Rank.TBGM;
        
        MutableText guildAlertComponent = Text.literal(tbgmRank.getBackgroundText())
                .setStyle(Style.EMPTY
                        .withColor(0x242424)
                        .withFont(Identifier.of("minecraft", "banner/pill")));
        
        MutableText guildAlertForegroundComponent = Text.literal(tbgmRank.getForegroundText())
                .setStyle(Style.EMPTY
                        .withColor(tbgmRank.getRankColor())
                        .withFont(Identifier.of("minecraft", "banner/pill")));
        
        return guildAlertComponent.append(guildAlertForegroundComponent).append(Text.literal(" "));
    }
}