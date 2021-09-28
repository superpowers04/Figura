package net.blancworks.figura.lua.api.model;

import net.blancworks.figura.models.CustomModelPart;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector3f;

public class VanillaModelPartCustomization {
    public Vector3f pos;
    public Vector3f rot;
    public Vector3f scale;
    public Boolean visible;
    public MatrixStack.Entry stackReference;
    public CustomModelPart part;
}
