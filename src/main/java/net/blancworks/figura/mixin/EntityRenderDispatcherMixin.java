package net.blancworks.figura.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.blancworks.figura.FiguraMod;
import net.blancworks.figura.PlayerData;
import net.blancworks.figura.PlayerDataManager;
import net.blancworks.figura.models.CustomModelPart;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.debug.DebugRenderer;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.util.math.*;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {


    @Shadow public abstract Quaternion getRotation();

    @Shadow public abstract <E extends Entity> int getLight(E entity, float tickDelta);

    @Shadow public abstract <T extends Entity> EntityRenderer<? super T> getRenderer(T entity);

    @Shadow private boolean renderHitboxes;

    @Inject(method = "renderHitbox", at = @At("RETURN"))
    private static void renderHitbox(MatrixStack matrices, VertexConsumer vertices, Entity entity, float tickDelta, CallbackInfo ci) {



    }

    @Inject(method = "render",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/util/math/MatrixStack;pop()V",
            shift = At.Shift.BEFORE))
    private <E extends Entity>void render(E entity, double x, double y, double z, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        if (entity instanceof PlayerEntity) {
            PlayerData data = PlayerDataManager.getDataForPlayer(entity.getUuid());

            if (data != null) {
                if (data.model != null) {
                    data.model.allParts.forEach((part) -> {
                        renderAllParts(matrices, vertexConsumers, entity, tickDelta, part);
                    });
                }
            }
        }
    }

    private void renderAllParts(MatrixStack matrices, VertexConsumerProvider vertexConsumers, Entity entity, float tickDelta, CustomModelPart part) {
        if (part.isHidden || !part.visible || !part.shouldRender) return;
        part.children.forEach((child) -> renderAllParts(matrices, vertexConsumers, entity, tickDelta, child));
        final float gridSize = 1/16f;

        Vec3f pos = part.pos.copy();

        Vec3f pivot = part.pivot.copy();
        pivot.scale(gridSize);

        Vec3f scale = part.scale.copy();
        scale.scale(gridSize);

        matrices.push();
        RenderSystem.enableBlend();
        RenderSystem.disableDepthTest();

        matrices.translate(-entity.getX(), -entity.getY(), -entity.getZ());
        matrices.method_34425(part.lastModelMatrix);

        if (this.renderHitboxes) {
            Box box = new Box(-gridSize/2,-gridSize/2,-gridSize/2,gridSize/2,gridSize/2,gridSize/2);
            WorldRenderer.drawBox(matrices, vertexConsumers.getBuffer(RenderLayer.getLines()), box, 1.0F, 1.0F, 1.0F, 1.0F);
        }


        while (!part.renderTasks.isEmpty()) {
            part.renderTasks.remove().render(((EntityRenderDispatcher) (Object) this), entity, part, matrices, matrices, vertexConsumers, getLight(entity, tickDelta), 0, 1, 1, 1, 1, MinecraftClient.getInstance());

        }


        matrices.pop();


    }


}
