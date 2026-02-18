package dev.wiji.tbgm.mixin.client;

import com.wynntils.mc.extension.EntityRenderStateExtension;
import dev.wiji.tbgm.badge.BadgeManager;
import dev.wiji.tbgm.controllers.PlayerManager;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.command.RenderCommandQueue;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
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
    private static EntityRenderState currentEntityState;
    @Unique
    private static float currentUOffset;
    @Unique
    private static float currentVOffset;

    @Inject(method = "renderLeaderboardBadge", at = @At("HEAD"))
    private static void captureRenderContext(
            MatrixStack poseStack, RenderCommandQueue collector, EntityRenderState entityState, CameraRenderState cameraState, Identifier texture, float width, float height, float uOffset, float vOffset, float u, float v, float textureWidth, float textureHeight, float customOffset, float horizontalShift, float verticalShift, CallbackInfo ci) {

        currentEntityState = entityState;
        currentUOffset = uOffset;
        currentVOffset = vOffset;
    }

    @ModifyVariable(method = "renderLeaderboardBadge", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private static Identifier modifyTexture(Identifier originalTexture) {
        if (currentEntityState != null && ((EntityRenderStateExtension) currentEntityState).getEntity() instanceof PlayerEntity player) {

            String name = player.getName().getString();

            PlayerManager.PlayerInfo info = PlayerManager.getPlayerInfo(name);
            if (info == null) return originalTexture;

            UUID playerUuid = info.getUuid();
            int customColor = BadgeManager.getBadgeColor(playerUuid, (int) currentUOffset, (int) currentVOffset);

            if (customColor != 0) return Identifier.of("tbgm", "textures/badges.png");
        }

        return originalTexture;
    }
}
