package net.blancworks.figura.mixin;

import net.blancworks.figura.avatar.AvatarData;
import net.blancworks.figura.avatar.AvatarDataManager;
import net.blancworks.figura.lua.api.model.VanillaModelAPI;
import net.blancworks.figura.lua.api.model.VanillaModelPartCustomization;
import net.blancworks.figura.trust.TrustContainer;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.Deadmau5FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Deadmau5FeatureRenderer.class)
public class Deadmau5FeatureRendererMixin extends FeatureRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {

    public Deadmau5FeatureRendererMixin(FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> context) {
        super(context);
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/LivingEntityRenderer;getOverlay(Lnet/minecraft/entity/LivingEntity;F)I", shift = At.Shift.AFTER), cancellable = true)
    private void afterGetOverlay(MatrixStack matrices, VertexConsumerProvider vertexConsumerProvider, int light, AbstractClientPlayerEntity entity, float limbAngle, float limbDistance, float tickDelta, float animationProgress, float headYaw, float headPitch, CallbackInfo ci) {
        AvatarData data = AvatarDataManager.getDataForPlayer(entity.getUuid());
        if (data == null || data.script == null || data.getTrustContainer().getTrust(TrustContainer.Trust.VANILLA_MODEL_EDIT) == 0)
            return;

        boolean prevEnabled = ((PlayerEntityModelAccessorMixin) this.getContextModel()).getEar().visible;
        ((PlayerEntityModelAccessorMixin) this.getContextModel()).getEar().visible = true;

        try {
            VertexConsumer vertexConsumer = vertexConsumerProvider.getBuffer(RenderLayer.getEntitySolid(entity.getSkinTexture()));
            int m = LivingEntityRenderer.getOverlay(entity, 0f);
            float o = MathHelper.lerp(tickDelta, entity.prevYaw, entity.getYaw()) - MathHelper.lerp(tickDelta, entity.prevBodyYaw, entity.bodyYaw);
            float p = MathHelper.lerp(tickDelta, entity.prevPitch, entity.getPitch());
            float q = 1.3333f;

            for (int i = 0; i < 2; ++i) {
                String id = i == 1 ? VanillaModelAPI.VANILLA_LEFT_EAR : VanillaModelAPI.VANILLA_RIGHT_EAR;
                matrices.push();

                //save part data
                for (VanillaModelAPI.ModelPartTable tbl : data.script.vanillaModelPartTables) {
                    if (tbl.accessor.equals(id)) {
                        tbl.pivotX = 0.375f * (i * 2 - 1);
                        tbl.pivotY = -0.375f;
                        tbl.pivotZ = 0f;

                        tbl.pitch = (float) Math.toRadians(p);
                        tbl.yaw = (float) Math.toRadians(o);
                        tbl.roll = 0f;

                        tbl.visible = prevEnabled;

                        break;
                    }
                }

                //apply custom data
                if (data.script.allCustomizations != null) {
                    VanillaModelPartCustomization customization = data.script.allCustomizations.get(id);
                    if (customization != null) {
                        if (customization.visible != null && !customization.visible) {
                            matrices.pop();
                            continue;
                        }

                        if (customization.pos != null)
                            matrices.translate(customization.pos.getX() / 16f, customization.pos.getY() / 16f, customization.pos.getZ() / 16f);

                        if (customization.rot != null) {
                            matrices.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(customization.rot.getZ()));
                            matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(customization.rot.getY()));
                            matrices.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(customization.rot.getX()));
                        }

                        if (customization.scale != null) {
                            Vec3f scale = customization.scale;
                            matrices.scale(scale.getX(), scale.getY(), scale.getZ());
                        }
                    }
                }

                //apply vanilla data
                matrices.push();
                matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(o));
                matrices.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(p));
                matrices.translate(0.375f * (float) (i * 2 - 1), 0f, 0f);
                matrices.translate(0f, -0.375f, 0f);
                matrices.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(-p));
                matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(-o));
                matrices.scale(q, q, q);
                this.getContextModel().renderEars(matrices, vertexConsumer, light, m);
                matrices.pop();

                matrices.pop();
            }

            ci.cancel();
        } catch (Exception e) {
            e.printStackTrace();
        }

        ((PlayerEntityModelAccessorMixin) this.getContextModel()).getEar().visible = prevEnabled;
    }

    @Shadow
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, AbstractClientPlayerEntity entity, float limbAngle, float limbDistance, float tickDelta, float animationProgress, float headYaw, float headPitch) {}
}
