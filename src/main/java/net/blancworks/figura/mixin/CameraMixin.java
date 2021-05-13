package net.blancworks.figura.mixin;

import net.blancworks.figura.CameraData;
import net.blancworks.figura.PlayerData;
import net.blancworks.figura.PlayerDataManager;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {

    @Shadow private float pitch;
    @Shadow private float yaw;
    @Shadow private float cameraY;
    @Shadow private float lastCameraY;

    @Inject(method = "update", at = @At(value = "TAIL"))
    private void updateTail(BlockView area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {

        PlayerData currentData = PlayerDataManager.getDataForPlayer(focusedEntity.getUuid());
        if (currentData != null && currentData.script != null) {
            CameraData data = currentData.script.camera;

            this.setRotation(this.yaw + data.rotation.y, this.pitch + data.rotation.x);

            if (!thirdPerson) {
                this.setPos(
                        MathHelper.lerp(tickDelta, focusedEntity.prevX, focusedEntity.getX()),
                        MathHelper.lerp(tickDelta, focusedEntity.prevY, focusedEntity.getY()) + (double) MathHelper.lerp(tickDelta, this.lastCameraY, this.cameraY) + data.fpPosition.getY(),
                        MathHelper.lerp(tickDelta, focusedEntity.prevZ, focusedEntity.getZ())
                );

                this.moveBy(-data.fpPosition.getZ(), 0.0d, -data.fpPosition.getX());
            }
            else {
                //y
                this.setPos(
                        MathHelper.lerp(tickDelta, focusedEntity.prevX, focusedEntity.getX()),
                        MathHelper.lerp(tickDelta, focusedEntity.prevY, focusedEntity.getY()) + (double) MathHelper.lerp(tickDelta, this.lastCameraY, this.cameraY) + data.position.getY(),
                        MathHelper.lerp(tickDelta, focusedEntity.prevZ, focusedEntity.getZ())
                );

                //x
                this.setRotation(this.yaw - 90, this.pitch);
                double x = -this.clipToSpace(data.position.getX());
                this.setRotation(this.yaw + 90, this.pitch);

                //z
                this.moveBy(-this.clipToSpace(4.0d + data.position.getZ()), 0.0d, x);
            }
        }
    }

    @Shadow protected abstract void moveBy(double x, double y, double z);

    @Shadow protected abstract double clipToSpace(double desiredCameraDistance);

    @Shadow protected abstract void setPos(double lerp, double v, double lerp1);

    @Shadow protected abstract void setRotation(float yaw, float pitch);
}