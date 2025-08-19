package dev.wiji.tbgm.mixin.client;

import dev.wiji.tbgm.controllers.DiscordBridge;
import dev.wiji.tbgm.controllers.GuildChatModifier;
import dev.wiji.tbgm.controllers.SocketMessageHandler;
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

        SocketMessageHandler.lastMessageIsGuildChat = message.getString().contains(DiscordBridge.GUILD_CHAT_PREFIX_FLAG)
                || message.getString().contains(DiscordBridge.DISCORD_MESSAGE_SEQUENCE);
    }
}