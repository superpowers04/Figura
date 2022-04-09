package net.blancworks.figura.lua.api.network;

import com.google.common.io.LittleEndianDataInputStream;
import com.google.common.io.LittleEndianDataOutputStream;
import net.blancworks.figura.lua.api.math.LuaVector;
import org.luaj.vm2.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.Queue;

@SuppressWarnings("UnstableApiUsage")
public class LuaNetworkReadWriter {

    public static final byte TABLE_ID = 0;
    public static final byte INT_ID = 1;
    public static final byte FLOAT_ID = 2;
    public static final byte BOOL_ID = 3;
    public static final byte STRING_ID = 4;
    public static final byte NIL_ID = 5;
    public static final byte VECTOR_ID = 6;

    public static void writeLuaValue(LuaValue val, LittleEndianDataOutputStream stream) throws Exception {
        if (val instanceof LuaVector vec) {
            writeLuaValue(vec, stream);
        } else if (val.isint()) {
            writeLuaValue(val.checkinteger(), stream);
        } else if (val.isnumber()) {
            writeLuaValue(val.checkdouble(), stream);
        } else if (val.isboolean()) {
            writeLuaValue(val.checkboolean(), stream);
        } else if (val.isstring()) {
            writeLuaValue(val.checkstring(), stream);
        } else if (val.istable()) {
            writeLuaValue(val.checktable(), stream);
        } else {
            stream.writeByte(NIL_ID);
        }
    }

    public static LuaValue readLuaValue(LittleEndianDataInputStream stream) throws IOException {
        byte type = stream.readByte();

        if (type == VECTOR_ID) {
            return readLuaVector(stream);
        } else if (type == TABLE_ID) {
            return readLuaTable(stream);
        } else if (type == INT_ID) {
            return readLuaInt(stream);
        } else if (type == FLOAT_ID) {
            return readLuaDouble(stream);
        } else if (type == BOOL_ID) {
            return readLuaBool(stream);
        } else if (type == STRING_ID) {
            return readLuaString(stream);
        }

        return LuaValue.NIL;
    }

    public static void writeLuaValue(LuaVector vec, LittleEndianDataOutputStream stream) throws Exception {
        stream.writeByte(VECTOR_ID);
        LuaTable tbl = vec.asTable();

        writeLuaValue(tbl, stream);
    }

    public static void writeLuaValue(LuaTable val, LittleEndianDataOutputStream stream) throws Exception {
        stream.writeByte(TABLE_ID);//Write ID

        Queue<LuaValue> keys = new LinkedList<>();
        Queue<LuaValue> values = new LinkedList<>();

        // Write value
        for (Varargs n = val.next(LuaValue.NIL); !n.arg1().isnil(); n = val.next(n.arg1())) {
            LuaValue key = n.arg1();
            LuaValue value = n.arg(2);

            if (key.istable())
                continue;

            keys.add(key);
            values.add(value);
        }

        stream.writeShort(keys.size());
        while (keys.size() > 0) {
            writeLuaValue(keys.poll(), stream); //Write index of this entry
            writeLuaValue(values.poll(), stream); //Write entry.
        }
    }

    public static void writeLuaValue(LuaInteger val, LittleEndianDataOutputStream stream) throws IOException {
        stream.writeByte(INT_ID);//Write ID
        stream.writeInt(val.v);// Write value
    }

    public static void writeLuaValue(double val, LittleEndianDataOutputStream stream) throws IOException {
        stream.writeByte(FLOAT_ID);//Write ID
        stream.writeFloat((float) val);// Write value
    }

    public static void writeLuaValue(boolean val, LittleEndianDataOutputStream stream) throws IOException {
        stream.writeByte(BOOL_ID);//Write ID
        stream.writeBoolean(val);// Write value
    }

    public static void writeLuaValue(LuaString val, LittleEndianDataOutputStream stream) throws Exception {
        String js = val.checkjstring();

        byte[] data = js.getBytes(StandardCharsets.US_ASCII);

        if (data.length > 1019) {
            throw new Exception("String is too large to send! Max string size is 1016 characters.");
        }

        stream.writeByte(STRING_ID); //Write ID
        stream.writeShort(data.length); // Write value
        stream.write(data);
    }

    public static LuaVector readLuaVector(LittleEndianDataInputStream stream) throws IOException {
        LuaTable tbl = (LuaTable) readLuaValue(stream);
        return (LuaVector) LuaVector.of(tbl);
    }

    public static LuaTable readLuaTable(LittleEndianDataInputStream stream) throws IOException {
        short count = stream.readShort();

        LuaTable table = new LuaTable();
        table.presize(count);

        for (int i = 0; i < count; i++) {
            LuaValue key = readLuaValue(stream);
            LuaValue value = readLuaValue(stream);

            table.set(key, value);
        }

        return table;
    }

    public static LuaInteger readLuaInt(LittleEndianDataInputStream stream) throws IOException {
        return LuaInteger.valueOf(stream.readInt());
    }

    public static LuaNumber readLuaDouble(LittleEndianDataInputStream stream) throws IOException {
        return LuaDouble.valueOf(stream.readFloat());
    }

    public static LuaBoolean readLuaBool(LittleEndianDataInputStream stream) throws IOException {
        return LuaBoolean.valueOf(stream.readBoolean());
    }

    public static LuaString readLuaString(LittleEndianDataInputStream stream) throws IOException {
        int count = stream.readShort();

        byte[] data = new byte[count];
        stream.read(data);

        return LuaString.valueOf(new String(data, StandardCharsets.US_ASCII));
    }

}
