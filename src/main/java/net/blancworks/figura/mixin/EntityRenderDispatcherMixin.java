package net.blancworks.figura.mixin;

import net.blancworks.figura.Config;
import net.blancworks.figura.FiguraMod;
import net.blancworks.figura.PlayerData;
import net.blancworks.figura.PlayerDataManager;
import net.blancworks.figura.trust.PlayerTrustManager;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Predicate;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {

    @Shadow private boolean renderShadows;
    private boolean renderShadowOld;

    private final Predicate<Entity> MOUNT_DISABLED_PREDICATE = (entity -> {
        if (entity instanceof PlayerEntity player) {
            PlayerData data = PlayerDataManager.getDataForPlayer(player.getUuid());
            if (data != null && data.script != null && data.getTrustContainer().getBoolSetting(PlayerTrustManager.ALLOW_VANILLA_MOD_ID)) {
                this.renderShadows = data.script.renderMountShadow;
                return !data.script.renderMount;
            }
        }
        return false;
    });

    @Inject(at = @At("HEAD"), method = "render")
    public <E extends Entity> void render(E entity, double x, double y, double z, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        renderShadowOld = this.renderShadows;

        if (entity instanceof PlayerEntity) {
            FiguraMod.renderDispatcher = (EntityRenderDispatcher) (Object) this;
        }
    }

    @Redirect(method = "render", at = @At(target = "Lnet/minecraft/client/render/entity/EntityRenderer;render(Lnet/minecraft/entity/Entity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", value = "INVOKE"))
    private <T extends Entity>void renderRenderEntity(EntityRenderer<T> entityRenderer, T entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        if (!entity.hasPassengerType(MOUNT_DISABLED_PREDICATE)) {
            entityRenderer.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private <T extends Entity> void renderEnd(T entity, double x, double y, double z, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        this.renderShadows = renderShadowOld;
    }

    @Inject(at = @At("HEAD"), method = "renderHitbox")
    private static void renderHitbox(MatrixStack matrices, VertexConsumer vertices, Entity entity, float tickDelta, CallbackInfo ci) {
        if (entity instanceof PlayerEntity player && (boolean) Config.entries.get("partsHitBox").value) {

            PlayerData data = PlayerDataManager.getDataForPlayer(player.getUuid());
            if (data == null || data.model == null) return;

            FiguraMod.currentData = data;
            data.model.allParts.forEach(part -> part.renderPivotHitbox(matrices, vertices, 1 / 32f));
        }
    }
}
