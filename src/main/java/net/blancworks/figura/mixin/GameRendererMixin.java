package net.blancworks.figura.mixin;

import net.blancworks.figura.access.GameRendererAccess;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin implements GameRendererAccess {

    @Shadow @Final private MinecraftClient client;

    @Override
    public double figura$getFov(Camera camera, boolean changingFov) {
        return this.getFov(camera, this.client.getTickDelta(), changingFov);
    }

    @Shadow protected abstract double getFov(Camera camera, float tickDelta, boolean changingFov);
}
