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
    }

    private void sendMessage(Text message, boolean useFlag) {
        MutableText prefix = Text.literal((useFlag ? DiscordBridge.GUILD_CHAT_PREFIX_FLAG : DiscordBridge.GUILD_CHAT_PREFIX_FLAGPOLE) + " ")
                .setStyle(Style.EMPTY.withColor(SUBTITLE_COLOR).withFont(Identifier.of("minecraft", "chat/prefix")));
        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(prefix.append(message));
    }

    private void sendWrappedMessage(Text message, boolean useFlag) {
        String[] lines = splitMessageToLines(message.getString(), CHARACTERS_PER_LINE);
        for (int i = 0; i < lines.length; i++) {
            sendMessage(Text.literal(lines[i]).setStyle(message.getStyle()), i == 0 && useFlag);
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
            MutableText commandName = Text.literal("/tbgm " + command.getName()).setStyle(Style.EMPTY.withColor(COMMAND_COLOR).withFont(Identifier.of("minecraft", "default")));
            MutableText separator = Text.literal(" - ").setStyle(Style.EMPTY.withColor(SUBTITLE_COLOR).withFont(Identifier.of("minecraft", "default")));
            MutableText description = Text.literal(command.getDescription()).setStyle(Style.EMPTY.withColor(DESCRIPTION_COLOR).withFont(Identifier.of("minecraft", "default")));
            sendMessage(commandName.append(separator).append(description), false);
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
            sendWrappedMessage(Text.literal("  " + cmd.getDescription()).setStyle(Style.EMPTY.withColor(DESCRIPTION_COLOR).withFont(Identifier.of("minecraft", "default"))), false);

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