package net.blancworks.figura.access;

import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;

public interface GameRendererAccess {
    double figura$getFov(Camera camera, float tickDelta, boolean changingFov);
    void figura$bobView(MatrixStack matrices, float f);
    void figura$bobViewWhenHurt(MatrixStack matrices, float f);
}
