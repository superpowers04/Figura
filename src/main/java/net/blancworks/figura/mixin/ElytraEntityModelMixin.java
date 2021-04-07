package net.blancworks.figura.mixin;

import net.blancworks.figura.PlayerData;
import net.blancworks.figura.PlayerDataManager;
import net.blancworks.figura.access.ModelPartAccess;
import net.blancworks.figura.lua.CustomScript;
import net.blancworks.figura.lua.api.model.ElytraModelAPI;
import net.blancworks.figura.lua.api.model.VanillaModelPartCustomization;
import net.blancworks.figura.trust.PlayerTrustManager;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.AnimalModel;
import net.minecraft.client.render.entity.model.ElytraEntityModel;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ElytraEntityModel.class)
public class ElytraEntityModelMixin<T extends LivingEntity> extends AnimalModel<T> {

    @Shadow @Final private ModelPart field_3365;
    @Shadow @Final private ModelPart field_3364;

    @Inject(at = @At("RETURN"), method = "setAngles(Lnet/minecraft/entity/LivingEntity;FFFFF)V")
    public void setAngles(T livingEntity, float f, float g, float h, float i, float j, CallbackInfo ci) {
        try {
            ModelPart rightWing = field_3364;
            ModelPart leftWing = field_3365;

            PlayerData currentData = null;
            if (livingEntity instanceof PlayerEntity)
                currentData = PlayerDataManager.getDataForPlayer(((PlayerEntity) livingEntity).getGameProfile().getId());
            CustomScript script = null;
            if (currentData != null) script = currentData.script;


            //Easy shortcut, null script = reset, so we can just set it to null if we don't have perms.
            if (script != null && !script.playerData.getTrustContainer().getBoolSetting(PlayerTrustManager.ALLOW_VANILLA_MOD_ID))
                script = null;

            VanillaModelPartCustomization customization = null;
            if (script != null) customization = script.getPartCustomization(ElytraModelAPI.VANILLA_RIGHT_WING);
            figura$applyCustomValueForPart(script, customization, rightWing);

            customization = null;
            if (script != null) customization = script.getPartCustomization(ElytraModelAPI.VANILLA_LEFT_WING);
            figura$applyCustomValueForPart(script, customization, leftWing);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    @Shadow
    protected Iterable<ModelPart> getHeadParts() { return null; }

    @Override
    @Shadow
    protected Iterable<ModelPart> getBodyParts() { return null; }

    @Override
    @Shadow
    public void setAngles(T entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) { }

    public void figura$applyCustomValueForPart(CustomScript script, VanillaModelPartCustomization customization, ModelPart part) {
        ModelPartAccess mpa = (ModelPartAccess) part;

        //Null script = reset
        if (script == null) {
            mpa.setAdditionalPos(new Vector3f());
            mpa.setAdditionalRot(new Vector3f());
            part.visible = true;
            return;
        }

        //No customization = reset
        if (customization == null) {
            mpa.setAdditionalPos(new Vector3f());
            mpa.setAdditionalRot(new Vector3f());
            part.visible = true;
            return;
        }

        mpa.setAdditionalPos(customization.pos);
        mpa.setAdditionalRot(customization.rot);
        if (customization.visible != null)
            part.visible = customization.visible;
    }
}
