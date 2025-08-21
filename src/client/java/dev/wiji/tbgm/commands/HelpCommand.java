package dev.wiji.tbgm.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.wiji.tbgm.objects.AbstractClientCommand;
import dev.wiji.tbgm.objects.ClientCommand;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import dev.wiji.tbgm.misc.Misc;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;
import java.util.Optional;

public class HelpCommand extends AbstractClientCommand {
    private final List<ClientCommand> commands;
    
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
    
    private int executeHelp(CommandContext<FabricClientCommandSource> context) {
        FabricClientCommandSource source = context.getSource();
        
        Misc.sendTbgmMessage("=== tbgm Commands ===");
        
        for (ClientCommand command : commands) {
            Misc.sendTbgmMessage("/tbgm " + command.getName() + " - " + command.getDescription());
        }
        
        Misc.sendTbgmMessage("Use '/tbgm help <command>' for more information about a specific command.");
        
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
            Misc.sendTbgmMessage("=== Command: /tbgm " + cmd.getName() + " ===");
            Misc.sendTbgmMessage("Description: " + cmd.getDescription());
            Misc.sendTbgmMessage("Usage:");
            
            String[] usageLines = cmd.getUsage().split("\n");
            for (String line : usageLines) Misc.sendTbgmMessage("  " + line);

        } else {
            sendErrorMessage(source, "Command not found: " + commandName);
        }
        
        return 1;
    }
}