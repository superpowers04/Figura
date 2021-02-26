package net.blancworks.figura.mixin;

import net.blancworks.figura.access.ModelPartAccess;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.time.Instant;

@Mixin(ModelPart.class)
public class ModelPartMixin implements ModelPartAccess {

    @Shadow @Final public float pivotX;
    @Shadow @Final public float pivotY;
    @Shadow @Final public float pivotZ;
    @Shadow @Final public float pitch;
    @Shadow @Final public float yaw;
    @Shadow @Final public float roll;
    
    private Vector3f additional_pos = new Vector3f();
    private Vector3f additional_rot = new Vector3f();
    
    @Inject(at = @At("HEAD"), method = "rotate", cancellable = true)
    public void rotate_head(MatrixStack matrix, CallbackInfo info) {
        if(additional_pos != null) {
            matrix.translate((pivotX + additional_pos.getX()) / 16.0f, (pivotY + additional_pos.getY()) / 16.0f, (pivotZ + additional_pos.getZ()) / 16.0f);
        } else {
            matrix.translate(pivotX / 16.0f, pivotY/ 16.0f, pivotZ/ 16.0f);
        }

        if (additional_rot != null) {
            matrix.multiply(Vector3f.POSITIVE_Z.getRadialQuaternion(roll + additional_rot.getZ()));
            matrix.multiply(Vector3f.POSITIVE_Y.getRadialQuaternion(yaw + additional_rot.getY()));
            matrix.multiply(Vector3f.POSITIVE_X.getRadialQuaternion(pitch + additional_rot.getX()));
        } else {
            matrix.multiply(Vector3f.POSITIVE_Z.getRadialQuaternion(roll));
            matrix.multiply(Vector3f.POSITIVE_Y.getRadialQuaternion(yaw));
            matrix.multiply(Vector3f.POSITIVE_X.getRadialQuaternion(pitch));
        }
        
        info.cancel();
    }

    public void setAdditionalPos(Vector3f v){
        additional_pos = v;
    }
    public void setAdditionalRot(Vector3f v){
        additional_rot = v;
    }

    public Vector3f getAdditionalPos(){
        return additional_pos;
    }
    public Vector3f getAdditionalRot(){
        return additional_rot;
    }
    
}
