package net.blancworks.figura.lua.api.math;

import com.google.common.collect.ImmutableMap;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.math.*;
import org.jetbrains.annotations.NotNull;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;

import java.util.Iterator;
import java.util.Map;
import java.util.function.UnaryOperator;

public class LuaVector extends LuaValue implements Iterable<Float> {
    public static final int TYPE = LuaValue.TVALUE;

    private final float[] values;

    private final Map<String, LuaValue> luaValues = new ImmutableMap.Builder<String, LuaValue>()
            .put("distanceTo", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1) {
                    return LuaNumber.valueOf(_distanceTo(arg1));
                }
            })
            .put("getLength", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaNumber.valueOf(_length());
                }
            })
            .put("normalized", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return _normalized();
                }
            })
            .put("dot", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    return LuaValue.valueOf(_dot(arg));
                }
            })
            .put("cross", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    return _cross(arg);
                }
            })
            .put("angleTo", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    return LuaValue.valueOf(_angleTo(arg));
                }
            })
            .put("toRad", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return _toRad();
                }
            })
            .put("toDeg", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return _toDeg();
                }
            })
            .put("asTable", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return asTable();
                }
            })

            .build();

    public LuaVector(float... values) {
        if (values.length > 6) {
            throw new IllegalArgumentException("LuaVector cannot have more than 6 fields!");
        }
        this.values = values;
    }

    public static LuaValue of(Vector4f vec) {
        if (vec == null) return NIL;
        return new LuaVector(vec.getX(), vec.getY(), vec.getZ(), vec.getW());
    }

    public static LuaValue of(Vec3f vec) {
        if (vec == null) return NIL;
        return new LuaVector(vec.getX(), vec.getY(), vec.getZ());
    }

    public static LuaValue of(Vec3d vec) {
        if (vec == null) return NIL;
        return new LuaVector((float) vec.x, (float) vec.y, (float) vec.z);
    }

    public static LuaValue of(Vec3i vec) {
        if (vec == null) return NIL;
        return new LuaVector((float) vec.getX(), (float) vec.getY(), (float) vec.getZ());
    }

    public static LuaValue of(Vec2f vec) {
        if (vec == null) return NIL;
        return new LuaVector(vec.x, vec.y);
    }

    public static LuaValue of(LuaTable t) {
        FloatArrayList fal = new FloatArrayList();

        Varargs entry = t.next(LuaValue.NIL);
        for (int i = 0; i < 6; i++) {
            if (entry.arg1().isnil())
                break;

            LuaValue l = entry.arg(2);

            if (l.isnil())
                fal.add(0f);
            else if (l.isnumber())
                fal.add(l.tofloat());
            else if (l.istable() || l instanceof LuaVector) {
                LuaVector v = LuaVector.checkOrNew(l);
                fal.addElements(fal.size(), v.values);
            }
            else {
                LuaValue numb = l.tonumber();
                if (numb.isnil()) fal.add(0f);
                else fal.add(numb.tofloat());
            }

            entry = t.next(entry.arg1());
        }

        //ensure size
        if (fal.size() > 6)
            fal.size(6);

        return new LuaVector(fal.toFloatArray());
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
            return (LuaVector) of((LuaTable) val);
        }
        throw new LuaError("Not a Vector table!");
    }

    public Vector4f asV4f() {
        return new Vector4f(x(), y(), z(), w());
    }

    public Vec3f asV3f() {
        return new Vec3f(x(), y(), z());
    }

    public Vec3d asV3d() {
        return new Vec3d(x(), y(), z());
    }

    public Vec3i asV3iFloored() {
        return new Vec3i(Math.floor(x()), Math.floor(y()), Math.floor(z()));
    }

    public Vec3i asV3iCeiled() {
        return new Vec3i(Math.ceil(x()), Math.ceil(y()), Math.ceil(z()));
    }

    public Vec3i asV3iRounded() {
        return new Vec3i(Math.round(x()), Math.round(y()), Math.round(z()));
    }

    public Vec2f asV2f() {
        return new Vec2f(x(), y());
    }

    public BlockPos asBlockPos() {
        return new BlockPos(x(), y(), z());
    }

    public LuaTable asTable() {
        LuaTable tbl = new LuaTable();
        for (int i = 1; i < 7; i++) {
            tbl.insert(i, LuaValue.valueOf(this._get(i)));
        }
        return tbl;
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
        Float f = _get(key);
        if (f == null) return NIL;
        return LuaNumber.valueOf(f);
    }

    @Override
    public LuaValue get(LuaValue key) {
        return rawget(key);
    }

    @Override
    public LuaValue rawget(LuaValue key) {
        if (key.isnumber())
            return get(key.checkint());
        return get(key.tojstring());
    }

    @Override
    public LuaValue get(String key) {
        Float f = _get(key);
        if (f == null) return _functions(key);
        return LuaNumber.valueOf(f);
    }

    @Override
    public void set(LuaValue key, LuaValue value) {
        float f = value.checknumber().tofloat();

        if (key.isnumber()) {
            int index = key.checkint();

            if (index > 6 || index < 1)
                throw new LuaError("Index out of bounds");

            if (index <= _size())
                values[index - 1] = f;
        }
        else
            values[_getIndex(key.tojstring()) - 1] = f;
    }

    @Override
    public LuaValue tostring() {
        return LuaString.valueOf(this.toString());
    }

    public float x() {
        return _get(1);
    }

    public float y() {
        return _get(2);
    }

    public float z() {
        return _get(3);
    }

    public float w() {
        return _get(4);
    }

    public float t() {
        return _get(5);
    }

    public float h() {
        return _get(6);
    }

    public LuaVector _add(LuaVector vec) {
        int n = Math.max(_size(), vec._size());
        float[] vals = new float[n];
        for (int i = 0; i < n; i++) {
            vals[i] = _get(i + 1) + vec._get(i + 1);
        }
        return new LuaVector(vals);
    }

    public LuaVector _add(float f) {
        int n = _size();
        float[] vals = new float[n];
        for (int i = 0; i < n; i++) {
            vals[i] = _get(i + 1) + f;
        }
        return new LuaVector(vals);
    }

    public LuaVector _sub(LuaVector vec) {
        int n = Math.max(_size(), vec._size());
        float[] vals = new float[n];
        for (int i = 0; i < n; i++) {
            vals[i] = _get(i + 1) - vec._get(i + 1);
        }
        return new LuaVector(vals);
    }

    public LuaVector _sub(float f) {
        int n = _size();
        float[] vals = new float[n];
        for (int i = 0; i < n; i++) {
            vals[i] = _get(i + 1) - f;
        }
        return new LuaVector(vals);
    }

    public LuaVector _mul(LuaVector vec) {
        int n = Math.max(_size(), vec._size());
        float[] vals = new float[n];
        for (int i = 0; i < n; i++) {
            vals[i] = _get(i + 1) * vec._get(i + 1);
        }
        return new LuaVector(vals);
    }

    public LuaVector _mul(float f) {
        int n = _size();
        float[] vals = new float[n];
        for (int i = 0; i < n; i++) {
            vals[i] = _get(i + 1) * f;
        }
        return new LuaVector(vals);
    }

    public LuaVector _div(LuaVector vec) {
        int n = Math.max(_size(), vec._size());
        float[] vals = new float[n];
        for (int i = 0; i < n; i++) {
            vals[i] = _get(i + 1) / vec._get(i + 1);
        }
        return new LuaVector(vals);
    }

    public LuaVector _div(float f) {
        int n = _size();
        float[] vals = new float[n];
        for (int i = 0; i < n; i++) {
            vals[i] = _get(i + 1) / f;
        }
        return new LuaVector(vals);
    }

    public LuaValue _functions(String name) {
        LuaValue v = luaValues.get(name);

        if (v == null)
            return NIL;
        else return v;
    }

    public double _distanceTo(LuaValue vector) {
        LuaVector vec = check(vector);
        int n = Math.max(_size(), vec._size()); // Only calculate for as many values as actually exist between both vectors
        float s = 0; // Sum value
        for (int i = 1; i <= n; i++) {
            float a = this._get(i); // This vector's value at current index
            float b = vec._get(i); // The passed vector's value at current index
            if (a == 0 && b == 0) continue; // Do not operate on values that are zero for both
            if (a == 0) {
                s += (b * b); // Only square the non zero value
                continue;
            }
            if (b == 0) {
                s += (a * a); // Only square the non zero value
                continue;
            }
            s += Math.pow(b - a, 2); // Square the difference when both values are non zero
        }
        return Math.sqrt(s); // Square root of the sum of all values
    }

    public double _dot(LuaValue vector) {
        LuaVector vec = check(vector);
        int n = Math.max(_size(), vec._size());
        double s = 0d;
        for (int i = 1; i <= n; i++) {
            s += _get(i) * vec._get(i);
        }
        return s;
    }

    public LuaVector _cross(LuaValue vector){
        LuaVector vec = check(vector);
        int n = Math.max(_size(), vec._size());
        float[] vals = new float[n];
        for (int i = 0; i < n; i++) {
            int j = ((i + 1) % n) + 1;
            int k = ((i + 2) % n) + 1;
            vals[i] = _get(j) * vec._get(k) - _get(k) * vec._get(j);
        }
        return new LuaVector(vals);
    }

    public double _angleTo(LuaValue vector){
        LuaVector other = check(vector);
        return Math.acos(_dot(other) / (_length() * other._length()));
    }

    public double _lengthSqr(){
        int n = _size();
        float s = 0;
        for (int i = 1; i <= n; i++) {
            float v = this._get(i);
            if (v != 0) s += v * v;
        }
        return s;
    }
    
    public double _length() {
        // We can't cache the length, what if someone edits the vector????
        // Leave it up to users to cache it.
        // -Zandra
        
        // Caches the vector's length upon first request, to conserve performance on repeated length tests on the same vector
        // -Foundation
        
        return Math.sqrt(_lengthSqr());
    }

    public LuaVector _normalized() {
        int n = _size();
        float s = 0;
        float[] vals = new float[n];
        for (int i = 1; i <= n; i++) {
            float v = this._get(i);
            if (v != 0) s += v * v;
        }
        float r = MathHelper.fastInverseSqrt(s);
        for (int j = 0; j < n; j++) {
            float v = this._get(j + 1);
            if (v != 0) vals[j] = v * r;
        }
        return new LuaVector(vals);
    }

    public LuaVector _toRad() {
        int n = _size();
        float[] vals = new float[n];
        for (int i = 1; i <= n; i++) {
            float v = this._get(i);
            vals[i - 1] = (float) Math.toRadians(v);
        }
        return new LuaVector(vals);
    }

    public LuaVector _toDeg() {
        int n = _size();
        float[] vals = new float[n];
        for (int i = 1; i <= n; i++) {
            float v = this._get(i);
            vals[i - 1] = (float) Math.toDegrees(v);
        }
        return new LuaVector(vals);
    }

    public int _size() {
        return values.length;
    }

    public Float _get(Integer index) {
        if (index == null || index > 6 || index < 1) {
            return null;
        }
        if (index <= _size()) {
            return values[index - 1];
        }
        return 0f;
    }

    public Float _get(String name) {
        return _get(_getIndex(name));
    }

    public Integer _getIndex(String name) {
        return switch (name) {
            case "x", "r", "u", "pitch" -> 1;
            case "y", "g", "v", "yaw", "volume" -> 2;
            case "z", "b", "roll" -> 3;
            case "w", "a" -> 4;
            case "t" -> 5;
            case "h" -> 6;
            default -> null;
        };
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
        return String.format("vec: {x=%f, y=%f, z=%f, w=%f, t=%f, h=%f}", x(), y(), z(), w(), t(), h());
    }

    public Text toJsonText(UnaryOperator<Style> keyColor, UnaryOperator<Style> valColor) {
        return new LiteralText("").append("vec: {")
                .append(new LiteralText("\n  x").styled(keyColor)).append(" = ").append(new LiteralText(String.valueOf(x())).styled(valColor))
                .append(new LiteralText(",\n  y").styled(keyColor)).append(" = ").append(new LiteralText(String.valueOf(y())).styled(valColor))
                .append(new LiteralText(",\n  z").styled(keyColor)).append(" = ").append(new LiteralText(String.valueOf(z())).styled(valColor))
                .append(new LiteralText(",\n  w").styled(keyColor)).append(" = ").append(new LiteralText(String.valueOf(w())).styled(valColor))
                .append(new LiteralText(",\n  t").styled(keyColor)).append(" = ").append(new LiteralText(String.valueOf(t())).styled(valColor))
                .append(new LiteralText(",\n  h").styled(keyColor)).append(" = ").append(new LiteralText(String.valueOf(h())).styled(valColor))
                .append("\n}");
    }
}