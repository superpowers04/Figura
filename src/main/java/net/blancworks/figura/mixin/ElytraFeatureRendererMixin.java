package net.blancworks.figura.mixin;

import net.blancworks.figura.avatar.AvatarData;
import net.blancworks.figura.avatar.AvatarDataManager;
import net.blancworks.figura.access.ElytraEntityModelAccess;
import net.blancworks.figura.access.ModelPartAccess;
import net.blancworks.figura.lua.api.model.ElytraModelAPI;
import net.blancworks.figura.lua.api.model.VanillaModelPartCustomization;
import net.blancworks.figura.trust.TrustContainer;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.ElytraFeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.ElytraEntityModel;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;

@Mixin(ElytraFeatureRenderer.class)
public class ElytraFeatureRendererMixin<T extends LivingEntity, M extends EntityModel<T>> extends FeatureRenderer<T, M> {

    public ElytraFeatureRendererMixin(FeatureRendererContext<T, M> context) {
        super(context);
    }

    @Shadow @Final private ElytraEntityModel<T> elytra;
    private final ArrayList<ModelPart> figura$customizedParts = new ArrayList<>();

    @Inject(at = @At("HEAD"), method = "render")
    public void onRender(MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, T livingEntity, float f, float g, float h, float j, float k, float l, CallbackInfo ci) {
        AvatarData data = livingEntity instanceof PlayerEntity ? AvatarDataManager.getDataForPlayer(livingEntity.getUuid()) : AvatarDataManager.getDataForEntity(livingEntity);

        if (data != null && data.getTrustContainer().getTrust(TrustContainer.Trust.VANILLA_MODEL_EDIT) == 1) {
            figura$applyPartCustomization(ElytraModelAPI.VANILLA_LEFT_WING, ((ElytraEntityModelAccess) elytra).getLeftWing(), data);
            figura$applyPartCustomization(ElytraModelAPI.VANILLA_RIGHT_WING, ((ElytraEntityModelAccess) elytra).getRightWing(), data);
        }
    }

    @Inject(at = @At("RETURN"), method = "render")
    public void postRender(MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, T livingEntity, float f, float g, float h, float j, float k, float l, CallbackInfo ci) {
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
