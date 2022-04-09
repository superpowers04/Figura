package net.blancworks.figura.lua.api.model;

import net.blancworks.figura.lua.CustomScript;
import net.blancworks.figura.lua.api.math.LuaVector;
import net.blancworks.figura.models.CustomModelPart;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3f;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;

public class VanillaModelPartCustomization {
    public Vec3f pos;
    public Vec3f rot;
    public Vec3f scale;
    public Boolean visible;
    public MatrixStack.Entry stackReference;
    public CustomModelPart part;

    public static LuaTable getTableForPart(String accessor, CustomScript targetScript) {
        return new LuaTable() {{
            set("getPos", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaVector.of(targetScript.getOrMakePartCustomization(accessor).pos);
                }
            });

            set("setPos", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1) {
                    targetScript.getOrMakePartCustomization(accessor).pos = LuaVector.checkOrNew(arg1).asV3f();
                    return NIL;
                }
            });

            set("getRot", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaVector.of(targetScript.getOrMakePartCustomization(accessor).rot);
                }
            });

            set("setRot", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1) {
                    targetScript.getOrMakePartCustomization(accessor).rot = LuaVector.checkOrNew(arg1).asV3f();
                    return NIL;
                }
            });

            set("getScale", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaVector.of(targetScript.getOrMakePartCustomization(accessor).scale);
                }
            });

            set("setScale", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1) {
                    targetScript.getOrMakePartCustomization(accessor).scale = LuaVector.checkOrNew(arg1).asV3f();
                    return NIL;
                }
            });

            set("getEnabled", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    Boolean enabled = targetScript.getOrMakePartCustomization(accessor).visible;
                    return enabled == null ? NIL : LuaValue.valueOf(enabled);
                }
            });

            set("setEnabled", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    targetScript.getOrMakePartCustomization(accessor).visible = arg.isnil() ? null : arg.checkboolean();
                    return NIL;
                }
            });
        }};
    }
}
