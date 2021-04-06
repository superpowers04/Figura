package net.blancworks.figura.mixin;

import net.blancworks.figura.PlayerData;
import net.blancworks.figura.PlayerDataManager;
import net.blancworks.figura.lua.CustomScript;
import net.blancworks.figura.lua.api.model.ItemModelAPI;
import net.blancworks.figura.lua.api.model.VanillaModelPartCustomization;
import net.blancworks.figura.trust.PlayerTrustManager;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.HeldItemFeatureRenderer;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemFeatureRenderer.class)
public class HeldItemFeatureRendererMixin {

    @Inject(at = @At("HEAD"), method = "renderItem", cancellable = true)
    public void onRenderItemStart(LivingEntity entity, ItemStack stack, ModelTransformation.Mode transformationMode, Arm arm, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        matrices.push();

        try {

            PlayerData currentData = null;
            if (entity instanceof PlayerEntity)
                currentData = PlayerDataManager.getDataForPlayer(((PlayerEntity) entity).getGameProfile().getId());
            CustomScript script = null;
            if (currentData != null) script = currentData.script;


            //Easy shortcut, null script = reset, so we can just set it to null if we don't have perms.
            if (script != null && !script.playerData.getTrustContainer().getBoolSetting(PlayerTrustManager.ALLOW_VANILLA_MOD_ID))
                script = null;

            VanillaModelPartCustomization customization = null;
            if (script != null)
                customization = script.getPartCustomization(arm == Arm.LEFT ? ItemModelAPI.VANILLA_LEFT_HAND : ItemModelAPI.VANILLA_RIGHT_HAND);

            if (customization != null && customization.visible != null && customization.visible == false) {
                matrices.pop();
                ci.cancel();
            } else {
                figura$applyCustomValueToStack(script, customization, matrices);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Inject(at = @At("RETURN"), method = "renderItem")
    public void onRenderItemEnd(LivingEntity entity, ItemStack stack, ModelTransformation.Mode transformationMode, Arm arm, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        matrices.pop();
    }

    public void figura$applyCustomValueToStack(CustomScript script, VanillaModelPartCustomization customization, MatrixStack stack) {

        //Null script = reset
        if (script == null || customization == null) {
            return;
        }

        if (customization.pos != null)
            stack.translate(customization.pos.getX(), customization.pos.getY(), customization.pos.getZ());

        if (customization.rot != null) {
            stack.multiply(Vector3f.POSITIVE_Z.getDegreesQuaternion(customization.rot.getZ()));
            stack.multiply(Vector3f.POSITIVE_Y.getDegreesQuaternion(customization.rot.getY()));
            stack.multiply(Vector3f.POSITIVE_X.getDegreesQuaternion(customization.rot.getX()));
        }

    }
}
