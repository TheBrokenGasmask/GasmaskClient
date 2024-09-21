package dev.wiji.wynntracker.mixin.client;

import dev.wiji.wynntracker.controllers.AspectReport;
import dev.wiji.wynntracker.controllers.RaidReport;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatHud.class)
public class ChatMixin {
    @Inject(method = { "addMessage(Lnet/minecraft/text/Text;)V" }, at = { @At("HEAD") })
    private void onMessage(Text message, final CallbackInfo ci) {
        RaidReport.parseChatMessage(message);
        AspectReport.parseChatMessage(message);
    }
}