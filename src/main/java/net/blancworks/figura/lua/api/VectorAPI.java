package net.blancworks.figura.lua.api;

import net.blancworks.figura.lua.CustomScript;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.client.util.math.Vector4f;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec2f;
import net.minecraft.world.World;
import org.luaj.vm2.LuaBoolean;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.ThreeArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;

import java.util.ArrayList;

public class VectorAPI {

    public static final ArrayList<String> componentNames = new ArrayList<String>() {{
        add("x,u");
        add("y,v");
        add("z");
        add("w");
        add("t");
        add("h");
    }};
    private static ReadOnlyLuaTable globalLuaTable;
    private static World lastWorld;

    private static World getWorld() {
        return MinecraftClient.getInstance().world;
    }

    public static Identifier getID() {
        return new Identifier("default", "vectors");
    }

    public static ReadOnlyLuaTable getForScript(CustomScript script) {

        if (getWorld() != lastWorld)
            updateGlobalTable();

        return globalLuaTable;
    }

    public static void updateGlobalTable() {
        globalLuaTable = new ReadOnlyLuaTable(new LuaTable(){{
            set("partToWorld", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    Vector3f arg1 = checkVec3(arg);

                    arg1.scale(16);
                    arg1.set(-arg1.getX(), -arg1.getY(), arg1.getZ());

                    return vec3fToLua(arg1);
                }
            });

            set("getVector", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    int count = arg.checkint();

                    return getVector(count);
                }
            });
        }});
    }

    public static Vector3f checkVec3(LuaValue val) {
        LuaTable table = val.checktable();

        if (table.length() != 3)
            LuaValue.error("Invalid number of components in vector, 3 expected.");

        Vector3f ret = new Vector3f(
                table.get(1).checknumber().tofloat(),
                table.get(2).checknumber().tofloat(),
                table.get(3).checknumber().tofloat()
        );

        return ret;
    }

    public static Vector4f checkVec4f(LuaValue val) {
        LuaTable table = val.checktable();

        if (table.length() != 4)
            LuaValue.error("Invalid number of components in vector, 4 expected.");

        Vector4f ret = new Vector4f(
                table.get(1).checknumber().tofloat(),
                table.get(2).checknumber().tofloat(),
                table.get(3).checknumber().tofloat(),
                table.get(4).checknumber().tofloat()
        );

        return ret;
    }

    public static Vec2f checkVec2f(LuaValue val) {
        LuaTable table = val.checktable();

        if (table.length() != 2)
            LuaValue.error("Invalid number of components in vector, 2 expected.");

        Vec2f ret = new Vec2f(
                table.get(1).checknumber().tofloat(),
                table.get(2).checknumber().tofloat()
        );

        return ret;
    }

    public static LuaValue vec3fToLua(Vector3f v){
        LuaTable table = getVector(3);
        table.set("x", LuaValue.valueOf(v.getX()));
        table.set("y", LuaValue.valueOf(v.getY()));
        table.set("z", LuaValue.valueOf(v.getZ()));
        return table;
    }

    public static LuaTable getVector(int componentCount) {
        LuaTable ret = new LuaTable();
        LuaTable metaTableSource = new LuaTable();
        LuaTable altAccessors = new LuaTable();

        componentCount = Math.min(componentCount, componentNames.size());

        for (int i = 0; i < componentCount; i++) {
            String name = componentNames.get(i);

            ret.set(i, LuaValue.valueOf(0));

            for (String s : name.split(",")) {
                altAccessors.set(s, i+1);
            }
        }

        metaTableSource.set("__index", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue table, LuaValue key) {
                LuaTable tbl = table.checktable();

                if (!key.isint()) {
                    LuaTable alts = tbl.get("alt_accessors").checktable();
                    LuaValue alt = alts.get(key);

                    if (!alt.isnil()) {
                        return table.rawget(alt);
                    }
                }

                return table.rawget(key);
            }
        });

        metaTableSource.set("__newindex", new ThreeArgFunction() {
            @Override
            public LuaValue call(LuaValue table, LuaValue key, LuaValue value) {
                LuaTable tbl = table.checktable();

                if (!key.isint()) {
                    LuaTable alts = tbl.get("alt_accessors").checktable();
                    LuaValue alt = alts.get(key);

                    if (!alt.isnil()) {
                        table.rawset(alt.tonumber().toint(), value);
                        return LuaBoolean.valueOf(true);
                    }
                } else {
                    table.rawset(key, value);
                }

                return LuaBoolean.valueOf(true);
            }
        });

        ret.set("alt_accessors", altAccessors);
        ret.setmetatable(new ReadOnlyLuaTable(metaTableSource));
        return ret;
    }
}
