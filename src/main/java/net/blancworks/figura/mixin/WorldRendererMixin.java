package net.blancworks.figura.mixin;

import net.blancworks.figura.avatar.AvatarData;
import net.blancworks.figura.avatar.AvatarDataManager;
import net.blancworks.figura.lua.api.RenderLayerAPI;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Matrix4f;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin {

    @Shadow @Final private EntityRenderDispatcher entityRenderDispatcher;
    @Shadow @Nullable private ClientWorld world;

    @Inject(at = @At("HEAD"), method = "render")
    private void render(MatrixStack matrices, float tickDelta, long limitTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f matrix4f, CallbackInfo ci) {
        if (this.world == null) return;

        this.world.getPlayers().forEach((player) -> {
            AvatarData data = AvatarDataManager.getDataForPlayer(player.getUuid());
            if (data != null && data.script != null) {
                data.script.onWorldRender(tickDelta);
            }
        });
    }

    @Inject(at = @At("HEAD"), method = "renderEntity")
    private void renderEntity(Entity entity, double cameraX, double cameraY, double cameraZ, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, CallbackInfo ci) {
        if (entity instanceof PlayerEntity ent) {
            matrices.push();

            try {
                AvatarData data = AvatarDataManager.getDataForPlayer(ent.getUuid());
                if (data != null && data.model != null)
                    data.model.renderWorldParts(cameraX, cameraY, cameraZ, matrices, data.getVCP(), entityRenderDispatcher.getLight(ent, tickDelta), OverlayTexture.DEFAULT_UV, 1f);
            } catch (Exception e) {
                e.printStackTrace();
            }

            matrices.pop();
        }
    }

    @Inject(at = @At("TAIL"), method = "onResized")
    public void resizeFiguraFramebuffers(int width, int height, CallbackInfo ci) {
        if (RenderLayerAPI.lastFramebufferCopy != null)
            RenderLayerAPI.lastFramebufferCopy.resize(width, height, MinecraftClient.IS_SYSTEM_MAC);
        if (RenderLayerAPI.mainFramebufferCopy != null)
            RenderLayerAPI.mainFramebufferCopy.resize(width, height, MinecraftClient.IS_SYSTEM_MAC);
    }
}
