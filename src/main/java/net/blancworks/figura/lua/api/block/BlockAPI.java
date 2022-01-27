package net.blancworks.figura.lua.api.block;

import net.blancworks.figura.lua.CustomScript;
import net.minecraft.util.Identifier;
import org.luaj.vm2.LuaTable;

//Not implemented yet
//Eventually provides blocks, with properties and all, to lua.
public class BlockAPI {

    private static final LuaTable globalLuaTable = new LuaTable();

    public static Identifier getID() {
        return new Identifier("default", "blocks");
    }

    public static LuaTable getForScript(CustomScript script) {
        return globalLuaTable;
    }
}
