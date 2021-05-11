package net.blancworks.figura.access;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;

public interface PlayerEntityRendererAccess {
    void figura$setupTransformsPublic(AbstractClientPlayerEntity abstractClientPlayerEntity, MatrixStack matrixStack, float f, float g, float h);
}