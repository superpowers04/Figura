package net.blancworks.figura.lua.api.math;

import net.blancworks.figura.lua.CustomScript;
import net.blancworks.figura.lua.api.ReadOnlyLuaTable;
import net.blancworks.figura.utils.ColorUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.client.util.math.Vector4f;
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
import org.luaj.vm2.lib.ZeroArgFunction;

import java.awt.*;
import java.util.ArrayList;

public class VectorAPI {

    public static final ArrayList<String> componentNames = new ArrayList<String>() {{
        add("x,u,r");
        add("y,v,g");
        add("z,b");
        add("w,a");
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
        globalLuaTable = new ReadOnlyLuaTable(new LuaTable() {{
            set("of", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    arg.checktable();
                    return LuaVector.of((LuaTable)arg);
                }
            });

            set("lerp", new ThreeArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1, LuaValue arg2, LuaValue arg3) {
                    LuaVector a = LuaVector.check(arg1);
                    LuaVector b = LuaVector.check(arg2);
                    arg3.checknumber();
                    return lerp(a, b, arg3.tofloat());
                }
            });

            set("RGBtoHSV", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    LuaVector rgb = LuaVector.check(arg);
                    return toHSV(rgb);
                }
            });

            set("HSVtoRGB", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    LuaVector hsv = LuaVector.check(arg);
                    return toRGB(hsv);
                }
            });
        }});
    }

    public static LuaVector lerp(LuaVector first, LuaVector second, float delta) {
        return new LuaVector(
                MathHelper.lerp(delta, first.x, second.x),
                MathHelper.lerp(delta, first.y, second.y),
                MathHelper.lerp(delta, first.z, second.z),
                MathHelper.lerp(delta, first.w, second.w),
                MathHelper.lerp(delta, first.t, second.t),
                MathHelper.lerp(delta, first.h, second.h)
        );
    }

    public static LuaVector toHSV(LuaVector rgb) {
        float[] hsv = new float[3];
        Color.RGBtoHSB((int)(rgb.x * 255), (int)(rgb.y * 255), (int)(rgb.z * 255), hsv);
        return new LuaVector(hsv[0], hsv[1], hsv[2], rgb.w, rgb.t, rgb.h);
    }

    public static LuaVector toRGB(LuaVector hsv) {
        int c = Color.HSBtoRGB(hsv.x, hsv.y, hsv.z);
        int[] rgb = new int[3];
        ColorUtils.split(c, rgb);
        return new LuaVector(((float)rgb[0]) / 255, ((float)rgb[1]) / 255, ((float)rgb[2]) / 255, hsv.w, hsv.t, hsv.h);
    }

    public static LuaVector RGBfromInt(int rgb) {
        int[] c = new int[3];
        ColorUtils.split(rgb, c);
        return new LuaVector(((float)c[0]) / 255, ((float)c[1]) / 255, ((float)c[2]) / 255, 0, 0, 0);
    }
}
