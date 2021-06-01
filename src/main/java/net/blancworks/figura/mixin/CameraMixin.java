package net.blancworks.figura.mixin;

import net.blancworks.figura.PlayerData;
import net.blancworks.figura.PlayerDataManager;
import net.blancworks.figura.lua.api.camera.CameraAPI;
import net.blancworks.figura.lua.api.camera.CameraCustomization;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
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

            CameraCustomization customization = thirdPerson ? currentData.script.cameraCustomizations.get(CameraAPI.THIRD_PERSON) : currentData.script.cameraCustomizations.get(CameraAPI.FIRST_PERSON);

            if (customization == null)
                return;

            if (customization.rotation != null && !customization.rotation.equals(Vec2f.ZERO))
                this.setRotation(this.yaw + customization.rotation.y, this.pitch + customization.rotation.x);

            if (customization.position != null) {
                this.setPos(
                        MathHelper.lerp(tickDelta, focusedEntity.prevX, focusedEntity.getX()),
                        MathHelper.lerp(tickDelta, focusedEntity.prevY, focusedEntity.getY()) + (double) MathHelper.lerp(tickDelta, this.lastCameraY, this.cameraY) + customization.position.getY(),
                        MathHelper.lerp(tickDelta, focusedEntity.prevZ, focusedEntity.getZ())
                );
            }

            if (!thirdPerson) {
                if (customization.position != null && !customization.position.equals(new Vector3f(0.0f, 0.0f, 0.0f)))
                this.moveBy(-customization.position.getZ(), 0.0d, -customization.position.getX());

                //bed fix
                if (focusedEntity instanceof LivingEntity && ((LivingEntity) focusedEntity).isSleeping()) {
                    this.moveBy(0.0d, 0.3d, 0.0d);
                }
            }
            else {
                if (customization.position != null) {
                    this.setRotation(this.yaw - 90, this.pitch);
                    double x = -this.clipToSpace(customization.position.getX());
                    this.setRotation(this.yaw + 90, this.pitch);

                    this.moveBy(-this.clipToSpace(4.0d + customization.position.getZ()), 0.0d, x);
                }
            }
        }
    }

    @Shadow protected abstract void moveBy(double x, double y, double z);

    @Shadow protected abstract double clipToSpace(double desiredCameraDistance);

    @Shadow protected abstract void setPos(double lerp, double v, double lerp1);

    @Shadow protected abstract void setRotation(float yaw, float pitch);
}