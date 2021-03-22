package net.blancworks.figura.lua.api.world.block;

import net.blancworks.figura.lua.CustomScript;
import net.blancworks.figura.lua.LuaUtils;
import net.blancworks.figura.lua.api.ReadOnlyLuaTable;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;

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
