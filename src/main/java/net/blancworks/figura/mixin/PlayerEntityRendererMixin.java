package net.blancworks.figura.mixin;

import net.blancworks.figura.FiguraMod;
import net.blancworks.figura.PlayerData;
import net.blancworks.figura.access.PlayerEntityModelAccess;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererMixin
        extends LivingEntityRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {
    PlayerEntityRendererMixin(EntityRenderDispatcher dispatcher, PlayerEntityModel<AbstractClientPlayerEntity> model, float shadowRadius) {
        super(dispatcher, model, shadowRadius);
    }

    @Inject(at = @At("HEAD"), method = "render")
    public void onRenderStart(AbstractClientPlayerEntity player, float f, float g, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int i, CallbackInfo ci) {
        FiguraMod.setRenderingMode(player, vertexConsumers, this.getModel(), g, matrices);
    }

    @Inject(at = @At("HEAD"), method = "renderArm", cancellable = true)
    private void onRenderArmStart(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, AbstractClientPlayerEntity player, ModelPart arm, ModelPart sleeve, CallbackInfo info) {
        FiguraMod.setRenderingMode(player, vertexConsumers, this.getModel(), MinecraftClient.getInstance().getTickDelta(), matrices);
        PlayerData playerData = FiguraMod.getCurrentData();
        PlayerEntityRenderer realRenderer = (PlayerEntityRenderer) (Object) this;
        PlayerEntityModel<AbstractClientPlayerEntity> model = realRenderer.getModel();

        if (playerData.script != null) {
            playerData.script.render(FiguraMod.deltaTime);
        }
        
        PlayerEntityModelAccess access = (PlayerEntityModelAccess) model;
        access.figura$setupCustomValuesFromScript(playerData.script);
    }

    @Inject(at = @At("RETURN"), method = "renderArm", cancellable = true)
    private void onRenderArmEnd(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, AbstractClientPlayerEntity player, ModelPart arm, ModelPart sleeve, CallbackInfo info) {
        PlayerEntityRenderer realRenderer = (PlayerEntityRenderer) (Object) this;
        PlayerEntityModel<AbstractClientPlayerEntity> model = realRenderer.getModel();
        FiguraMod.setRenderingMode(player, vertexConsumers, model, MinecraftClient.getInstance().getTickDelta(), matrices);
        PlayerData playerData = FiguraMod.getCurrentData();

        //If there's player data and a model associated with it.
        if(playerData != null && playerData.model != null){
            //Only render if texture is ready
            if(playerData.texture == null || !playerData.texture.ready)
                return;
            
            playerData.model.renderArm(playerData, matrices, vertexConsumers, light, player, arm, sleeve);
        }
        
        //TODO Re-implement
        /*if (playerData != null) {

            if (playerData.model != null) {
                if (playerData.texture == null || !playerData.texture.ready) {
                    return;
                }
                //We actually wanna use this custom vertex consumer, not the one provided by the render arguments.
                VertexConsumer vc = vertexConsumers.getBuffer(RenderLayer.getEntityCutout(playerData.texture.id));

                for (CustomModelPart part : playerData.model.allParts) {
                    if (part.parentType == CustomModelPart.ParentType.RightArm && arm == model.rightArm) {
                        matrices.push();

                        model.rightArm.rotate(matrices);
                        part.render(999, matrices, vc, light, OverlayTexture.DEFAULT_UV);

                        matrices.pop();
                    } else if (part.parentType == CustomModelPart.ParentType.LeftArm && arm == model.leftArm) {
                        matrices.push();

                        model.leftArm.rotate(matrices);
                        part.render(999, matrices, vc, light, OverlayTexture.DEFAULT_UV);

                        matrices.pop();
                    }
                }
            }
        }

        PlayerEntityModelAccess playerEntityModel = (PlayerEntityModelAccess) model;
        playerEntityModel.figura$getDisabledParts().clear();*/
    }


    @Inject(at = @At("RETURN"), method = "setModelPose")
    public void onSetModelPose(AbstractClientPlayerEntity player, CallbackInfo ci) {
        PlayerEntityModel<AbstractClientPlayerEntity> model = this.getModel();
        PlayerEntityModelAccess playerEntityModel = (PlayerEntityModelAccess) model;

        if (playerEntityModel.figura$getDisabledParts().contains(model.helmet)) model.helmet.visible = false;
        if (playerEntityModel.figura$getDisabledParts().contains(model.jacket)) model.jacket.visible = false;
        if (playerEntityModel.figura$getDisabledParts().contains(model.leftPantLeg)) model.leftPantLeg.visible = false;
        if (playerEntityModel.figura$getDisabledParts().contains(model.rightPantLeg)) model.rightPantLeg.visible = false;
        if (playerEntityModel.figura$getDisabledParts().contains(model.leftSleeve)) model.leftSleeve.visible = false;
        if (playerEntityModel.figura$getDisabledParts().contains(model.rightSleeve)) model.rightSleeve.visible = false;
    }

}