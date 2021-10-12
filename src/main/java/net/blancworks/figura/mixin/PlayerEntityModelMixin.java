package net.blancworks.figura.mixin;

import net.blancworks.figura.access.PlayerEntityModelAccess;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityModel.class)
public class PlayerEntityModelMixin implements PlayerEntityModelAccess {

    @Shadow @Final private ModelPart cape;
    @Shadow @Final private ModelPart ears;

    private boolean prevCloakVisible = false;
    private boolean prevEarVisible = false;

    @Override
    public ModelPart getCloak() {
        return this.cape;
    }

    @Override
    public ModelPart getEar() {
        return this.ears;
    }

    @Inject(at = @At("HEAD"), method = "renderCape")
    public void onRenderCape(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, CallbackInfo ci) {
        prevCloakVisible = cape.visible;
        cape.visible = true;
    }

    @Inject(at = @At("RETURN"), method = "renderCape")
    public void posRenderCape(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, CallbackInfo ci) {
        cape.visible = prevCloakVisible;
    }

    @Inject(at = @At("HEAD"), method = "renderEars")
    public void onRenderEars(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, CallbackInfo ci) {
        prevEarVisible = ears.visible;
        ears.visible = true;
    }

    @Inject(at = @At("RETURN"), method = "renderEars")
    public void posRenderEars(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, CallbackInfo ci) {
        ears.visible = prevEarVisible;
    }
}
