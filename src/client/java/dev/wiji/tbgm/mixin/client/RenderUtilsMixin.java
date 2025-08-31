package dev.wiji.tbgm.mixin.client;

import dev.wiji.tbgm.badge.BadgeManager;
import dev.wiji.tbgm.controllers.PlayerManager;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(targets = "com.wynntils.utils.render.RenderUtils", remap = false)
public class RenderUtilsMixin {
    
    // Store context for the current render call
    @Unique
    private static Entity currentEntity;
    @Unique
    private static int currentUOffset;
    @Unique
    private static int currentVOffset;
    
    @Inject(method = "renderProfessionBadge", at = @At("HEAD"))
    private static void captureRenderContext(
            MatrixStack matrices,
            EntityRenderDispatcher dispatcher,
            Entity entity,
            Identifier texture,
            float width,
            float height,
            int uOffset,
            int vOffset,
            int u,
            int v,
            int textureWidth,
            int textureHeight,
            float customOffset,
            float horizontalShift,
            float verticalShift,
            CallbackInfo ci) {
        
        // Store context for ModifyVariable
        currentEntity = entity;
        currentUOffset = uOffset;
        currentVOffset = vOffset;
    }
    
    @ModifyVariable(method = "renderProfessionBadge", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private static Identifier modifyTexture(Identifier originalTexture) {
        if (currentEntity instanceof PlayerEntity player) {

            String name = player.getName().getString();

            PlayerManager.PlayerInfo info = PlayerManager.getPlayerInfo(name);
            if (info == null) return originalTexture;

            UUID playerUuid = info.getUuid();
            int customColor = BadgeManager.getBadgeColor(playerUuid, currentUOffset, currentVOffset);
            
            if (customColor != 0) return Identifier.of("tbgm", "textures/badges.png");
        }
        
        return originalTexture;
    }
}