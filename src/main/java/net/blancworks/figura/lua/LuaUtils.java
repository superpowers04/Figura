package net.blancworks.figura.lua;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import org.luaj.vm2.LuaDouble;
import org.luaj.vm2.LuaNumber;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

public class LuaUtils {

    public static Vector3f getVectorFromTable(LuaTable table) {
        if (table.length() != 3)
            return null;

        return null;
    }

    public static BlockPos getBlockPosFromTable(LuaTable table) {
        if (table.length() != 3)
            return null;
        
        return new BlockPos(
                table.get(1).checkint(),
                table.get(2).checkint(),
                table.get(3).checkint()
        );
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

    public static LuaTable getTableFromVec3d(Vec3d v){
        LuaTable targetTable = new LuaTable();
        LuaNumber x = LuaDouble.valueOf(v.getX());
        LuaNumber y = LuaDouble.valueOf(v.getY());
        LuaNumber z = LuaDouble.valueOf(v.getZ());

        targetTable.set(1, x);
        targetTable.set("x", x);

        targetTable.set(2, y);
        targetTable.set("y", y);

        targetTable.set(3, z);
        targetTable.set("z", z);
        return targetTable;
    }

    public static LuaTable getTableFromVector3f(Vector3f v){
        LuaTable targetTable = new LuaTable();
        LuaNumber x = LuaDouble.valueOf(v.getX());
        LuaNumber y = LuaDouble.valueOf(v.getY());
        LuaNumber z = LuaDouble.valueOf(v.getZ());

        targetTable.set(1, x);
        targetTable.set("x", x);

        targetTable.set(2, y);
        targetTable.set("y", y);

        targetTable.set(3, z);
        targetTable.set("z", z);
        return targetTable;
    }

    public static LuaTable getTableFromVec2f(Vec2f v){
        LuaTable targetTable = new LuaTable();
        LuaNumber x = LuaDouble.valueOf(v.x);
        LuaNumber y = LuaDouble.valueOf(v.y);

        targetTable.set(1, x);
        targetTable.set("x", x);

        targetTable.set(2, y);
        targetTable.set("y", y);
        return targetTable;
    }

    public static LuaTable fillTableFromVector3f(LuaTable targetTable, Vector3f v){
        LuaNumber x = LuaDouble.valueOf(v.getX());
        LuaNumber y = LuaDouble.valueOf(v.getY());
        LuaNumber z = LuaDouble.valueOf(v.getZ());
        
        targetTable.set(1, x);
        targetTable.set("x", x);

        targetTable.set(2, y);
        targetTable.set("y", y);

        targetTable.set(3, z);
        targetTable.set("z", z);
        return targetTable;
    }
}
