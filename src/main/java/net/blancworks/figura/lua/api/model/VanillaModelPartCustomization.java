package net.blancworks.figura.lua.api.model;

import net.blancworks.figura.models.CustomModelPart;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3f;

public class VanillaModelPartCustomization {
    public Vec3f pos;
    public Vec3f rot;
    public Boolean visible;
    public MatrixStack.Entry stackReference;
    public CustomModelPart part;
}
