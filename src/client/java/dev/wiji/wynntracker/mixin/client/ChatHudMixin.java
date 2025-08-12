package dev.wiji.wynntracker.mixin.client;

import dev.wiji.wynntracker.controllers.GuildChatModifier;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatHud.class)
public class ChatHudMixin {

    private boolean isProcessing = false;

    @Inject(method = "addMessage", at = @At("HEAD"), cancellable = true)
    private void onAddMessage(Text message, CallbackInfo ci) {
        if (isProcessing) {
            return;
        }

        System.out.println("processing message: " + message.getString());
        System.out.println("sibling count: " + message.getSiblings().size());

        for (int i = 0; i < message.getSiblings().size(); i++) {
            Text sibling = message.getSiblings().get(i);
            System.out.println("sibling " + i + ": \"" + sibling.getString() + "\" - style: " + sibling.getStyle());
        }

        Text modifiedMessage = GuildChatModifier.modifyGuildMessage(message);

        if (modifiedMessage != message) {
            ci.cancel();
            isProcessing = true;

            try {
                ((ChatHud)(Object)this).addMessage(modifiedMessage);
            } finally {
                isProcessing = false;
            }
        }
    }
}