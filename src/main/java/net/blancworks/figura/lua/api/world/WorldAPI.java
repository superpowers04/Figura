package net.blancworks.figura.lua.api.world;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import net.blancworks.figura.lua.CustomScript;
import net.blancworks.figura.lua.LuaUtils;
import net.blancworks.figura.lua.api.ReadOnlyLuaTable;
import net.blancworks.figura.lua.api.world.block.BlockStateAPI;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.ThreeArgFunction;

public class WorldAPI {

    private static final ReadOnlyLuaTable globalLuaTable = new ReadOnlyLuaTable(new LuaTable() {{
        
        set("getBlockState", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue arg) {
                LuaTable getPosTable = arg.checktable();
                BlockPos pos = LuaUtils.getBlockPosFromTable(getPosTable);

                if(pos == null) return NIL;
                
                World w = MinecraftClient.getInstance().world;
                
                if(!w.isChunkLoaded(pos)) return NIL;
                
                BlockState state = w.getBlockState(pos);
                
                return BlockStateAPI.getTable(state);
            }
        });
        
    }});


    public static Identifier getID() {
        return new Identifier("default", "world");
    }

    public static ReadOnlyLuaTable getForScript(CustomScript script) {
        return globalLuaTable;
    }
    
}
