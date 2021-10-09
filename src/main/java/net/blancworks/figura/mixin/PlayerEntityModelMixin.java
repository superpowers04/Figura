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

    @Shadow @Final private ModelPart cloak;
    @Shadow @Final private ModelPart ear;

    private boolean prevCloakVisible = false;
    private boolean prevEarVisible = false;

    @Override
    public ModelPart getCloak() {
        return this.cloak;
    }

    @Override
    public ModelPart getEar() {
        return this.ear;
    }

    @Inject(at = @At("HEAD"), method = "renderCape")
    public void onRenderCape(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, CallbackInfo ci) {
        prevCloakVisible = cloak.visible;
        cloak.visible = true;
    }

    @Inject(at = @At("RETURN"), method = "renderCape")
    public void posRenderCape(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, CallbackInfo ci) {
        cloak.visible = prevCloakVisible;
    }

    @Inject(at = @At("HEAD"), method = "renderEars")
    public void onRenderEars(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, CallbackInfo ci) {
        prevEarVisible = ear.visible;
        ear.visible = true;
    }

    @Inject(at = @At("RETURN"), method = "renderEars")
    public void posRenderEars(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, CallbackInfo ci) {
        ear.visible = prevEarVisible;
    }
}
