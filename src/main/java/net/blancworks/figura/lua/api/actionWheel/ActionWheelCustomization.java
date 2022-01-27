package net.blancworks.figura.lua.api.actionWheel;

import net.blancworks.figura.lua.CustomScript;
import net.blancworks.figura.lua.api.item.ItemStackAPI;
import net.blancworks.figura.lua.api.math.LuaVector;
import net.blancworks.figura.models.FiguraTexture;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3f;
import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.ThreeArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;

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

    public static LuaTable getTableForPart(String accessor, CustomScript targetScript) {
        return new LuaTable() {{
            set("getFunction", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return targetScript.getOrMakeActionWheelCustomization(accessor).function;
                }
            });

            set("setFunction", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1) {
                    targetScript.getOrMakeActionWheelCustomization(accessor).function = arg1.isnil() ? null : arg1.checkfunction();
                    return NIL;
                }
            });

            set("getItem", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return ItemStackAPI.getTable(targetScript.getOrMakeActionWheelCustomization(accessor).item);
                }
            });

            set("setItem", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1) {
                    targetScript.getOrMakeActionWheelCustomization(accessor).item = arg1.isnil() ? null : ItemStackAPI.checkOrCreateItemStack(arg1);
                    return NIL;
                }
            });

            set("getHoverItem", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return ItemStackAPI.getTable(targetScript.getOrMakeActionWheelCustomization(accessor).hoverItem);
                }
            });

            set("setHoverItem", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    targetScript.getOrMakeActionWheelCustomization(accessor).hoverItem = arg.isnil() ? null : ItemStackAPI.checkOrCreateItemStack(arg);
                    return NIL;
                }
            });

            set("getColor", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaVector.of(targetScript.getOrMakeActionWheelCustomization(accessor).color);
                }
            });

            set("setColor", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    targetScript.getOrMakeActionWheelCustomization(accessor).color = arg.isnil() ? null : LuaVector.checkOrNew(arg).asV3f();
                    return NIL;
                }
            });

            set("getHoverColor", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaVector.of(targetScript.getOrMakeActionWheelCustomization(accessor).hoverColor);
                }
            });

            set("setHoverColor", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    targetScript.getOrMakeActionWheelCustomization(accessor).hoverColor = arg.isnil() ? null : LuaVector.checkOrNew(arg).asV3f();
                    return NIL;
                }
            });

            set("getTitle", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(targetScript.getOrMakeActionWheelCustomization(accessor).title);
                }
            });

            set("setTitle", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1) {
                    targetScript.getOrMakeActionWheelCustomization(accessor).title = arg1.isnil() ? null : arg1.checkjstring();
                    return NIL;
                }
            });

            set("getTexture", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaString.valueOf(targetScript.getOrMakeActionWheelCustomization(accessor).texture.toString());
                }
            });

            set("setTexture", new TwoArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1, LuaValue arg2) {
                    ActionWheelCustomization cust = targetScript.getOrMakeActionWheelCustomization(accessor);
                    try {
                        cust.texture = ActionWheelCustomization.TextureType.valueOf(arg1.checkjstring());

                        if (cust.texture == ActionWheelCustomization.TextureType.Resource)
                            cust.texturePath = new Identifier(arg2.checkjstring());
                    } catch (Exception ignored) {
                        cust.texture = ActionWheelCustomization.TextureType.None;
                    }

                    return NIL;
                }
            });

            set("getTextureScale", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaVector.of(targetScript.getOrMakeActionWheelCustomization(accessor).textureScale);
                }
            });

            set("setTextureScale", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1) {
                    targetScript.getOrMakeActionWheelCustomization(accessor).textureScale = LuaVector.checkOrNew(arg1).asV2f();
                    return NIL;
                }
            });

            set("getUV", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    ActionWheelCustomization cust = targetScript.getOrMakeActionWheelCustomization(accessor);

                    Vec2f offset = cust.uvOffset;
                    Vec2f size = cust.uvSize;
                    Vec2f tex = cust.textureSize;

                    if (offset == null)
                        offset = new Vec2f(0f, 0f);

                    if (size == null)
                        size = new Vec2f(0f, 0f);

                    if (tex == null)
                        tex = new Vec2f(0f, 0f);

                    return new LuaVector(offset.x, offset.y, size.x, size.y, tex.x, tex.y);
                }
            });

            set("setUV", new ThreeArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1, LuaValue arg2, LuaValue arg3) {
                    ActionWheelCustomization cust = targetScript.getOrMakeActionWheelCustomization(accessor);

                    LuaVector offset = LuaVector.checkOrNew(arg1);
                    LuaVector size = LuaVector.checkOrNew(arg2);

                    cust.uvOffset = offset.asV2f();
                    cust.uvSize = size.asV2f();

                    if (!arg3.isnil()) {
                        LuaVector tex = LuaVector.checkOrNew(arg3);
                        cust.textureSize = tex.asV2f();
                    }

                    return NIL;
                }
            });

            set("clear", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    targetScript.actionWheelCustomizations.put(accessor, new ActionWheelCustomization());
                    return NIL;
                }
            });
        }};
    }
}
