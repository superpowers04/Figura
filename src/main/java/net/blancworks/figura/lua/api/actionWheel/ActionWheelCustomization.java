package net.blancworks.figura.lua.api.actionWheel;

import net.minecraft.client.util.math.Vector3f;
import net.minecraft.client.util.math.Vector4f;
import net.minecraft.item.ItemStack;
import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaTable;

public class ActionWheelCustomization {
    public LuaFunction function;
    public ItemStack item;
    public ItemStack hoverItem;
    public Vector4f color;
    public Vector4f hoverColor;
    public String title;
}
