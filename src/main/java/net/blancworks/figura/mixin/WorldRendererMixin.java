package net.blancworks.figura.mixin;

import net.blancworks.figura.FiguraMod;
import net.blancworks.figura.PlayerData;
import net.blancworks.figura.access.MatrixStackAccess;
import net.blancworks.figura.models.CustomModelPart;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin {
    

    @Shadow @Final private MinecraftClient client;
    @Shadow @Final private BufferBuilderStorage bufferBuilders;

    @Inject(at = @At("HEAD"), method = "render(Lnet/minecraft/client/util/math/MatrixStack;FJZLnet/minecraft/client/render/Camera;Lnet/minecraft/client/render/GameRenderer;Lnet/minecraft/client/render/LightmapTextureManager;Lnet/minecraft/util/math/Matrix4f;)V")
    private void render(MatrixStack matrices, float tickDelta, long limitTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f matrix4f, CallbackInfo ci) {
        try {
            Entity ent = client.getCameraEntity();

            if (ent instanceof AbstractClientPlayerEntity) {
                //Setup
                EntityRenderer realRenderer = client.getEntityRenderDispatcher().getRenderer(ent);
                PlayerEntityModel<AbstractClientPlayerEntity> model = (PlayerEntityModel<AbstractClientPlayerEntity>) ((LivingEntityRenderer)realRenderer).getModel();
                FiguraMod.setRenderingMode((AbstractClientPlayerEntity) ent, bufferBuilders.getEntityVertexConsumers(), model, tickDelta, matrices);
                PlayerData playerData = FiguraMod.getCurrentData();
                
                //If playerdata
                if(playerData != null) {
                    if (playerData.model == null || playerData.texture == null || !playerData.texture.ready)
                        return;

                    if (playerData.script != null)
                        playerData.script.render(FiguraMod.deltaTime);

                    VertexConsumer vc = FiguraMod.vertexConsumerProvider.getBuffer(RenderLayer.getEntityCutout(playerData.texture.id));

                    for (CustomModelPart part : playerData.model.allParts) {
                        if (part.parentType == CustomModelPart.ParentType.WORLD) {
                            //Make shallow copy of OG stack.
                            MatrixStack tempStack = new MatrixStack();
                            ((MatrixStackAccess) matrices).copyTo(tempStack);
                            tempStack.push();

                            tempStack.translate(-camera.getPos().getX(), -camera.getPos().getY(), -camera.getPos().getZ());
                            tempStack.scale(-1,-1,1);

                            part.render(99999, tempStack, vc, client.getEntityRenderDispatcher().getLight(this.client.player, tickDelta), OverlayTexture.DEFAULT_UV);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
