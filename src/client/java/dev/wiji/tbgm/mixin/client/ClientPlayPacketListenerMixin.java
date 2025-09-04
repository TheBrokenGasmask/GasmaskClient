package dev.wiji.tbgm.mixin.client;
import com.wynntils.core.WynntilsMod;
import dev.wiji.tbgm.controllers.PlayerManager;
import dev.wiji.tbgm.controllers.Updater;
import dev.wiji.tbgm.controllers.WarReport;
import dev.wiji.tbgm.misc.Misc;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayPacketListenerMixin {
    private static int JOIN_COUNTER = 0;

    @Inject(method = "onGameJoin", at = @At("RETURN"))
    private void onGameJoinReturn(GameJoinS2CPacket packet, CallbackInfo ci) {
        JOIN_COUNTER++;
        if (JOIN_COUNTER == 1) {
            PlayerManager.startAutoFetch();
            WynntilsMod.registerEventListener(new WarReport());
        } else if (JOIN_COUNTER == 2) {
            new Thread(() -> {
                try {
                    Thread.sleep(7000);
                    if (Updater.IS_CLIENT_UPDATED){
                        Misc.sendTbgmSuccessMessage("New version installed! Please restart Minecraft to apply the update.");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }
    }
}
