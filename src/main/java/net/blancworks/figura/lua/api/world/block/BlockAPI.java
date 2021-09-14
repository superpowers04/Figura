package net.blancworks.figura.lua.api.world.block;

import net.blancworks.figura.lua.CustomScript;
import net.blancworks.figura.lua.api.ReadOnlyLuaTable;
import net.minecraft.util.Identifier;
import org.luaj.vm2.LuaTable;

//Not implemented yet
//Eventually provides blocks, with properties and all, to lua.
public class BlockAPI {

    private static final ReadOnlyLuaTable globalLuaTable = new ReadOnlyLuaTable(new LuaTable() {{
        
        

    }});


    public static Identifier getID() {
        return new Identifier("default", "blocks");
    }

    public static ReadOnlyLuaTable getForScript(CustomScript script) {
        return globalLuaTable;
    }
}
