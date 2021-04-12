package net.blancworks.figura.mixin;

import net.blancworks.figura.FiguraMod;
import net.blancworks.figura.PlayerData;
import net.blancworks.figura.access.ElytraEntityModelAccess;
import net.blancworks.figura.access.MatrixStackAccess;
import net.blancworks.figura.lua.api.model.ElytraModelAPI;
import net.blancworks.figura.lua.api.model.VanillaModelPartCustomization;
import net.blancworks.figura.models.CustomModelPart;
import net.blancworks.figura.models.FiguraTexture;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.AnimalModel;
import net.minecraft.client.render.entity.model.ElytraEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.function.Function;

@Mixin(ElytraEntityModel.class)
public class ElytraEntityModelMixin<T extends LivingEntity> extends AnimalModel<T> implements ElytraEntityModelAccess {

    @Shadow
    @Final
    private ModelPart field_3365;
    @Shadow
    @Final
    private ModelPart field_3364;

    @Override
    @Shadow
    protected Iterable<ModelPart> getHeadParts() {
        return null;
    }

    @Override
    @Shadow
    protected Iterable<ModelPart> getBodyParts() {
        return null;
    }

    @Override
    @Shadow
    public void setAngles(T entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {

    }

    @Override
    public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, float red, float green, float blue, float alpha) {
        PlayerData data = FiguraMod.currentData;

        if (data != null && data.model != null) {

            //Left wing
            {
                VanillaModelPartCustomization originModification = data.model.originModifications.get(ElytraModelAPI.VANILLA_LEFT_WING_ID);

                if (originModification != null && originModification.stackReference != null) {
                    if (originModification.visible == null || originModification.visible == true) {
                        MatrixStackAccess msa = (MatrixStackAccess) (Object) new MatrixStack();
                        msa.pushEntry(originModification.stackReference);
                        getLeftWing().render((MatrixStack) msa, vertices, light, overlay, red, green, blue, alpha);
                    }

                    getLeftWing().visible = false;
                }
            }

            {
                VanillaModelPartCustomization originModification = data.model.originModifications.get(ElytraModelAPI.VANILLA_RIGHT_WING_ID);

                if (originModification != null && originModification.stackReference != null) {
                    if (originModification.visible == null || originModification.visible == true) {
                        MatrixStackAccess msa = (MatrixStackAccess) (Object) new MatrixStack();
                        msa.pushEntry(originModification.stackReference);
                        getRightWing().render((MatrixStack) msa, vertices, light, overlay, red, green, blue, alpha);
                    }

                    getRightWing().visible = false;
                }
            }
        }

        super.render(matrices, vertices, light, overlay, red, green, blue, alpha);

        getLeftWing().visible = true;
        getRightWing().visible = true;

        if (data != null && data.model != null) {
            figura$renderExtraElytraPartsWithTexture(data, RenderLayer.getEntityTranslucent(data.texture.id), matrices, light, overlay);

            for (FiguraTexture extraTexture : data.extraTextures) {
                Function<Identifier, RenderLayer> renderLayerGetter = FiguraTexture.EXTRA_TEXTURE_TO_RENDER_LAYER.get(extraTexture.type);

                if (renderLayerGetter != null) {
                    figura$renderExtraElytraPartsWithTexture(data, renderLayerGetter.apply(extraTexture.id), matrices, light, overlay);
                }
            }
        }
    }

    public ModelPart getLeftWing() {
        return field_3365;
    }

    public ModelPart getRightWing() {
        return field_3364;
    }

    public void figura$renderExtraElytraPartsWithTexture(PlayerData data, RenderLayer layer, MatrixStack matrices, int light, int overlay) {
        VertexConsumer actualConsumer = FiguraMod.vertexConsumerProvider.getBuffer(layer);

        //Render left parts.
        matrices.push();
        getLeftWing().rotate(matrices);

        for (CustomModelPart modelPart : data.model.leftElytraParts) {
            data.model.leftToRender = modelPart.render(data.model.leftToRender, matrices, actualConsumer, light, overlay);

            if (data.model.leftToRender == 0)
                break;
        }

        matrices.pop();

        //Render right parts.
        matrices.push();
        getRightWing().rotate(matrices);

        for (CustomModelPart modelPart : data.model.rightElytraParts) {
            data.model.leftToRender = modelPart.render(data.model.leftToRender, matrices, actualConsumer, light, overlay);

            if (data.model.leftToRender == 0)
                break;
        }

        matrices.pop();
    }
}
