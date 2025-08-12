package dev.wiji.wynntracker.mixin.client;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.wiji.wynntracker.badge.BadgeManager;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(targets = "com.wynntils.utils.render.RenderUtils", remap = false)
public class RenderUtilsMixin {
    
    @Inject(method = "renderProfessionBadge", at = @At("HEAD"))
    private static void onRenderProfessionBadge(
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
        
        // Check if this is a player entity and if we have custom colors for their badges
        if (entity instanceof PlayerEntity player) {
            UUID playerUuid = player.getUuid();
            int customColor = BadgeManager.getBadgeColor(playerUuid, uOffset, vOffset);
            
            if (customColor != 0) {
                // Apply the custom color using RenderSystem color tinting
                float red = ((customColor >> 16) & 0xFF) / 255.0f;
                float green = ((customColor >> 8) & 0xFF) / 255.0f;
                float blue = (customColor & 0xFF) / 255.0f;
                
                RenderSystem.setShaderColor(red, green, blue, 1.0f);
            }
        }
    }
    
    @Inject(method = "renderProfessionBadge", at = @At("RETURN"))
    private static void onRenderProfessionBadgeReturn(
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
        
        // Reset color to white after rendering
        if (entity instanceof PlayerEntity player) {
            UUID playerUuid = player.getUuid();
            int customColor = BadgeManager.getBadgeColor(playerUuid, uOffset, vOffset);
            
            if (customColor != 0) {
                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            }
        }
    }
}