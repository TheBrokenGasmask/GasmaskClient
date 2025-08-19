package dev.wiji.tbgm.mixin.client;

import com.wynntils.core.text.StyledText;
import com.wynntils.handlers.chat.type.MessageType;
import com.wynntils.handlers.chat.type.RecipientType;
import dev.wiji.tbgm.controllers.DiscordBridge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RecipientType.class)
public class MatchPatternMixin {

    @Inject(
            method = "matchPattern(Lcom/wynntils/core/text/StyledText;Lcom/wynntils/handlers/chat/type/MessageType;)Z",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void onMatchPattern(StyledText msg, MessageType messageType, CallbackInfoReturnable<Boolean> cir) {
        if ((Object)this == RecipientType.GUILD && messageType == MessageType.FOREGROUND) {
            String text = null;
            try {
                text = (String) StyledText.class.getMethod("getString").invoke(msg);
            } catch (Exception ignored) {}

            if (text == null) text = msg.toString();

            if (text.contains(DiscordBridge.GUILD_CHAT_PREFIX_FLAG) || text.contains(DiscordBridge.DISCORD_MESSAGE_SEQUENCE)) cir.setReturnValue(true);
        }
    }
}
