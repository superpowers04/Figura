package net.blancworks.figura.lua.api;

import net.minecraft.nbt.*;
import org.luaj.vm2.*;

import java.util.HashMap;
import java.util.function.Function;

public class NBTAPI {
    public Tag target;

    public static HashMap<Class, Function<Tag, LuaValue>> nbtConverters = new HashMap<Class, Function<Tag, LuaValue>>(){{
        put(CompoundTag.class, NBTAPI::fromCompoundTag);
        put(IntTag.class, NBTAPI::fromIntTag);
        put(ShortTag.class, NBTAPI::fromShortTag);
        put(ByteTag.class, NBTAPI::fromByteTag);
        put(FloatTag.class, NBTAPI::fromFloatTag);
        put(LongTag.class, NBTAPI::fromLongTag);
        put(StringTag.class, NBTAPI::fromStringTag);

        put(IntArrayTag.class, NBTAPI::fromIntTag);
        put(ByteArrayTag.class, NBTAPI::fromByteArrayTag);
        put(LongArrayTag.class, NBTAPI::fromLongArrayTag);
        put(ListTag.class, NBTAPI::fromListTag);
    }};


    //Automatically determines the tag type and converts it.
    public static LuaValue fromTag(Tag tag){
        if(tag == null)
            return LuaValue.NIL;

        Class getClass = tag.getClass();
        Function<Tag, LuaValue> builder = nbtConverters.get(getClass);
        if(builder == null) return LuaValue.NIL;
        return builder.apply(tag);
    }

    public static LuaValue fromCompoundTag(Tag tag){
        LuaTable table = new LuaTable();
        CompoundTag t = (CompoundTag) tag;

        for (String key : t.getKeys()) {
            table.set(key.toLowerCase(), fromTag(t.get(key)));
        }
        return new ReadOnlyLuaTable(table);
    }

    public static LuaValue fromIntTag(Tag tag){
        return LuaInteger.valueOf(((IntTag)tag).getInt());
    }
    public static LuaValue fromByteTag(Tag tag){
        return LuaBoolean.valueOf(((ByteTag)tag).getByte() == 1);
    }
    public static LuaValue fromShortTag(Tag tag){
        return LuaNumber.valueOf(((ShortTag)tag).getShort());
    }

    public static LuaValue fromFloatTag(Tag tag){
        return LuaNumber.valueOf(((FloatTag)tag).getFloat());
    }
    public static LuaValue fromLongTag(Tag tag){
        return LuaNumber.valueOf(((LongTag)tag).getLong());
    }
    public static LuaValue fromStringTag(Tag tag){
        return LuaString.valueOf(((StringTag)tag).asString());
    }


    public static LuaValue fromIntArrayTag(Tag tag){
        LuaTable table = new LuaTable();
        IntArrayTag t = (IntArrayTag) tag;

        for (int i = 0; i < t.size(); i++) {
            table.set(i, fromTag(t.get(i)));
        }

        return new ReadOnlyLuaTable(table);
    }
    public static LuaValue fromByteArrayTag(Tag tag){
        LuaTable table = new LuaTable();
        ByteArrayTag t = (ByteArrayTag) tag;

        for (int i = 0; i < t.size(); i++) {
            table.set(i, fromTag(t.get(i)));
        }

        return new ReadOnlyLuaTable(table);
    }
    public static LuaValue fromLongArrayTag(Tag tag){
        LuaTable table = new LuaTable();
        LongArrayTag t = (LongArrayTag) tag;

        for (int i = 0; i < t.size(); i++) {
            table.set(i, fromTag(t.get(i)));
        }

        return new ReadOnlyLuaTable(table);
    }
    public static LuaValue fromListTag(Tag tag){
        LuaTable table = new LuaTable();
        ListTag t = (ListTag) tag;

        for (int i = 0; i < t.size(); i++) {
            table.set(i, fromTag(t.get(i)));
        }
        return new ReadOnlyLuaTable(table);
    }
}
