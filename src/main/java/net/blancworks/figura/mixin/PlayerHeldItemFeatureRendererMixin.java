package net.blancworks.figura.mixin;

import net.blancworks.figura.FiguraMod;
import net.blancworks.figura.PlayerData;
import net.blancworks.figura.access.MatrixStackAccess;
import net.blancworks.figura.lua.api.model.SpyglassModelAPI;
import net.blancworks.figura.lua.api.model.VanillaModelPartCustomization;
import net.blancworks.figura.trust.PlayerTrustManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.PlayerHeldItemFeatureRenderer;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.math.Vec3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerHeldItemFeatureRenderer.class)
public class PlayerHeldItemFeatureRendererMixin {

    public VanillaModelPartCustomization figura$customization;
    private int figura$pushedMatrixCount = 0;

    @Inject(method = "renderSpyglass", at = @At(value = "HEAD"), cancellable = true)
    public void renderSpyglass(LivingEntity entity, ItemStack stack, Arm arm, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        PlayerData data = FiguraMod.currentData;

        if (data == null || data.playerId.compareTo(entity.getUuid()) != 0 || !data.getTrustContainer().getBoolSetting(PlayerTrustManager.ALLOW_VANILLA_MOD_ID))
            return;

        boolean left = arm == Arm.LEFT;
        try {
            if (data.model != null) {
                VanillaModelPartCustomization originModification = left ? data.model.originModifications.get(SpyglassModelAPI.VANILLA_LEFT_SPYGLASS_ID) : data.model.originModifications.get(SpyglassModelAPI.VANILLA_RIGHT_SPYGLASS_ID);

                if (originModification != null) {
                    if (originModification.part == null || originModification.visible == null || data.model.lastComplexity >= data.getTrustContainer().getFloatSetting(PlayerTrustManager.MAX_COMPLEXITY_ID)) {
                        ci.cancel();
                        return;
                    }

                    if (originModification.stackReference != null) {
                        //apply modifications
                        MatrixStack freshStack = new MatrixStack();
                        MatrixStackAccess access = (MatrixStackAccess) freshStack;
                        access.pushEntry(originModification.stackReference);

                        //flag to not render anymore
                        originModification.visible = null;

                        //render :3
                        freshStack.push();
                        freshStack.translate(0f, 0f, 4.5f / 16f);
                        freshStack.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(180f));
                        freshStack.scale(0.625f, -0.625f, -0.625f);
                        MinecraftClient.getInstance().getHeldItemRenderer().renderItem(entity, stack, ModelTransformation.Mode.HEAD, left, freshStack, vertexConsumers, light);
                        freshStack.pop();

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
                figura$customization = data.script.allCustomizations.get(left ? SpyglassModelAPI.VANILLA_LEFT_SPYGLASS : SpyglassModelAPI.VANILLA_RIGHT_SPYGLASS);
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
                        matrices.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(figura$customization.rot.getZ()));
                        matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(figura$customization.rot.getY()));
                        matrices.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(figura$customization.rot.getX()));
                    }

                    if (figura$customization.scale != null) {
                        Vec3f scale = figura$customization.scale;
                        matrices.scale(scale.getX(), scale.getY(), scale.getZ());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Inject(at = @At("RETURN"), method = "renderSpyglass")
    public void postRenderSpyglass(LivingEntity entity, ItemStack stack, Arm arm, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        for (int i = 0; i < figura$pushedMatrixCount; i++)
            matrices.pop();

        figura$pushedMatrixCount = 0;
    }
}
