package dev.wiji.wynntracker.mixin.client;

import com.wynntils.features.players.CustomNametagRendererFeature;
import com.wynntils.mc.event.PlayerNametagRenderEvent;
import com.wynntils.mc.extension.EntityRenderStateExtension;
import dev.wiji.wynntracker.controllers.PlayerIconManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import java.util.List;

@Mixin(value = CustomNametagRendererFeature.class, remap = false, priority = 2000)
public class NametagRendererMixin {

    @ModifyConstant(
            method = "addAccountTypeNametag",
            constant = @Constant(stringValue = "\uE100")
    )
    private String replaceLogo(String original, PlayerNametagRenderEvent event, List<?> nametags) {
        Entity entity = ((EntityRenderStateExtension) event.getEntityRenderState()).getEntity();
        if (entity instanceof PlayerEntity player) {
			return PlayerIconManager.getPlayerIcon(player.getUuid()).getCharacter();
        }
        return original;
    }
}
