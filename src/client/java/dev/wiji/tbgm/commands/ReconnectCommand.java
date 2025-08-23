package dev.wiji.tbgm.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import dev.wiji.tbgm.controllers.Authentication;
import dev.wiji.tbgm.objects.AbstractClientCommand;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import dev.wiji.tbgm.misc.Misc;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

public class ReconnectCommand extends AbstractClientCommand {

    public ReconnectCommand() {
        super(
                "reconnect",
                "Forces reauthentication and reconnects to the WebSocket",
                "/tbgm reconnect"
        );
    }

    @Override
    public void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommandManager.literal(PREFIX)
                .then(ClientCommandManager.literal(name)
                        .executes(this::executeReconnect)));
    }

    private int executeReconnect(CommandContext<FabricClientCommandSource> source) {
        new Thread(() -> {
            ClientPlayerEntity player = MinecraftClient.getInstance().player;
            if (player == null) return;

            try {
                Authentication.getWebSocketManager().disconnect();
                Authentication.invalidateToken();
                Thread.sleep(1000);

                Authentication.forceReauthentication()
                        .exceptionally(ex -> {
                            ClientPlayerEntity p = MinecraftClient.getInstance().player;
                            if (p != null) {
                                Misc.sendTbgmErrorMessage("Failed to reconnect: " + ex.getMessage());
                            }
                            return null;
                        });

            } catch (Exception e) {
                sendErrorMessage(source.getSource(), "Failed to initiate reconnection: " + e.getMessage());
            }

        }).start();

        return 1;
    }
}