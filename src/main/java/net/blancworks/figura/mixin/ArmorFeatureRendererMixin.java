package net.blancworks.figura.mixin;

import net.blancworks.figura.avatar.AvatarData;
import net.blancworks.figura.avatar.AvatarDataManager;
import net.blancworks.figura.access.ModelPartAccess;
import net.blancworks.figura.lua.api.model.ArmorModelAPI;
import net.blancworks.figura.lua.api.model.VanillaModelPartCustomization;
import net.blancworks.figura.trust.TrustContainer;
import net.minecraft.client.model.ModelPart;
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

import java.util.ArrayList;
import java.util.HashMap;

@Mixin(ArmorFeatureRenderer.class)
public class ArmorFeatureRendererMixin<T extends LivingEntity, M extends BipedEntityModel<T>, A extends BipedEntityModel<T>> extends FeatureRenderer<T, M>{
    public ArmorFeatureRendererMixin(FeatureRendererContext<T, M> context) {
        super(context);
    }

    private final ArrayList<ModelPart> figura$customizedParts = new ArrayList<>();
    private final HashMap<EquipmentSlot, String> partMap = new HashMap<>();

    @Inject(at = @At("HEAD"), method = "renderArmor")
    private void onRenderArmor(MatrixStack matrices, VertexConsumerProvider vertexConsumers, T livingEntity, EquipmentSlot equipmentSlot, int i, A bipedEntityModel, CallbackInfo ci) {
        if (partMap.isEmpty()) {
            partMap.put(EquipmentSlot.HEAD, ArmorModelAPI.VANILLA_HELMET);
            partMap.put(EquipmentSlot.CHEST, ArmorModelAPI.VANILLA_CHESTPLATE);
            partMap.put(EquipmentSlot.LEGS, ArmorModelAPI.VANILLA_LEGGINGS);
            partMap.put(EquipmentSlot.FEET, ArmorModelAPI.VANILLA_BOOTS);
        }

        String partID = partMap.get(equipmentSlot);

        if (partID != null) {
            AvatarData data = livingEntity instanceof PlayerEntity ? AvatarDataManager.getDataForPlayer(livingEntity.getUuid()) : AvatarDataManager.getDataForEntity(livingEntity);

            if (data != null && data.getTrustContainer().getTrust(TrustContainer.Trust.VANILLA_MODEL_EDIT) == 1) {
                figura$applyPartCustomization(partID, bipedEntityModel.head, data);
                figura$applyPartCustomization(partID, bipedEntityModel.hat, data);
                figura$applyPartCustomization(partID, bipedEntityModel.body, data);
                figura$applyPartCustomization(partID, bipedEntityModel.leftArm, data);
                figura$applyPartCustomization(partID, bipedEntityModel.leftLeg, data);
                figura$applyPartCustomization(partID, bipedEntityModel.rightArm, data);
                figura$applyPartCustomization(partID, bipedEntityModel.rightLeg, data);
            }
        }
    }

    @Inject(at = @At("RETURN"), method = "renderArmor")
    private void postRenderArmor(MatrixStack matrices, VertexConsumerProvider vertexConsumers, T livingEntity, EquipmentSlot equipmentSlot, int i, A bipedEntityModel, CallbackInfo ci) {
        figura$clearAllPartCustomizations();
    }

    @Shadow
    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, T entity, float limbAngle, float limbDistance, float tickDelta, float animationProgress, float headYaw, float headPitch) {}

    public void figura$applyPartCustomization(String id, ModelPart part, AvatarData data) {
        if (data != null && data.script != null && data.script.allCustomizations != null) {
            VanillaModelPartCustomization customization = data.script.allCustomizations.get(id);

            if (customization != null) {
                ((ModelPartAccess) (Object) part).figura$setPartCustomization(customization);
                figura$customizedParts.add(part);
            }
        }
    }

    public void figura$clearAllPartCustomizations() {
        for (ModelPart part : figura$customizedParts) {
            ((ModelPartAccess) (Object) part).figura$setPartCustomization(null);
        }

        figura$customizedParts.clear();
    }
}
