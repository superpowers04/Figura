package net.blancworks.figura.mixin;

import net.blancworks.figura.PlayerData;
import net.blancworks.figura.PlayerDataManager;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ArmorFeatureRenderer.class)
public abstract class ArmorFeatureRendererMixin<T extends LivingEntity, M extends BipedEntityModel<T>, A extends BipedEntityModel<T>>
        extends FeatureRenderer<T, M> {
    private PlayerData figura$currentData;

    public ArmorFeatureRendererMixin(FeatureRendererContext<T, M> context) {
        super(context);
    }

    @Shadow
    public abstract void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, T entity,
                                float limbAngle, float limbDistance, float tickDelta, float animationProgress,
                                float headYaw, float headPitch);

    @Inject(at = @At("HEAD"), method = "render")
    public void onRenderStart(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, T entity,
                              float f, float g, float h, float j, float k, float l, CallbackInfo ci) {
        if (entity instanceof PlayerEntity) {
            figura$currentData = PlayerDataManager.getDataForPlayer(((PlayerEntity) entity).getGameProfile().getId());
        }
    }

    @Inject(at = @At("RETURN"), method = "render")
    public void onRenderEnd(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, T entity,
                            float f, float g, float h, float j, float k, float l, CallbackInfo ci) {
        figura$currentData = null;
    }

    @Inject(at = @At("RETURN"), method = "setVisible")
    protected void setVisible(A model, EquipmentSlot slot, CallbackInfo ci) {
        PlayerData currentData = figura$currentData;

        if (currentData == null)
            return;

        if (currentData.script != null && currentData.script.vanillaModifications != null) {

            if (slot == EquipmentSlot.HEAD)
                currentData.script.applyArmorValues(model, 12);
            if (slot == EquipmentSlot.CHEST)
                currentData.script.applyArmorValues(model, 13);
            if (slot == EquipmentSlot.LEGS)
                currentData.script.applyArmorValues(model, 14);
            if (slot == EquipmentSlot.FEET)
                currentData.script.applyArmorValues(model, 15);
        }
    }
}
