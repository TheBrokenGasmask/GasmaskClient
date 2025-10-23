package dev.wiji.tbgm.mixin.client;

import dev.wiji.tbgm.controllers.AspectReport;
import dev.wiji.tbgm.controllers.DiscordBridge;
import dev.wiji.tbgm.controllers.RaidReport;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ChatMixin {

    @Inject(method = "onGameMessage", at = @At("HEAD"))
    private void onGameMessagePacket(GameMessageS2CPacket packet, CallbackInfo ci) {

        if (packet.overlay()) return;

        String threadName = Thread.currentThread().getName();
        if (!threadName.startsWith("Netty Client IO")) return;

        Text message = packet.content();

        RaidReport.parseChatMessage(message);
        AspectReport.parseChatMessage(message);
        String username = DiscordBridge.parseChatMessage(message);

        if (username != null) System.out.println("WynnTracker: Message from " + username);

    }
}