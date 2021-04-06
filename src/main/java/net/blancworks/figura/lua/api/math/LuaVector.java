package net.blancworks.figura.lua.api.math;

import net.minecraft.client.util.math.Vector3f;
import net.minecraft.client.util.math.Vector4f;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import org.jetbrains.annotations.NotNull;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;

import java.util.Iterator;

public class LuaVector extends LuaValue implements Iterable<Float> {
    public static final int TYPE = LuaValue.TVALUE;

    public final float x, y, z, w, t, h;
    private Double cachedLength = null;

    public LuaVector(float x, float y, float z, float w, float t, float h) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
        this.t = t;
        this.h = h;
    }

    public static LuaVector of(Vector4f vec) {
        return new LuaVector(vec.getX(), vec.getY(), vec.getZ(), vec.getW(), 0, 0);
    }

    public static LuaVector of(Vector3f vec) {
        return new LuaVector(vec.getX(), vec.getY(), vec.getZ(), 0, 0, 0);
    }

    public static LuaVector of(Vec3d vec) {
        return new LuaVector((float) vec.x, (float) vec.y, (float) vec.z, 0, 0, 0);
    }

    public static LuaVector of(Vec3i vec) {
        return new LuaVector((float) vec.getX(), (float) vec.getY(), (float) vec.getZ(), 0, 0, 0);
    }

    public static LuaVector of(Vec2f vec) {
        return new LuaVector(vec.x, vec.y, 0, 0, 0, 0);
    }

    public static LuaVector of(LuaTable t) {
        int n = t.length();
        float[] arr = new float[6];
        for (int i = 0; i < 6; i++) {
            if (i < n) {
                LuaValue l =  t.get(i + 1);
                l.checknumber();
                arr[i] = l.tofloat();
            }
            else arr[i] = 0;
        }
        return new LuaVector(arr[0], arr[1], arr[2], arr[3], arr[4], arr[5]);
    }

    public static LuaVector check(LuaValue val) {
        if (val instanceof LuaVector) {
            return (LuaVector) val;
        }
        throw new LuaError("Not a Vector table!");
    }

    public static LuaVector checkOrNew(LuaValue val) {
        if (val instanceof LuaVector) {
            return (LuaVector) val;
        } else if (val.istable()) {
            return of((LuaTable)val);
        }
        throw new LuaError("Not a Vector table!");
    }

    public Vector4f asV4f() {
        return new Vector4f(x, y, z, w);
    }

    public Vector3f asV3f() {
        return new Vector3f(x, y, z);
    }

    public Vec3d asV3d() {
        return new Vec3d(x, y, z);
    }

    public Vec2f asV2f() {
        return new Vec2f(x, y);
    }

    @Override
    public int type() {
        return TYPE;
    }

    @Override
    public String typename() {
        return "vector";
    }

    @Override
    public LuaValue add(LuaValue rhs) {
        if (rhs.isnumber()) return _add(rhs.tofloat());
        return _add(check(rhs));
    }

    @Override
    public LuaValue sub(LuaValue rhs) {
        if (rhs.isnumber()) return _sub(rhs.tofloat());
        return _sub(check(rhs));
    }

    @Override
    public LuaValue mul(LuaValue rhs) {
        if (rhs.isnumber()) return _mul(rhs.tofloat());
        return _mul(check(rhs));
    }

    @Override
    public LuaValue div(LuaValue rhs) {
        if (rhs.isnumber()) return _div(rhs.tofloat());
        return _div(check(rhs));
    }

    @Override
    public LuaValue get(int key) {
        //System.out.println("INT GET");
        Float f = _get(key + 1);
        if (f == null) return NIL;
        return LuaNumber.valueOf(f);
    }

    @Override
    public LuaValue get(LuaValue key) {
        //System.out.println("VALUE GET");
        return rawget(key);
    }

    @Override
    public LuaValue rawget(LuaValue key) {
        return get(key.tojstring());
    }

    @Override
    public LuaValue get(String key) {
        Float f = _get(key);
        if (f == null) return _functions(key);
        return LuaNumber.valueOf(f);
    }

    @Override
    public LuaValue tostring() {
        return LuaString.valueOf(this.toString());
    }

    public LuaVector _add(LuaVector vec) {
        return new LuaVector(x + vec.x, y + vec.y, z + vec.z, w + vec.w, t + vec.t, h + vec.h);
    }

    public LuaVector _add(float f) {
        return new LuaVector(x + f, y + f, z + f, w + f, t + f, h + f);
    }

    public LuaVector _sub(LuaVector vec) {
        return new LuaVector(x - vec.x, y - vec.y, z - vec.z, w - vec.w, t - vec.t, h - vec.h);
    }

    public LuaVector _sub(float f) {
        return new LuaVector(x - f, y - f, z - f, w - f, t - f, h - f);
    }

    public LuaVector _mul(LuaVector vec) {
        return new LuaVector(x * vec.x, y * vec.y, z * vec.z, w * vec.w, t * vec.t, h * vec.h);
    }

    public LuaVector _mul(float f) {
        return new LuaVector(x * f, y * f, z * f, w * f, t * f, h * f);
    }

    public LuaVector _div(LuaVector vec) {
        return new LuaVector(x / vec.x, y / vec.y, z / vec.z, w / vec.w, t / vec.t, h / vec.h);
    }

    public LuaVector _div(float f) {
        return new LuaVector(x / f, y / f, z / f, w / f, t / f, h / f);
    }

    public LuaValue _functions(String name) {
        switch (name) {
            case "distanceTo":
                return new OneArgFunction() {
                    @Override
                    public LuaValue call(LuaValue arg1) {
                        return LuaNumber.valueOf(_distanceTo(arg1));
                    }
                };
            case "vecLength":
                return new ZeroArgFunction() {
                    @Override
                    public LuaValue call() {
                        return LuaNumber.valueOf(_length());
                    }
                };
            case "normalized":
                return new ZeroArgFunction() {
                    @Override
                    public LuaValue call() {
                        return _normalized();
                    }
                };
            default:
                return NIL;
        }
    }

    public double _distanceTo(LuaValue vector) {
        LuaVector vec = check(vector);
        float s = 0;
        for (int i = 0; i < 6; i++) {
            float a = this._get(i);
            float b = vec._get(i);
            if(a == 0 && b == 0) continue;
            if(a == 0) {
                s += (b * b);
                continue;
            }
            if(b == 0) {
                s += (a * a);
                continue;
            }
            s += Math.pow(b - a, 2);
        }
        return Math.sqrt(s);
    }

    public double _length() {
        if (cachedLength == null) {
            float s = 0;
            for (int i = 0; i < 6; i++) {
                float v = this._get(i);
                if(v == 0) continue;
                s += v * v;
            }
            cachedLength = Math.sqrt(s);
        }
        return cachedLength;
    }

    public LuaVector _normalized() {
        float s = 0;
        for (int i = 0; i < 6; i++) {
            float v = this._get(i);
            if(v == 0) continue;
            s += v * v;
        }
        float r = MathHelper.fastInverseSqrt(s);
        return new LuaVector(x * r, y * r, z * r, w * r, t * r, h * r);
    }

    public Float _get(int index) {
        switch (index) {
            case 0:
                return x;
            case 1:
                return y;
            case 2:
                return z;
            case 3:
                return w;
            case 4:
                return t;
            case 5:
                return h;
            default:
                return null;
        }
    }

    public Float _get(String name) {
        switch (name) {
            case "x":
            case "r":
            case "u":
            case "pitch":
                return x;
            case "y":
            case "g":
            case "v":
            case "yaw":
                return y;
            case "z":
            case "b":
            case "roll":
                return z;
            case "w":
            case "a":
                return w;
            case "t":
                return t;
            case "h":
                return h;
            default:
                return null;
        }
    }

    @NotNull
    @Override
    public Iterator<Float> iterator() {
        return new Iter(this);
    }

    public static class Iter implements Iterator<Float> {
        private final LuaVector vector;
        private int index;

        public Iter(LuaVector vector) {
            this.vector = vector;
        }

        @Override
        public boolean hasNext() {
            return index < 6;
        }

        @Override
        public Float next() {
            Float r = vector._get(index);
            index++;
            if (r == null) throw new IllegalStateException("Iterator at invalid index!");
            return r;
        }
    }

    @Override
    public String toString() {
        return String.format("vector: x=%f, y=%f, z=%f, w=%f, t=%f, h=%f", x, y, z, w, t, h);
    }
}
