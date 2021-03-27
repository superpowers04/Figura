package net.blancworks.figura.lua.api;

import net.minecraft.nbt.*;
import org.luaj.vm2.*;

import java.util.HashMap;
import java.util.function.Function;

public class NBTAPI {
    public NbtElement target;

    public static HashMap<Class, Function<NbtElement, LuaValue>> nbtConverters = new HashMap<Class, Function<NbtElement, LuaValue>>(){{
        put(NbtCompound.class, NBTAPI::fromCompoundTag);
        put(NbtInt.class, NBTAPI::fromIntTag);
        put(NbtShort.class, NBTAPI::fromShortTag);
        put(NbtByte.class, NBTAPI::fromByteTag);
        put(NbtFloat.class, NBTAPI::fromFloatTag);
        put(NbtLong.class, NBTAPI::fromLongTag);
        put(NbtString.class, NBTAPI::fromStringTag);

        put(NbtIntArray.class, NBTAPI::fromIntTag);
        put(NbtByteArray.class, NBTAPI::fromByteArrayTag);
        put(NbtLongArray.class, NBTAPI::fromLongArrayTag);
        put(NbtList.class, NBTAPI::fromListTag);
    }};


    //Automatically determines the tag type and converts it.
    public static LuaValue fromTag(NbtElement tag){
        if(tag == null)
            return LuaValue.NIL;

        Class getClass = tag.getClass();
        Function<NbtElement, LuaValue> builder = nbtConverters.get(getClass);
        if(builder == null) return LuaValue.NIL;
        return builder.apply(tag);
    }

    public static LuaValue fromCompoundTag(NbtElement tag){
        LuaTable table = new LuaTable();
        NbtCompound t = (NbtCompound) tag;

        for (String key : t.getKeys()) {
            table.set(key.toLowerCase(), fromTag(t.get(key)));
        }
        return new ReadOnlyLuaTable(table);
    }

    public static LuaValue fromIntTag(NbtElement tag){
        return LuaInteger.valueOf(((NbtInt)tag).intValue());
    }
    public static LuaValue fromByteTag(NbtElement tag){
        return LuaBoolean.valueOf(((NbtByte)tag).byteValue() == 1);
    }
    public static LuaValue fromShortTag(NbtElement tag){
        return LuaNumber.valueOf(((NbtShort)tag).shortValue());
    }

    public static LuaValue fromFloatTag(NbtElement tag){
        return LuaNumber.valueOf(((NbtFloat)tag).floatValue());
    }
    public static LuaValue fromLongTag(NbtElement tag){
        return LuaNumber.valueOf(((NbtLong)tag).longValue());
    }
    public static LuaValue fromStringTag(NbtElement tag){
        return LuaString.valueOf(((NbtString)tag).asString());
    }


    public static LuaValue fromIntArrayTag(NbtElement tag){
        LuaTable table = new LuaTable();
        NbtIntArray t = (NbtIntArray) tag;

        for (int i = 0; i < t.size(); i++) {
            table.set(i, fromTag(t.get(i)));
        }

        return new ReadOnlyLuaTable(table);
    }
    public static LuaValue fromByteArrayTag(NbtElement tag){
        LuaTable table = new LuaTable();
        NbtByteArray t = (NbtByteArray) tag;

        for (int i = 0; i < t.size(); i++) {
            table.set(i, fromTag(t.get(i)));
        }

        return new ReadOnlyLuaTable(table);
    }
    public static LuaValue fromLongArrayTag(NbtElement tag){
        LuaTable table = new LuaTable();
        NbtLongArray t = (NbtLongArray) tag;

        for (int i = 0; i < t.size(); i++) {
            table.set(i, fromTag(t.get(i)));
        }

        return new ReadOnlyLuaTable(table);
    }
    public static LuaValue fromListTag(NbtElement tag){
        LuaTable table = new LuaTable();
        NbtList t = (NbtList) tag;

        for (int i = 0; i < t.size(); i++) {
            table.set(i, fromTag(t.get(i)));
        }
        return new ReadOnlyLuaTable(table);
    }
}
