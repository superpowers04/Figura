package net.blancworks.figura.mixin;

import net.blancworks.figura.FiguraMod;
import net.blancworks.figura.PlayerData;
import net.blancworks.figura.access.MatrixStackAccess;
import net.blancworks.figura.lua.api.model.ItemModelAPI;
import net.blancworks.figura.lua.api.model.VanillaModelPartCustomization;
import net.blancworks.figura.models.CustomModelPart;
import net.blancworks.figura.trust.PlayerTrustManager;
import net.blancworks.figura.trust.TrustContainer;
import net.minecraft.client.MinecraftClient;
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

    public VanillaModelPartCustomization figura$customization;
    
    private int figura$pushedMatrixCount = 0;

    @Inject(at = @At("HEAD"), cancellable = true, method = "renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformation$Mode;Lnet/minecraft/util/Arm;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V")
    private void onRenderItem(LivingEntity entity, ItemStack stack, ModelTransformation.Mode transformationMode, Arm arm, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        PlayerData data = FiguraMod.currentData;
        
        if(data == null)
            return;

        TrustContainer tc = data.getTrustContainer();

        if (tc == null || !tc.getBoolSetting(PlayerTrustManager.ALLOW_VANILLA_MOD_ID))
            return;
        
        try {
            if (data.model != null) {
                VanillaModelPartCustomization originModification = arm == Arm.LEFT ? data.model.originModifications.get(ItemModelAPI.VANILLA_LEFT_HAND_ID) : data.model.originModifications.get(ItemModelAPI.VANILLA_RIGHT_HAND_ID);

                if (originModification != null) {
                    if (originModification.part == null || !originModification.part.visible || data.model.lastComplexity >= data.getTrustContainer().getFloatSetting(PlayerTrustManager.MAX_COMPLEXITY_ID)) {
                        ci.cancel();
                        return;
                    }

                    if (originModification.stackReference != null) {
                        figura$CustomOriginPointRender(entity, stack, transformationMode, arm, originModification.stackReference, vertexConsumers, light);
                        ci.cancel();
                        return;
                    }
                }
            }
        } catch (Exception e){
            e.printStackTrace();
        }

        try {
            if (data.script != null && data.script.allCustomizations != null) {
                figura$customization = data.script.allCustomizations.get(arm == Arm.LEFT ? ItemModelAPI.VANILLA_LEFT_HAND : ItemModelAPI.VANILLA_RIGHT_HAND);
                if (figura$customization != null) {
                    if (figura$customization.visible != null && !figura$customization.visible) {
                        ci.cancel();
                        return;
                    }

                    matrices.push();
                    figura$pushedMatrixCount++;

                    if (figura$customization.pos != null)
                        matrices.translate(figura$customization.pos.getX() / 16.0f, figura$customization.pos.getY() / 16.0f, figura$customization.pos.getZ() / 16.0f);

                    if (figura$customization.rot != null) {
                        matrices.multiply(Vector3f.POSITIVE_Z.getDegreesQuaternion(figura$customization.rot.getZ()));
                        matrices.multiply(Vector3f.POSITIVE_Y.getDegreesQuaternion(figura$customization.rot.getY()));
                        matrices.multiply(Vector3f.POSITIVE_X.getDegreesQuaternion(figura$customization.rot.getX()));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Inject(at = @At("RETURN"), method = "renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformation$Mode;Lnet/minecraft/util/Arm;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V")
    private void postRenderItem(LivingEntity entity, ItemStack stack, ModelTransformation.Mode transformationMode, Arm arm, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        
        for(int i = 0; i < figura$pushedMatrixCount; i++)
            matrices.pop();
        
        figura$pushedMatrixCount = 0;
    }

    @Shadow
    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, T entity, float limbAngle, float limbDistance, float tickDelta, float animationProgress, float headYaw, float headPitch) {

    }

    private void figura$CustomOriginPointRender(LivingEntity entity, ItemStack stack, ModelTransformation.Mode transformationMode, Arm arm, MatrixStack.Entry modified, VertexConsumerProvider vertexConsumers, int light) {
        if (!stack.isEmpty()) {
            MatrixStack freshStack = new MatrixStack();
            MatrixStackAccess access = (MatrixStackAccess) (Object) freshStack;
            access.pushEntry(modified);
            boolean bl = arm == Arm.LEFT;
            MinecraftClient.getInstance().getHeldItemRenderer().renderItem(entity, stack, transformationMode, bl, freshStack, vertexConsumers, light);
        }
    }

}
