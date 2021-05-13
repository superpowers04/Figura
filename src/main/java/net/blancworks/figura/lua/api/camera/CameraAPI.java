package net.blancworks.figura.lua.api.camera;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import net.blancworks.figura.CameraData;
import net.blancworks.figura.lua.CustomScript;
import net.blancworks.figura.lua.LuaUtils;
import net.blancworks.figura.lua.api.ReadOnlyLuaTable;
import net.blancworks.figura.lua.api.ScriptLocalAPITable;
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
                    return LuaUtils.getTableFromVector3f(data.position);
                }
            });
            ret.set("setPos", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1) {
                    FloatArrayList fas = LuaUtils.getFloatsFromTable(arg1.checktable());
                    data.position = new Vector3f(
                            fas.getFloat(0),
                            fas.getFloat(1),
                            fas.getFloat(2)
                    );

                    return NIL;
                }
            });

            ret.set("getfpPos", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaUtils.getTableFromVector3f(data.fpPosition);
                }
            });
            ret.set("setfpPos", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1) {
                    FloatArrayList fas = LuaUtils.getFloatsFromTable(arg1.checktable());
                    data.fpPosition = new Vector3f(
                            fas.getFloat(0),
                            fas.getFloat(1),
                            fas.getFloat(2)
                    );

                    return NIL;
                }
            });

            ret.set("getRot", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaUtils.getTableFromVec2f(data.rotation);
                }
            });
            ret.set("setRot", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1) {
                    FloatArrayList fas = LuaUtils.getFloatsFromTable(arg1.checktable());
                    data.rotation = new Vec2f(
                            fas.getFloat(0),
                            fas.getFloat(1)
                    );

                    return NIL;
                }
            });

            return ret;
        }
    }
}
