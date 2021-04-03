package net.blancworks.figura.mixin;

import net.blancworks.figura.access.ModelPartAccess;
import net.minecraft.client.model.ModelPart;
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

    private Vector3f figura$additionalPos = new Vector3f();
    private Vector3f figura$additionalRot = new Vector3f();

    //Used sometimes for copying stuff to armor, or similar.
    private Vector3f figura$lastAdditionalPos = new Vector3f();
    private Vector3f figura$lastAdditionalRot = new Vector3f();

    @Inject(at = @At("HEAD"), method = "rotate", cancellable = true)
    public void onRotate(MatrixStack matrices, CallbackInfo info) {
        if (figura$additionalPos != null) {
            matrices.translate((pivotX + figura$additionalPos.getX()) / 16.0f, (pivotY + figura$additionalPos.getY()) / 16.0f, (pivotZ + figura$additionalPos.getZ()) / 16.0f);
        } else {
            matrices.translate(pivotX / 16.0f, pivotY / 16.0f, pivotZ / 16.0f);
        }

        if (figura$additionalRot != null) {
            matrices.multiply(Vector3f.POSITIVE_Z.getRadialQuaternion(roll + figura$additionalRot.getZ()));
            matrices.multiply(Vector3f.POSITIVE_Y.getRadialQuaternion(yaw + figura$additionalRot.getY()));
            matrices.multiply(Vector3f.POSITIVE_X.getRadialQuaternion(pitch + figura$additionalRot.getX()));
        } else {
            matrices.multiply(Vector3f.POSITIVE_Z.getRadialQuaternion(roll));
            matrices.multiply(Vector3f.POSITIVE_Y.getRadialQuaternion(yaw));
            matrices.multiply(Vector3f.POSITIVE_X.getRadialQuaternion(pitch));
        }

        info.cancel();
    }

    @Inject(at = @At("RETURN"), method = "copyPositionAndRotation(Lnet/minecraft/client/model/ModelPart;)V")
    public void copyPositionAndRotation(ModelPart modelPart, CallbackInfo ci) {
        setAdditionalPos(((ModelPartAccess) modelPart).getAdditionalPos());
        setAdditionalRot(((ModelPartAccess) modelPart).getAdditionalRot());
    }

    @Override
    public void setAdditionalPos(Vector3f v) {
        figura$lastAdditionalPos = figura$additionalPos;
        figura$additionalPos = v;
    }

    @Override
    public void setAdditionalRot(Vector3f v) {
        figura$lastAdditionalRot = figura$additionalRot;
        figura$additionalRot = v;
    }

    @Override
    public Vector3f getAdditionalPos() {
        return figura$additionalPos;
    }

    @Override
    public Vector3f getAdditionalRot() {
        return figura$additionalRot;
    }

    // @TODO remove?
    public Vector3f getLastAdditionalPos() {
        return figura$lastAdditionalPos;
    }

    public Vector3f getLastAdditionalRot() {
        return figura$lastAdditionalRot;
    }
}
