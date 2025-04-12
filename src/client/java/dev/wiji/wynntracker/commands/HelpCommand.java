package dev.wiji.wynntracker.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.wiji.wynntracker.objects.AbstractClientCommand;
import dev.wiji.wynntracker.objects.ClientCommand;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;
import java.util.Optional;

public class HelpCommand extends AbstractClientCommand {
    private final List<ClientCommand> commands;
    
    public HelpCommand(List<ClientCommand> commands) {
        super(
            "help", 
            "Shows available WynnTracker commands",
            "/wynntracker help [command]"
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
    
    private int executeHelp(CommandContext<FabricClientCommandSource> context) {
        FabricClientCommandSource source = context.getSource();
        
        source.sendFeedback(Text.literal("=== WynnTracker Commands ===").formatted(Formatting.GOLD));
        
        for (ClientCommand command : commands) {
            source.sendFeedback(Text.literal("/wynntracker " + command.getName() + " - " + command.getDescription())
                .formatted(Formatting.YELLOW));
        }
        
        source.sendFeedback(Text.literal("Use '/wynntracker help <command>' for more information about a specific command.")
            .formatted(Formatting.GRAY));
        
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
            source.sendFeedback(Text.literal("=== Command: /wynntracker " + cmd.getName() + " ===")
                .formatted(Formatting.GOLD));
            source.sendFeedback(Text.literal("Description: " + cmd.getDescription())
                .formatted(Formatting.YELLOW));
            source.sendFeedback(Text.literal("Usage:").formatted(Formatting.YELLOW));
            
            String[] usageLines = cmd.getUsage().split("\n");
            for (String line : usageLines) {
                source.sendFeedback(Text.literal("  " + line).formatted(Formatting.AQUA));
            }
        } else {
            sendErrorMessage(source, "Command not found: " + commandName);
        }
        
        return 1;
    }
}