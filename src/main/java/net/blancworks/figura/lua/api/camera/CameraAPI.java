package net.blancworks.figura.lua.api.camera;

import net.blancworks.figura.lua.CustomScript;
import net.blancworks.figura.lua.api.math.LuaVector;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3f;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;

public class CameraAPI {
    public static final String FIRST_PERSON = "FIRST_PERSON";
    public static final String THIRD_PERSON = "THIRD_PERSON";

    public static Identifier getID() {
        return new Identifier("default", "camera");
    }

    public static LuaTable getForScript(CustomScript script) {
        return new LuaTable() {{
            set(FIRST_PERSON, getTableForPart(FIRST_PERSON, script));
            set(THIRD_PERSON, getTableForPart(THIRD_PERSON, script));
        }};
    }

    public static LuaTable getTableForPart(String accessor, CustomScript targetScript) {
        return new LuaTable() {{
            set("getPos", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    Vec3f pos = targetScript.getOrMakeCameraCustomization(accessor).position;
                    return pos == null ? NIL : LuaVector.of(pos);
                }
            });

            set("setPos", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1) {
                    targetScript.getOrMakeCameraCustomization(accessor).position = arg1.isnil() ? null : LuaVector.checkOrNew(arg1).asV3f();
                    return NIL;
                }
            });

            set("getRot", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    Vec3f rot = targetScript.getOrMakeCameraCustomization(accessor).rotation;
                    return rot == null ? NIL : LuaVector.of(rot);
                }
            });

            set("setRot", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1) {
                    targetScript.getOrMakeCameraCustomization(accessor).rotation = arg1.isnil() ? null : LuaVector.checkOrNew(arg1).asV3f();
                    return NIL;
                }
            });

            set("getPivot", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    Vec3f piv = targetScript.getOrMakeCameraCustomization(accessor).pivot;
                    return piv == null ? NIL : LuaVector.of(piv);
                }
            });

            set("setPivot", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1) {
                    targetScript.getOrMakeCameraCustomization(accessor).pivot = arg1.isnil() ? null : LuaVector.checkOrNew(arg1).asV3f();
                    return NIL;
                }
            });
        }};
    }
}
