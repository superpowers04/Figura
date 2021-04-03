package net.blancworks.figura.mixin;

import net.blancworks.figura.PlayerData;
import net.blancworks.figura.PlayerDataManager;
import net.blancworks.figura.access.ModelPartAccess;
import net.blancworks.figura.lua.CustomScript;
import net.blancworks.figura.lua.api.model.ArmorModelAPI;
import net.blancworks.figura.lua.api.model.VanillaModelAPI;
import net.blancworks.figura.lua.api.model.VanillaModelPartCustomization;
import net.blancworks.figura.trust.PlayerTrustManager;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.PlayerScreenHandler;
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
        CustomScript script = null;
        if(currentData != null)
            script = currentData.script;


        //Easy shortcut, null script = reset, so we can just set it to null if we don't have perms.
        if(script != null && !script.playerData.getTrustContainer().getBoolSetting(PlayerTrustManager.ALLOW_VANILLA_MOD_ID))
            script = null;
        
        if (slot == EquipmentSlot.HEAD) {
            VanillaModelPartCustomization customization = null;
            if (script != null)
                customization = script.getPartCustomization(ArmorModelAPI.VANILLA_HELMET);

            figura$applyCustomValueForPart(script, customization, model.head);
        }
        if (slot == EquipmentSlot.CHEST) {
            VanillaModelPartCustomization customization = null;
            if (script != null)
                customization = script.getPartCustomization(ArmorModelAPI.VANILLA_CHESTPLATE);

            figura$applyCustomValueForPart(script, customization, model.torso);
            figura$applyCustomValueForPart(script, customization, model.rightArm);
            figura$applyCustomValueForPart(script, customization, model.leftArm);
        }
        if (slot == EquipmentSlot.LEGS) {
            VanillaModelPartCustomization customization = null;
            if (script != null)
                customization = script.getPartCustomization(ArmorModelAPI.VANILLA_LEGGINGS);

            figura$applyCustomValueForPart(script, customization, model.torso);
            figura$applyCustomValueForPart(script, customization, model.rightLeg);
            figura$applyCustomValueForPart(script, customization, model.leftLeg);
        }
        if (slot == EquipmentSlot.FEET) {
            VanillaModelPartCustomization customization = null;
            if (script != null)
                customization = script.getPartCustomization(ArmorModelAPI.VANILLA_BOOTS);

            figura$applyCustomValueForPart(script, customization, model.rightLeg);
            figura$applyCustomValueForPart(script, customization, model.leftLeg);
        }
        
    }

    public void figura$applyCustomValueForPart(CustomScript script, VanillaModelPartCustomization customization, ModelPart part) {
        ModelPartAccess mpa = (ModelPartAccess) part;

        //Null script = reset
        if (script == null) {
            mpa.setAdditionalPos(new Vector3f());
            mpa.setAdditionalRot(new Vector3f());
            return;
        }

        //No customization = reset
        if (customization == null) {
            mpa.setAdditionalPos(new Vector3f());
            mpa.setAdditionalRot(new Vector3f());
            return;
        }

        mpa.setAdditionalPos(customization.pos);
        mpa.setAdditionalRot(customization.rot);
        if (customization.visible != null)
            part.visible = customization.visible;
    }

}
