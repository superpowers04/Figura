package net.blancworks.figura.lua.api.math;

import net.blancworks.figura.lua.CustomScript;
import net.blancworks.figura.lua.api.ReadOnlyLuaTable;
import net.blancworks.figura.utils.ColorUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.ThreeArgFunction;

import java.awt.*;

public class VectorAPI {
    private static ReadOnlyLuaTable globalLuaTable;
    private static boolean initialized;

    private static World getWorld() {
        return MinecraftClient.getInstance().world;
    }

    public static Identifier getID() {
        return new Identifier("default", "vectors");
    }

    public static ReadOnlyLuaTable getForScript(CustomScript script) {
        if (!initialized) {
            updateGlobalTable();
            initialized = true;
        }

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
                    LuaVector a = LuaVector.checkOrNew(arg1);
                    LuaVector b = LuaVector.checkOrNew(arg2);
                    arg3.checknumber();
                    return lerp(a, b, arg3.tofloat());
                }
            });

            set("rgbToINT", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    LuaVector rgb = LuaVector.checkOrNew(arg);
                    return LuaValue.valueOf(intFromRGB(rgb));
                }
            });

            set("intToRGB", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    int i = arg.checkint();
                    return RGBfromInt(i);
                }
            });

            set("rgbToHSV", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    LuaVector rgb = LuaVector.checkOrNew(arg);
                    return toHSV(rgb);
                }
            });

            set("hsvToRGB", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    LuaVector hsv = LuaVector.checkOrNew(arg);
                    return toRGB(hsv);
                }
            });

            set("worldToPart", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    LuaVector vec = LuaVector.checkOrNew(arg);
                    return toModelSpace(vec);
                }
            });

            set("getVector", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    int n = arg.checkint();
                    return new LuaVector(new float[n]);
                }
            });
        }});
    }

    public static LuaVector lerp(LuaVector first, LuaVector second, float delta) {
        int n = Math.max(first._size(), second._size());
        float[] vals = new float[n];
        for (int i = 0; i < n; i++) {
            vals[i] = MathHelper.lerp(delta, first._get(i + 1), second._get(i + 1));
        }
        return new LuaVector(vals);
    }

    public static LuaVector toHSV(LuaVector rgb) {
        float[] hsv = new float[3];
        Color.RGBtoHSB((int)(rgb.x() * 255), (int)(rgb.y() * 255), (int)(rgb.z() * 255), hsv);
        return new LuaVector(hsv);
    }

    public static LuaVector toRGB(LuaVector hsv) {
        int c = Color.HSBtoRGB(hsv.x(), hsv.y(), hsv.z());
        int[] rgb = new int[3];
        ColorUtils.split(c, rgb);
        return new LuaVector(((float)rgb[0]) / 255, ((float)rgb[1]) / 255, ((float)rgb[2]) / 255);
    }

    public static LuaVector RGBfromInt(int rgb) {
        int[] c = new int[3];
        ColorUtils.split(rgb, c);
        return new LuaVector(((float)c[0]) / 255, ((float)c[1]) / 255, ((float)c[2]) / 255);
    }

    public static int intFromRGB(LuaVector rgb) {
        int c = (int) (rgb.x() * 255);
        c = (c << 8) + (int) (rgb.y() * 255);
        c = (c << 8) + (int) (rgb.z() * 255);
        return c;
    }

    private static final LuaVector[] MODEL_SPACE_FACTORS = new LuaVector[7];

    public static LuaVector toModelSpace(LuaVector vec) {
        return vec._mul(MODEL_SPACE_FACTORS[vec._size()]); // Multiplies the vector by the correct size vector in the factors array
    }

    static {
        // Store seven vectors with sizes ranging from 0 to 6, as factors for converting from world to model space
        for (int i = 0; i < 7; i++) {
            // Create a float array with size i
            float[] vals = new float[i];
            // Iterate through this array
            for (int j = 0; j < i; j++) {
                if(j == 0) vals[j] = -16; // Set the x value to -16
                else if(j == 1) vals[j] = -16; // Set the y value to -16
                else vals[j] = 16; // Set all other values to 16
            }
            MODEL_SPACE_FACTORS[i] = new LuaVector(vals); // Put this vector to the factors array
        }
    }
}
