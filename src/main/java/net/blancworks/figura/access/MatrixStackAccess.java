package net.blancworks.figura.access;

import net.minecraft.client.util.math.MatrixStack;

public interface MatrixStackAccess {
    void pushEntry(MatrixStack.Entry entry);
}
