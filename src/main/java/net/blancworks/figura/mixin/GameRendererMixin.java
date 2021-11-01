package net.blancworks.figura.mixin;

import net.blancworks.figura.access.GameRendererAccess;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin implements GameRendererAccess {

    @Shadow @Final private MinecraftClient client;

    @Override
    public double figura$getFov(Camera camera, float tickDelta, boolean changingFov) {
        return this.getFov(camera, tickDelta, changingFov);
    }

    @Override
    public void figura$bobView(MatrixStack matrices, float distance) {
        if (this.client.getCameraEntity() instanceof PlayerEntity playerEntity) {
            float g = playerEntity.horizontalSpeed - playerEntity.prevHorizontalSpeed;
            float h = -(playerEntity.horizontalSpeed + g * this.client.getTickDelta());
            float i = MathHelper.lerp(this.client.getTickDelta(), playerEntity.prevStrideDistance, playerEntity.strideDistance);
            matrices.translate(MathHelper.sin(h * 3.1415927f) * i * 0.5f, -Math.abs(MathHelper.cos(h * 3.1415927f) * i), 0f);
            matrices.multiply(Vec3f.NEGATIVE_Z.getDegreesQuaternion(MathHelper.sin(h * 3.1415927f) * i * 3f));
            matrices.multiply(Vec3f.NEGATIVE_X.getDegreesQuaternion(Math.abs(MathHelper.cos(h * 3.1415927f - 0.2f) * i) * 5f));
        }
    }

    @Override
    public void figura$bobViewWhenHurt(MatrixStack matrices, float f) {
        this.bobViewWhenHurt(matrices, f);
    }

    @Shadow protected abstract double getFov(Camera camera, float tickDelta, boolean changingFov);
    @Shadow protected abstract void bobViewWhenHurt(MatrixStack matrices, float f);
}
