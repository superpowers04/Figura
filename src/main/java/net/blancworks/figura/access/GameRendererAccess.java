package net.blancworks.figura.access;

import net.minecraft.client.render.Camera;

public interface GameRendererAccess {
    double figura$getFov(Camera camera, boolean changingFov);
}
