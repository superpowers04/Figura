package net.blancworks.figura.models.lua;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import net.minecraft.client.util.math.Vector3f;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

public class LuaUtils {

    public static Vector3f getVectorFromTable(LuaTable table) {
        if (table.length() != 3)
            return null;

        return null;
    }

    public static FloatArrayList getFloatsFromTable(LuaTable table) {
        FloatArrayList ret = new FloatArrayList();

        for (int i = 0; i < table.length(); i++) {
            LuaValue v = table.get(i+1);

            //Recursively add from tables.
            if (v.istable()) {
                ret.addAll(getFloatsFromTable(v.checktable()));
            } else if (v.isnumber()) {
                ret.add(v.checknumber().tofloat());
            }
        }

        return ret;
    }

}
