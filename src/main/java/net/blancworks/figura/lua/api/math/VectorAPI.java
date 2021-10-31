package net.blancworks.figura.lua.api.math;

import net.blancworks.figura.lua.CustomScript;
import net.blancworks.figura.lua.api.ReadOnlyLuaTable;
import net.blancworks.figura.utils.ColorUtils;
import net.blancworks.figura.utils.MathUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.*;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.ThreeArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;

import java.awt.*;

public class VectorAPI {
    private static ReadOnlyLuaTable globalLuaTable;
    private static boolean initialized;

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
                    return LuaVector.of(arg.checktable());
                }
            });

            set("lerp", new ThreeArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1, LuaValue arg2, LuaValue arg3) {
                    LuaVector a = LuaVector.checkOrNew(arg1);
                    LuaVector b = LuaVector.checkOrNew(arg2);
                    return lerp(a, b, arg3.checknumber().tofloat());
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

            set("worldToCameraPos", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    LuaVector vec = LuaVector.checkOrNew(arg);
                    return toCameraSpace(vec);
                }
            });

            set("getVector", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    int n = arg.checkint();
                    return new LuaVector(new float[n]);
                }
            });

            set("rotateWithQuaternion", new TwoArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1, LuaValue arg2) {
                    return rotateWithQuaternion(LuaVector.checkOrNew(arg1), LuaVector.checkOrNew(arg2));
                }
            });


            set("asTable", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    LuaVector vec = LuaVector.checkOrNew(arg);
                    return toTable(vec);
                }
            });

            set("toQuaternion", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    Quaternion q = Quaternion.fromEulerXyzDegrees(LuaVector.checkOrNew(arg).asV3f());
                    return LuaVector.of(new Vector4f(q.getX(), q.getY(), q.getZ(), q.getW()));
                }
            });

            set("fromQuaternion", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    LuaVector vec = LuaVector.checkOrNew(arg);
                    return LuaVector.of(MathUtils.quaternionToEulerXYZ(new Quaternion(vec.x(), vec.y(), vec.z(), vec.w())));
                }
            });

            set("worldToScreenSpace", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    return LuaVector.of(MathUtils.worldToScreenSpace(LuaVector.checkOrNew(arg).asV3f()));
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

    public static LuaValue rotateWithQuaternion(LuaVector vector, LuaVector rotation) {
        Quaternion quat = Quaternion.fromEulerXyzDegrees(vector.asV3f());
        Quaternion rot = Quaternion.fromEulerXyzDegrees(rotation.asV3f());
        quat.hamiltonProduct(rot);

        //we cant use the quaternion to euler from the quaternion class because NaN and weird rotations
        return LuaVector.of(MathUtils.quaternionToEulerXYZ(quat));
    }

    public static LuaTable toTable(LuaVector vector) {
        LuaTable tbl = new LuaTable();
        for (int i = 1; i < 7; i++) {
            tbl.insert(i, vector.get(i));
        }
        return tbl;
    }

    public static LuaValue toCameraSpace(LuaVector vec) {
        Matrix3f transformMatrix = new Matrix3f(MinecraftClient.getInstance().gameRenderer.getCamera().getRotation());
        transformMatrix.invert();
        Vec3f target = new Vec3f(vec.x(), vec.y(), vec.z());
        target.subtract(new Vec3f(MinecraftClient.getInstance().gameRenderer.getCamera().getPos()));
        target.transform(transformMatrix);
        target.set(-target.getX(), target.getY(), target.getZ());
        return LuaVector.of(target);
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
