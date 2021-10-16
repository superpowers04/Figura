package net.blancworks.figura.mixin;

import net.blancworks.figura.FiguraMod;
import net.blancworks.figura.PlayerData;
import net.blancworks.figura.access.PlayerEntityModelAccess;
import net.blancworks.figura.lua.api.model.VanillaModelAPI;
import net.blancworks.figura.lua.api.model.VanillaModelPartCustomization;
import net.blancworks.figura.trust.PlayerTrustManager;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.CapeFeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.client.util.math.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(CapeFeatureRenderer.class)
public class CapeFeatureRendererMixin extends FeatureRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {

    public CapeFeatureRendererMixin(FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> context) {
        super(context);
    }

    @Unique private boolean prevEnabled;
    @Unique private int figura$pushedMatrixCount = 0;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void onRender(MatrixStack matrices, VertexConsumerProvider vertexConsumerProvider, int light, AbstractClientPlayerEntity entity, float limbAngle, float limbDistance, float tickDelta, float animationProgress, float headYaw, float headPitch, CallbackInfo ci) {
        prevEnabled = ((PlayerEntityModelAccess) this.getContextModel()).getCloak().visible;
        ((PlayerEntityModelAccess) this.getContextModel()).getCloak().visible = true;
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void postRender(MatrixStack matrices, VertexConsumerProvider vertexConsumerProvider, int light, AbstractClientPlayerEntity entity, float limbAngle, float limbDistance, float tickDelta, float animationProgress, float headYaw, float headPitch, CallbackInfo ci) {
        ((PlayerEntityModelAccess) this.getContextModel()).getCloak().visible = prevEnabled;

        for (int i = 0; i < figura$pushedMatrixCount; i++)
            matrices.pop();

        figura$pushedMatrixCount = 0;
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;isInSneakingPose()Z", shift = At.Shift.AFTER), locals = LocalCapture.CAPTURE_FAILHARD, cancellable = true)
    private void afterSneakingPose(MatrixStack matrices, VertexConsumerProvider vertexConsumerProvider, int i, AbstractClientPlayerEntity entity, float var5, float var6, float h, float var8, float var9, float var10, CallbackInfo ci, ItemStack itemStack, double d, double e, double m, float n, double o, double p, float q, float r, float s, float t) {
        PlayerData data = FiguraMod.currentData;
        if (data == null || data.script == null || data.playerId.compareTo(entity.getUuid()) != 0 || !data.getTrustContainer().getBoolSetting(PlayerTrustManager.ALLOW_VANILLA_MOD_ID)) {
            return;
        }

        try {
            for (VanillaModelAPI.ModelPartTable tbl : data.script.vanillaModelPartTables) {
                if (tbl.accessor.equals(VanillaModelAPI.VANILLA_CAPE)) {
                    tbl.pivotX = 0f;
                    tbl.pivotY = 0f;
                    tbl.pivotZ = -0.125f;

                    tbl.pitch = (float) -Math.toRadians(6f + r / 2f + q);
                    tbl.yaw = (float) Math.toRadians(s / 2f);
                    tbl.roll = (float) Math.toRadians(s / 2f);

                    tbl.visible = prevEnabled;

                    break;
                }
            }

            if (data.script.allCustomizations != null) {
                VanillaModelPartCustomization customization = data.script.allCustomizations.get(VanillaModelAPI.VANILLA_CAPE);
                if (customization != null) {
                    if (customization.visible != null && !customization.visible) {
                        matrices.pop();
                        ci.cancel();
                        return;
                    }

                    matrices.push();
                    figura$pushedMatrixCount++;

                    if (customization.pos != null)
                        matrices.translate(customization.pos.getX() / 16f, customization.pos.getY() / 16f, customization.pos.getZ() / 16f);

                    if (customization.rot != null) {
                        matrices.multiply(Vector3f.POSITIVE_Z.getDegreesQuaternion(customization.rot.getZ()));
                        matrices.multiply(Vector3f.POSITIVE_Y.getDegreesQuaternion(customization.rot.getY()));
                        matrices.multiply(Vector3f.POSITIVE_X.getDegreesQuaternion(customization.rot.getX()));
                    }

                    if (customization.scale != null) {
                        Vector3f scale = customization.scale;
                        matrices.scale(scale.getX(), scale.getY(), scale.getZ());
                    }
                }
            }
        } catch (Exception ex) {
            matrices.pop();
            ex.printStackTrace();
        }
    }

    @Shadow
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, AbstractClientPlayerEntity entity, float limbAngle, float limbDistance, float tickDelta, float animationProgress, float headYaw, float headPitch) {}
}
