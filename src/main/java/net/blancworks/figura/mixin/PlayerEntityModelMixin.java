package net.blancworks.figura.mixin;

import net.blancworks.figura.FiguraMod;
import net.blancworks.figura.PlayerData;
import net.blancworks.figura.PlayerDataManager;
import net.blancworks.figura.access.ModelPartAccess;
import net.blancworks.figura.access.PlayerEntityModelAccess;
import net.blancworks.figura.lua.CustomScript;
import net.blancworks.figura.lua.api.model.VanillaModelAPI;
import net.blancworks.figura.lua.api.model.VanillaModelPartCustomization;
import net.blancworks.figura.models.FiguraTexture;
import net.blancworks.figura.trust.PlayerTrustManager;
import net.blancworks.figura.trust.TrustContainer;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.net.ssl.TrustManager;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

@Mixin(PlayerEntityModel.class)
public class PlayerEntityModelMixin<T extends LivingEntity> extends BipedEntityModel<T> implements PlayerEntityModelAccess {

    @Shadow
    @Final
    public ModelPart jacket;
    @Shadow
    @Final
    public ModelPart leftPantLeg;
    @Shadow
    @Final
    public ModelPart leftSleeve;
    @Shadow
    @Final
    public ModelPart rightSleeve;
    @Shadow
    @Final
    public ModelPart rightPantLeg;
    @Shadow
    @Final
    private ModelPart ears;
    @Shadow
    private List<ModelPart> parts;

    private final Set<ModelPart> figura$disabledParts = new HashSet<>();

    public PlayerEntityModelMixin(float scale) {
        super(scale);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, float red, float green, float blue, float alpha) {
        try {
            PlayerEntityModel<T> self = (PlayerEntityModel<T>) (Object) this;
            PlayerData playerData = FiguraMod.getCurrentData();
            if (playerData == null) {
                super.render(matrices, vertices, light, overlay, red, green, blue, alpha);
                return;
            }

            PlayerDataManager.checkForPlayerDataRefresh(playerData);
            TrustContainer trustData = playerData.getTrustContainer();

            for (ModelPart part : parts) {
                ModelPartAccess mpa = (ModelPartAccess) part;
                mpa.setAdditionalPos(new Vector3f());
                mpa.setAdditionalRot(new Vector3f());
            }
            
            figura$setupCustomValuesFromScript(playerData.script);
            
            //Render vanilla model.
            super.render(matrices, vertices, light, overlay, red, green, blue, alpha);

            if (playerData.model != null) {
                if (playerData.texture == null || !playerData.texture.ready) {
                    return;
                }
                //We actually wanna use this custom vertex consumer, not the one provided by the render arguments.
                VertexConsumer actualConsumer = FiguraMod.vertexConsumerProvider.getBuffer(RenderLayer.getEntityCutoutNoCull(playerData.texture.id));
                playerData.model.render(self, matrices, actualConsumer, light, overlay, red, green, blue, alpha);

                for (int i = 0; i < playerData.extraTextures.size(); i++) {
                    FiguraTexture texture = playerData.extraTextures.get(i);

                    if (!texture.ready) {
                        continue;
                    }

                    Function<Identifier, RenderLayer> renderLayerGetter = FiguraTexture.EXTRA_TEXTURE_TO_RENDER_LAYER.get(texture.type);

                    if (renderLayerGetter != null) {
                        actualConsumer = FiguraMod.vertexConsumerProvider.getBuffer(renderLayerGetter.apply(texture.id));
                        playerData.model.render(self, matrices, actualConsumer, light, overlay, red, green, blue, alpha);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Inject(at = @At("TAIL"), method = "setVisible(Z)V")
    public void setVisible(boolean visible, CallbackInfo ci) {
        for (ModelPart part : this.figura$disabledParts) {
            part.visible = false;
            if (part == helmet)
                ears.visible = false;
        }
    }

    @Override
    public Set<ModelPart> figura$getDisabledParts() {
        return this.figura$disabledParts;
    }
    
    
    //Applies all the values from a custom script's customizations to this model.
    @Override
    public void figura$setupCustomValuesFromScript(CustomScript script){
        figura$disabledParts.clear();

        //Easy shortcut, null script = reset, so we can just set it to null if we don't have perms.
        if(script != null && !script.playerData.getTrustContainer().getBoolSetting(PlayerTrustManager.ALLOW_VANILLA_MOD_ID))
            script = null;
        
        //Main body reset
        figura$applyCustomValueForPart(script, VanillaModelAPI.VANILLA_HEAD, head);
        figura$applyCustomValueForPart(script, VanillaModelAPI.VANILLA_TORSO, torso);
        figura$applyCustomValueForPart(script, VanillaModelAPI.VANILLA_LEFT_ARM, leftArm);
        figura$applyCustomValueForPart(script, VanillaModelAPI.VANILLA_RIGHT_ARM, rightArm);
        figura$applyCustomValueForPart(script, VanillaModelAPI.VANILLA_LEFT_LEG, leftLeg);
        figura$applyCustomValueForPart(script, VanillaModelAPI.VANILLA_RIGHT_LEG, rightLeg);

        //Layer reset
        figura$applyCustomValueForPart(script, VanillaModelAPI.VANILLA_HAT, helmet);
        figura$applyCustomValueForPart(script, VanillaModelAPI.VANILLA_JACKET, jacket);
        figura$applyCustomValueForPart(script, VanillaModelAPI.VANILLA_LEFT_SLEEVE, leftSleeve);
        figura$applyCustomValueForPart(script, VanillaModelAPI.VANILLA_RIGHT_SLEEVE, rightSleeve);
        figura$applyCustomValueForPart(script, VanillaModelAPI.VANILLA_LEFT_PANTS, leftPantLeg);
        figura$applyCustomValueForPart(script, VanillaModelAPI.VANILLA_RIGHT_PANTS, rightPantLeg);
    }

    @Override
    public void figura$applyCustomValueForPart(CustomScript script, String accessor, ModelPart part){
        ModelPartAccess mpa = (ModelPartAccess) part;
        
        //Null script = reset
        if(script == null){
            mpa.setAdditionalPos(new Vector3f());
            mpa.setAdditionalRot(new Vector3f());
            return;
        }
        VanillaModelPartCustomization customization = script.getPartCustomization(accessor);
        
        //No customization = reset
        if(customization == null) {
            mpa.setAdditionalPos(new Vector3f());
            mpa.setAdditionalRot(new Vector3f());
            return;
        }

        mpa.setAdditionalPos(customization.pos);
        mpa.setAdditionalRot(customization.rot);
        if(customization.visible != null)
            figura$disabledParts.add(part);
    }
}
