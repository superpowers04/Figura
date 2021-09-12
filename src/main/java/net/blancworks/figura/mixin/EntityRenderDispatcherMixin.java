package net.blancworks.figura.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.blancworks.figura.PlayerData;
import net.blancworks.figura.PlayerDataManager;
import net.blancworks.figura.models.CustomModelPart;
import net.blancworks.figura.trust.PlayerTrustManager;
import net.blancworks.figura.trust.TrustContainer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3f;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Predicate;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {


    @Shadow private static void renderShadow(MatrixStack matrices, VertexConsumerProvider vertexConsumers, Entity entity, float opacity, float tickDelta, WorldView world, float radius) {}
    @Shadow public abstract <E extends Entity> int getLight(E entity, float tickDelta);
    @Shadow private boolean renderHitboxes;

    @Unique private boolean renderShadowOld;
    @Shadow private boolean renderShadows;
    private final Predicate<Entity> MOUNT_DISABLED_PREDICATE = (e -> {
        if (e instanceof PlayerEntity player) {
            PlayerData data = PlayerDataManager.getDataForPlayer(player.getUuid());
            if (data != null && data.script != null) {
                TrustContainer tc = data.getTrustContainer();

                if (tc == null || !tc.getBoolSetting(PlayerTrustManager.ALLOW_VANILLA_MOD_ID))
                    return false;

                this.renderShadows = data.script.renderMountShadow;
                return !data.script.renderMount;
            }
        }
        return false;
    });

    @Redirect(method = "render", at = @At(target = "Lnet/minecraft/client/render/entity/EntityRenderer;render(Lnet/minecraft/entity/Entity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", value = "INVOKE"))
    private <T extends Entity>void render(EntityRenderer<T> entityRenderer, T entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        if (!entity.hasPassengerType(MOUNT_DISABLED_PREDICATE)) {
            entityRenderer.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
        }
    }


    @Inject(method = "render", at = @At("HEAD"))
    private <T extends Entity> void renderStart(T entity, double x, double y, double z, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        this.renderShadowOld = this.renderShadows;
    }

    @Inject(method = "render", at = @At("TAIL"))
    private <T extends Entity> void renderEnd(T entity, double x, double y, double z, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        this.renderShadows = this.renderShadowOld;
    }

    @Inject(method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/util/math/MatrixStack;pop()V",
                    shift = At.Shift.BEFORE
            )
    )
    private <E extends Entity>void renderParts(E entity, double x, double y, double z, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        if (entity instanceof PlayerEntity) {
            PlayerData data = PlayerDataManager.getDataForPlayer(entity.getUuid());

            if (data != null) {
                if (data.model != null) {
                    data.model.allParts.forEach((part) -> renderAllParts(matrices, vertexConsumers, entity, tickDelta, part));
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
