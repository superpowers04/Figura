package net.blancworks.figura.mixin;

import net.blancworks.figura.PlayerData;
import net.blancworks.figura.PlayerDataManager;
import net.blancworks.figura.lua.api.model.FirstPersonModelAPI;
import net.blancworks.figura.lua.api.model.VanillaModelPartCustomization;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemRenderer.class)
public abstract class HeldItemRendererMixin {

    @Unique private int figura$pushedMatrixCount = 0;

    @Inject(at = @At("HEAD"), method = "renderFirstPersonItem", cancellable = true)
    private void onRenderFirstPersonItem(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        PlayerData data = PlayerDataManager.getDataForPlayer(player.getUuid());

        if (data == null || data != PlayerDataManager.localPlayer || data.script == null || data.script.allCustomizations == null)
            return;

        try {
            VanillaModelPartCustomization customization = data.script.allCustomizations.get(hand == Hand.MAIN_HAND ? FirstPersonModelAPI.MAIN_HAND : FirstPersonModelAPI.OFF_HAND);

            if (customization == null)
                return;

            if (customization.visible != null && !customization.visible) {
                ci.cancel();
                return;
            }

            matrices.push();
            figura$pushedMatrixCount++;

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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Inject(at = @At("RETURN"), method = "renderFirstPersonItem")
    private void postRenderFirstPersonItem(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        for (int i = 0; i < figura$pushedMatrixCount; i++)
            matrices.pop();

        figura$pushedMatrixCount = 0;
    }
}
