package net.blancworks.figura.lua.api;

import net.blancworks.figura.lua.CustomScript;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3f;
import net.minecraft.util.math.Vector4f;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.world.World;
import org.luaj.vm2.LuaBoolean;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.ThreeArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;

import java.awt.*;
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
                    Vec3f arg1 = checkVec3(arg);

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

            set("hsvToRGB", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    Vector3f hsv = checkVec3(arg);

                    return getVector(hsvToRGB(hsv));
                }
            });


            set("rgbToHSV", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    Vector3f rgb = checkVec3(arg);

                    return getVector(rgbToHSV(rgb));
                }
            });

        }});
    }

    public static Vec3f checkVec3(LuaValue val) {
        LuaTable table = val.checktable();

        if (table.length() != 3)
            LuaValue.error("Invalid number of components in vector, 3 expected.");

        Vec3f ret = new Vec3f(
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

    public static LuaValue vec3fToLua(Vec3f v){
        LuaTable table = getVector(3);
        table.set("x", LuaValue.valueOf(v.getX()));
        table.set("y", LuaValue.valueOf(v.getY()));
        table.set("z", LuaValue.valueOf(v.getZ()));
        return table;
    }

    public static LuaTable getVector(Vector3f v){
        LuaTable ret = getVector(3);
        ret.set("x", v.getX());
        ret.set("y", v.getY());
        ret.set("z", v.getZ());
        return ret;
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

    public static Vector3f rgbToHSV(Vector3f rgb)
    {
        float r = rgb.getX();
        float g = rgb.getY();
        float b = rgb.getZ();

        // h, s, v = hue, saturation, value
        float cmax = Math.max(r, Math.max(g, b)); // maximum of r, g, b
        float cmin = Math.min(r, Math.min(g, b)); // minimum of r, g, b
        float diff = cmax - cmin; // diff of cmax and cmin.
        float h = -1, s = -1;

        // if cmax and cmax are equal then h = 0
        if (cmax == cmin)
            h = 0;

            // if cmax equal r then compute h
        else if (cmax == r)
            h = (60 * ((g - b) / diff) + 360) % 360;

            // if cmax equal g then compute h
        else if (cmax == g)
            h = (60 * ((b - r) / diff) + 120) % 360;

            // if cmax equal b then compute h
        else if (cmax == b)
            h = (60 * ((r - g) / diff) + 240) % 360;

        // if cmax equal zero
        if (cmax == 0)
            s = 0;
        else
            s = (diff / cmax) * 100;

        // compute v
        float v = cmax * 100;

        return new Vector3f(h,s,v);
    }

    public static Vector3f hsvToRGB(Vector3f hsv){
        float S = hsv.getY();
        float V = hsv.getZ();
        double H = hsv.getX();
        while (H < 0) { H += 360; };
        while (H >= 360) { H -= 360; };
        double R, G, B;
        if (V <= 0)
        { R = G = B = 0; }
        else if (S <= 0)
        {
            R = G = B = V;
        }
        else
        {
            double hf = H / 60.0;
            int i = (int)Math.floor(hf);
            double f = hf - i;
            double pv = V * (1 - S);
            double qv = V * (1 - S * f);
            double tv = V * (1 - S * (1 - f));
            switch (i)
            {

                // Red is the dominant color

                case 0:

                    // Just in case we overshoot on our math by a little, we put these here. Since its a switch it won't slow us down at all to put these here.

                case 6:
                    R = V;
                    G = tv;
                    B = pv;
                    break;

                // Green is the dominant color

                case 1:
                    R = qv;
                    G = V;
                    B = pv;
                    break;
                case 2:
                    R = pv;
                    G = V;
                    B = tv;
                    break;

                // Blue is the dominant color

                case 3:
                    R = pv;
                    G = qv;
                    B = V;
                    break;
                case 4:
                    R = tv;
                    G = pv;
                    B = V;
                    break;

                // Red is the dominant color

                case 5:
                case -1:
                    R = V;
                    G = pv;
                    B = qv;
                    break;

                // The color is not defined, we should throw an error.

                default:
                    R = G = B = V; // Just pretend its black/white
                    break;
            }
        }
        float r = MathHelper.clamp((int)(R * 255.0), 0, 255);
        float g = MathHelper.clamp((int)(G * 255.0), 0, 255);
        float b = MathHelper.clamp((int)(B * 255.0), 0, 255);

        return new Vector3f(r,g,b);
    }
}
