package net.blancworks.figura.mixin;

import net.blancworks.figura.FiguraMod;
import net.blancworks.figura.PlayerData;
import net.blancworks.figura.PlayerDataManager;
import net.blancworks.figura.models.CustomModelPart;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.BufferBuilderStorage;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin {

    @Shadow
    @Final
    private MinecraftClient client;
    @Shadow
    @Final
    private BufferBuilderStorage bufferBuilders;

    @Shadow
    @Final
    private EntityRenderDispatcher entityRenderDispatcher;

    @Inject(at = @At("HEAD"), method = "renderEntity")
    private void renderEntity(Entity entity, double cameraX, double cameraY, double cameraZ, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, CallbackInfo ci) {
        if (entity instanceof AbstractClientPlayerEntity) {
            AbstractClientPlayerEntity ent = (AbstractClientPlayerEntity) entity;

            matrices.push();

            try {

                matrices.translate(-cameraX, -cameraY, -cameraZ);
                matrices.scale(-1, -1, 1);

                PlayerData data = PlayerDataManager.getDataForPlayer(ent.getUuid());
                FiguraMod.currentData = data;

                if (data != null && data.model != null && data.lastEntity != null) {
                    for (CustomModelPart part : data.model.worldParts) {
                        data.model.leftToRender = part.renderUsingAllTextures(data, matrices, new MatrixStack(), FiguraMod.vertexConsumerProvider, entityRenderDispatcher.getLight(data.lastEntity, tickDelta), OverlayTexture.DEFAULT_UV, 1.0f);
                    }
                }

                FiguraMod.clearRenderingData();
            } catch (Exception e) {
                e.printStackTrace();
            }

            matrices.pop();
        }
    }
}
