package net.blancworks.figura.mixin;

import net.blancworks.figura.avatar.AvatarData;
import net.blancworks.figura.avatar.AvatarDataManager;
import net.blancworks.figura.access.ModelPartAccess;
import net.blancworks.figura.lua.api.model.VanillaModelAPI;
import net.blancworks.figura.lua.api.model.VanillaModelPartCustomization;
import net.blancworks.figura.trust.TrustContainer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.*;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;

@Mixin(MobEntityRenderer.class)
public abstract class MobEntityRendererMixin<T extends MobEntity, M extends EntityModel<T>> extends LivingEntityRenderer<T, M> {
    public MobEntityRendererMixin(EntityRendererFactory.Context ctx, M model, float shadowRadius) {
        super(ctx, model, shadowRadius);
    }

    @Unique private final ArrayList<ModelPart> figura$customizedParts = new ArrayList<>();

    @Inject(at = @At("HEAD"), method = "render")
    public void onRender(T entity, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo ci) {
        MobEntityRenderer<T, M> mob = (MobEntityRenderer<T, M>) (Object) this;

        AvatarData data = AvatarDataManager.getDataForEntity(entity);
        AvatarData.setRenderingData(data, vertexConsumerProvider, mob.getModel(), MinecraftClient.getInstance().getTickDelta());

        //shadowRadius = 0.5f; //Vanilla shadow radius.
        //Reset this here because... Execution order.

        if (mob instanceof PiglinEntityRenderer renderer) {
            if (data != null && data.script != null && data.getTrustContainer().getTrust(TrustContainer.Trust.VANILLA_MODEL_EDIT) == 1) {
                figura$applyPartCustomization(VanillaModelAPI.VANILLA_HEAD, renderer.getModel().head, entity);
                figura$applyPartCustomization(VanillaModelAPI.VANILLA_TORSO, renderer.getModel().body, entity);
                figura$applyPartCustomization(VanillaModelAPI.VANILLA_LEFT_ARM, renderer.getModel().leftArm, entity);
                figura$applyPartCustomization(VanillaModelAPI.VANILLA_RIGHT_ARM, renderer.getModel().rightArm, entity);
                figura$applyPartCustomization(VanillaModelAPI.VANILLA_LEFT_LEG, renderer.getModel().leftLeg, entity);
                figura$applyPartCustomization(VanillaModelAPI.VANILLA_RIGHT_LEG, renderer.getModel().rightLeg, entity);

                figura$applyPartCustomization(VanillaModelAPI.VANILLA_HAT, renderer.getModel().hat, entity);
                figura$applyPartCustomization(VanillaModelAPI.VANILLA_JACKET, renderer.getModel().jacket, entity);
                figura$applyPartCustomization(VanillaModelAPI.VANILLA_LEFT_SLEEVE, renderer.getModel().leftSleeve, entity);
                figura$applyPartCustomization(VanillaModelAPI.VANILLA_RIGHT_SLEEVE, renderer.getModel().rightSleeve, entity);
                figura$applyPartCustomization(VanillaModelAPI.VANILLA_LEFT_PANTS, renderer.getModel().leftPants, entity);
                figura$applyPartCustomization(VanillaModelAPI.VANILLA_RIGHT_PANTS, renderer.getModel().rightPants, entity);

                if (data.script.customShadowSize != null)
                    shadowRadius = data.script.customShadowSize;
            }
        }
    }

    @Inject(at = @At("RETURN"), method = "render")
    public void postRender(T entity, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo ci) {
        AvatarData data = AvatarDataManager.getDataForEntity(entity);

        if (data != null && data.script != null && data.script.isDone) {
            for (VanillaModelAPI.ModelPartTable partTable : data.script.vanillaModelPartTables) {
                if (VanillaModelAPI.isPartSpecial(partTable.accessor))
                    continue;

                partTable.updateFromPart();
            }
        }

        figura$clearAllPartCustomizations();
    }

    public void figura$applyPartCustomization(String id, ModelPart part, Entity entity) {
        AvatarData data = AvatarDataManager.getDataForEntity(entity);

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
