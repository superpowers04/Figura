package net.blancworks.figura.lua.api.camera;

import net.blancworks.figura.lua.CustomScript;
import net.blancworks.figura.lua.api.ReadOnlyLuaTable;
import net.blancworks.figura.lua.api.ScriptLocalAPITable;
import net.blancworks.figura.lua.api.math.LuaVector;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.util.Identifier;
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

    public static ReadOnlyLuaTable getForScript(CustomScript script) {
        return new ScriptLocalAPITable(script, new LuaTable() {{
            set(FIRST_PERSON, getTableForPart(FIRST_PERSON, script));
            set(THIRD_PERSON, getTableForPart(THIRD_PERSON, script));
        }});
    }

    public static ReadOnlyLuaTable getTableForPart(String accessor, CustomScript script) {
        return new CameraTable(accessor, script);
    }

    private static class CameraTable extends ScriptLocalAPITable {
        String accessor;

        public CameraTable(String accessor, CustomScript script) {
            super(script);
            this.accessor = accessor;
            super.setTable(getTable());
        }

        public LuaTable getTable() {
            LuaTable ret = new LuaTable();

            ret.set("getPos", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    Vector3f pos = targetScript.getOrMakeCameraCustomization(accessor).position;
                    return pos == null ? NIL : LuaVector.of(pos);
                }
            });

            ret.set("setPos", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1) {
                    targetScript.getOrMakeCameraCustomization(accessor).position = arg1.isnil() ? null : LuaVector.checkOrNew(arg1).asV3f();
                    return NIL;
                }
            });

            ret.set("getRot", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    Vector3f rot = targetScript.getOrMakeCameraCustomization(accessor).rotation;
                    return rot == null ? NIL : LuaVector.of(rot);
                }
            });

            ret.set("setRot", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1) {
                    targetScript.getOrMakeCameraCustomization(accessor).rotation = arg1.isnil() ? null : LuaVector.checkOrNew(arg1).asV3f();
                    return NIL;
                }
            });

            ret.set("getPivot", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    Vector3f piv = targetScript.getOrMakeCameraCustomization(accessor).pivot;
                    return piv == null ? NIL : LuaVector.of(piv);
                }
            });

            ret.set("setPivot", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1) {
                    targetScript.getOrMakeCameraCustomization(accessor).pivot = arg1.isnil() ? null : LuaVector.checkOrNew(arg1).asV3f();
                    return NIL;
                }
            });

            return ret;
        }
    }
}
