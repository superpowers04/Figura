package net.blancworks.figura.lua.api.actionWheel;

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
}
