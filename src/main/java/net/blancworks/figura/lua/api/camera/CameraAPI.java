package net.blancworks.figura.lua.api.camera;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import net.blancworks.figura.CameraData;
import net.blancworks.figura.lua.CustomScript;
import net.blancworks.figura.lua.LuaUtils;
import net.blancworks.figura.lua.api.ReadOnlyLuaTable;
import net.blancworks.figura.lua.api.ScriptLocalAPITable;
import net.blancworks.figura.lua.api.math.LuaVector;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec2f;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;

public class CameraAPI {
    public static Identifier getID() {
        return new Identifier("default", "camera");
    }

    public static ReadOnlyLuaTable getForScript(CustomScript script) {
        return new ScriptLocalAPITable(script, new CameraTable(script));
    }
    private static class CameraTable extends ScriptLocalAPITable {
        CameraData data;
        public CameraTable(CustomScript script) {
            super(script);
            this.data = script.camera;
            super.setTable(getTable());
        }

        public LuaTable getTable() {
            LuaTable ret = new LuaTable();

            ret.set("getPos", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaVector.of(data.position);
                }
            });
            ret.set("setPos", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1) {
                    data.position = LuaVector.checkOrNew(arg1).asV3f();
                    return NIL;
                }
            });

            ret.set("getfpPos", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaVector.of(data.fpPosition);
                }
            });
            ret.set("setfpPos", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1) {
                    data.fpPosition = LuaVector.checkOrNew(arg1).asV3f();
                    return NIL;
                }
            });

            ret.set("getRot", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaVector.of(data.rotation);
                }
            });
            ret.set("setRot", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1) {
                    data.rotation = LuaVector.checkOrNew(arg1).asV2f();
                    return NIL;
                }
            });

            return ret;
        }
    }
}
