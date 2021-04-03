package net.blancworks.figura.mixin;

import net.blancworks.figura.access.ModelPartAccess;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.ElytraFeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ElytraFeatureRenderer.class)
public abstract class ElytraFeatureRendererMixin<T extends LivingEntity, M extends EntityModel<T>> extends FeatureRenderer<T, M> {

    ElytraFeatureRendererMixin(FeatureRendererContext<T, M> context) {
        super(context);
    }

    @SuppressWarnings("unchecked")
    @Inject(at = @At("HEAD"), method = "render")
    public void onRenderStart(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int i, LivingEntity entity,
                              float f, float g, float h, float j, float k, float l, CallbackInfo ci) {
        matrices.push();

        try {
            BipedEntityModel<T> mdl = (BipedEntityModel<T>) this.getContextModel();
            ModelPartAccess access = (ModelPartAccess) mdl.torso;
            Vector3f additionalMove = access.getAdditionalPos();
            if (additionalMove != null)
                matrices.translate(additionalMove.getX() / 16.0f, additionalMove.getY() / 16.0f, additionalMove.getZ() / 16.0f);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Inject(at = @At("RETURN"), method = "render")
    public void onRenderEnd(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int i, LivingEntity entity,
                            float f, float g, float h, float j, float k, float l, CallbackInfo ci) {
        matrices.pop();
    }
}
