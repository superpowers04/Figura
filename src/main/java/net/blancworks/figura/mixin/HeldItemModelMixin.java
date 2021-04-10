package net.blancworks.figura.mixin;

import net.blancworks.figura.FiguraMod;
import net.blancworks.figura.PlayerData;
import net.blancworks.figura.access.ModelPartAccess;
import net.blancworks.figura.lua.api.model.ItemModelAPI;
import net.blancworks.figura.lua.api.model.VanillaModelPartCustomization;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.feature.HeldItemFeatureRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.ModelWithArms;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemFeatureRenderer.class)
public class HeldItemModelMixin<T extends LivingEntity, M extends EntityModel<T> & ModelWithArms> extends FeatureRenderer<T, M> {

    public HeldItemModelMixin(FeatureRendererContext<T, M> context) {
        super(context);
    }

    public VanillaModelPartCustomization customization;

    @Inject(at = @At("HEAD"), method = "renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformation$Mode;Lnet/minecraft/util/Arm;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", cancellable = true)
    private void onRenderItem(LivingEntity entity, ItemStack stack, ModelTransformation.Mode transformationMode, Arm arm, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        PlayerData data = FiguraMod.currentData;

        try {

            System.out.println("TEST");

            if (data != null && data.script != null && data.script.allCustomizations != null) {
                customization = data.script.allCustomizations.get(arm == Arm.LEFT ? ItemModelAPI.VANILLA_LEFT_HAND : ItemModelAPI.VANILLA_RIGHT_HAND);
                if (customization != null) {
                    if (customization.visible != null) {
                        if (customization.visible == false) {
                            ci.cancel();
                            return;
                        }
                    }
                    
                    matrices.push();

                    matrices.translate(customization.pos.getX() / 16.0f, customization.pos.getY()/ 16.0f, customization.pos.getZ()/ 16.0f);
                    matrices.multiply(Vector3f.POSITIVE_Z.getDegreesQuaternion(customization.rot.getZ()));
                    matrices.multiply(Vector3f.POSITIVE_Y.getDegreesQuaternion(customization.rot.getY()));
                    matrices.multiply(Vector3f.POSITIVE_X.getDegreesQuaternion(customization.rot.getX()));
                }
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    @Inject(at = @At("RETURN"), method = "renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformation$Mode;Lnet/minecraft/util/Arm;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V")
    private void postRenderItem(LivingEntity entity, ItemStack stack, ModelTransformation.Mode transformationMode, Arm arm, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        PlayerData data = FiguraMod.currentData;

        if (data != null && data.script != null && data.script.allCustomizations != null) {
            customization = data.script.allCustomizations.get(arm == Arm.LEFT ? ItemModelAPI.VANILLA_LEFT_HAND : ItemModelAPI.VANILLA_RIGHT_HAND);

            if (customization != null) {
                matrices.pop();
            }
        }
    }

    @Shadow
    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, T entity, float limbAngle, float limbDistance, float tickDelta, float animationProgress, float headYaw, float headPitch) {
    
    }
}
