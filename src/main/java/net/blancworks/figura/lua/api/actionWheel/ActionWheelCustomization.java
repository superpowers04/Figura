package net.blancworks.figura.lua.api.actionWheel;

import net.blancworks.figura.models.FiguraTexture;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3f;
import net.minecraft.item.ItemStack;
import org.luaj.vm2.LuaFunction;

public class ActionWheelCustomization {
    public LuaFunction function;

    public ItemStack item;
    public ItemStack hoverItem;

    public Vec3f color;
    public Vec3f hoverColor;

    public String title;

    public Vec2f uvOffset;
    public Vec2f uvSize;

    public TextureType texture = TextureType.None;
    public Identifier texturePath = FiguraTexture.DEFAULT_ID;
    public Vec2f textureSize = new Vec2f(64f, 64f);
    public Vec2f textureScale = new Vec2f(1f, 1f);

    public enum TextureType {
        None,
        Custom,
        Skin,
        Cape,
        Elytra,
        Resource
    }
}
