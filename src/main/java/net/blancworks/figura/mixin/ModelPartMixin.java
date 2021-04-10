package net.blancworks.figura.mixin;

import net.blancworks.figura.access.ModelPartAccess;
import net.blancworks.figura.lua.api.model.VanillaModelPartCustomization;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModelPart.class)
public class ModelPartMixin implements ModelPartAccess {
    @Shadow
    public float pivotX;
    @Shadow
    public float pivotY;
    @Shadow
    public float pivotZ;
    @Shadow
    public float pitch;
    @Shadow
    public float yaw;
    @Shadow
    public float roll;

    @Shadow
    public boolean visible;
    private VanillaModelPartCustomization figura$customization = null;

    private boolean prevVisible;

    //Modify transformation matrices for this part
    @Inject(at = @At("HEAD"), method = "rotate")
    public void onRotate(MatrixStack matrices, CallbackInfo info) {
        if (figura$customization != null) {
            if (figura$customization.pos != null) {
                pivotX += figura$customization.pos.getX();
                pivotY += figura$customization.pos.getY();
                pivotZ += figura$customization.pos.getZ();
            }

            if (figura$customization.rot != null) {
                pitch += figura$customization.rot.getX();
                yaw += figura$customization.rot.getY();
                roll += figura$customization.rot.getZ();
            }
        }
    }

    //Restore matrices
    @Inject(at = @At("RETURN"), method = "rotate")
    public void postRotate(MatrixStack matrices, CallbackInfo info) {
        if (figura$customization != null) {
            if (figura$customization.pos != null) {
                pivotX -= figura$customization.pos.getX();
                pivotY -= figura$customization.pos.getY();
                pivotZ -= figura$customization.pos.getZ();
            }

            if (figura$customization.rot != null) {
                pitch -= figura$customization.rot.getX();
                yaw -= figura$customization.rot.getY();
                roll -= figura$customization.rot.getZ();
            }
        }
    }

    //Store/Modify visible state
    @Inject(at = @At("HEAD"), method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;IIFFFF)V")
    public void onRender(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, float red, float green, float blue, float alpha, CallbackInfo ci) {
        prevVisible = visible;
        if (figura$customization != null) {

            if (visible && figura$customization.visible != null) {
                visible = figura$customization.visible;
            }
        }
    }

    //Restore visible state
    @Inject(at = @At("RETURN"), method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;IIFFFF)V")
    public void postRender(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, float red, float green, float blue, float alpha, CallbackInfo ci) {
        visible = prevVisible;
    }

    @Override
    public VanillaModelPartCustomization figura$getPartCustomization() {
        return figura$customization;
    }

    @Override
    public void figura$setPartCustomization(VanillaModelPartCustomization toSet) {
        figura$customization = toSet;
    }
}
