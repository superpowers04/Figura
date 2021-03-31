package net.blancworks.figura.mixin;

import net.blancworks.figura.access.ModelPartAccess;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModelPart.class)
public class ModelPartMixin implements ModelPartAccess {

    @Shadow
    @Final
    public float pivotX;
    @Shadow
    @Final
    public float pivotY;
    @Shadow
    @Final
    public float pivotZ;
    @Shadow
    @Final
    public float pitch;
    @Shadow
    @Final
    public float yaw;
    @Shadow
    @Final
    public float roll;

    private Vec3f additional_pos = new Vec3f();
    private Vec3f additional_rot = new Vec3f();

    //Used sometimes for copying stuff to armor, or similar.
    private Vec3f last_additional_pos = new Vec3f();
    private Vec3f last_additional_rot = new Vec3f();

    @Inject(at = @At("HEAD"), method = "rotate", cancellable = true)
    public void rotate_head(MatrixStack matrix, CallbackInfo info) {
        if (additional_pos != null) {
            matrix.translate((pivotX + additional_pos.getX()) / 16.0f, (pivotY + additional_pos.getY()) / 16.0f, (pivotZ + additional_pos.getZ()) / 16.0f);
        } else {
            matrix.translate(pivotX / 16.0f, pivotY / 16.0f, pivotZ / 16.0f);
        }

        if (additional_rot != null) {
            matrix.multiply(Vec3f.POSITIVE_Z.getRadialQuaternion(roll + additional_rot.getZ()));
            matrix.multiply(Vec3f.POSITIVE_Y.getRadialQuaternion(yaw + additional_rot.getY()));
            matrix.multiply(Vec3f.POSITIVE_X.getRadialQuaternion(pitch + additional_rot.getX()));
        } else {
            matrix.multiply(Vec3f.POSITIVE_Z.getRadialQuaternion(roll));
            matrix.multiply(Vec3f.POSITIVE_Y.getRadialQuaternion(yaw));
            matrix.multiply(Vec3f.POSITIVE_X.getRadialQuaternion(pitch));
        }

        info.cancel();
    }

    @Inject(at = @At("RETURN"), method = "copyTransform(Lnet/minecraft/client/model/ModelPart;)V")
    public void copyTransform(ModelPart modelPart, CallbackInfo ci) {
        setAdditionalPos(((ModelPartAccess)(Object)modelPart).getAdditionalPos());
        setAdditionalRot(((ModelPartAccess)(Object)modelPart).getAdditionalRot());
    }

    public void setAdditionalPos(Vec3f v) {
        last_additional_pos = additional_pos;
        additional_pos = v;
    }

    public void setAdditionalRot(Vec3f v) {
        last_additional_rot = additional_rot;
        additional_rot = v;
    }

    public Vec3f getAdditionalPos() {
        return additional_pos;
    }

    public Vec3f getAdditionalRot() {
        return additional_rot;
    }

    public Vec3f getLastAdditionalPos() {
        return last_additional_pos;
    }

    public Vec3f getLastAdditionalRot() {
        return last_additional_rot;
    }
}
