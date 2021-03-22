package net.blancworks.figura.lua.api.world;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import net.blancworks.figura.lua.CustomScript;
import net.blancworks.figura.lua.LuaUtils;
import net.blancworks.figura.lua.api.ReadOnlyLuaTable;
import net.blancworks.figura.lua.api.world.block.BlockStateAPI;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.BuiltinRegistries;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.ThreeArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;

public class WorldAPI {
    
    private static World getWorld(){
        return MinecraftClient.getInstance().world;
    }

    private static final ReadOnlyLuaTable globalLuaTable = new ReadOnlyLuaTable(new LuaTable() {{
        
        set("getBlockState", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue arg) {
                LuaTable getPosTable = arg.checktable();
                BlockPos pos = LuaUtils.getBlockPosFromTable(getPosTable);

                if(pos == null) return NIL;
                
                World w = getWorld();
                
                if(!w.isChunkLoaded(pos)) return NIL;
                
                BlockState state = w.getBlockState(pos);
                
                return BlockStateAPI.getTable(state);
            }
        });


        set("getTime", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                return LuaNumber.valueOf(getWorld().getTime());
            }
        });


        set("getTimeOfDay", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                return LuaNumber.valueOf(getWorld().getTimeOfDay());
            }
        });

        set("getMoonPhase", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                return LuaNumber.valueOf(getWorld().getMoonPhase());
            }
        });

        set("getLunarTime", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                return LuaNumber.valueOf(getWorld().getLunarTime());
            }
        });

        set("getRainGradient", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue a) {
                return LuaNumber.valueOf(getWorld().getRainGradient((float)(a.checkdouble())));
            }
        });

        set("getThunderGradient", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue a) {
                return LuaNumber.valueOf(getWorld().getThunderGradient((float)(a.checkdouble())));
            }
        });

        set("getLightLevel", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue a) {
                BlockPos pos = LuaUtils.getBlockPosFromTable(a.checktable());
                
                if(pos == null)
                    return LuaInteger.valueOf(0);
                
                int block = getWorld().getLightLevel(LightType.BLOCK, pos);
                int sky = getWorld().getLightLevel(LightType.SKY, pos);
                
                int ambientDark = getWorld().getAmbientDarkness();
                
                
                
                return LuaInteger.valueOf(Math.max(block, sky));
            }
        });

        set("getBiomeID", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue a) {

                BlockPos pos = LuaUtils.getBlockPosFromTable(a.checktable());

                if(pos == null)
                    return NIL;
                
                Biome b = getWorld().getBiome(pos);
                
                if(b == null)
                    return NIL;
                
                Identifier id = getWorld().getRegistryManager().get(Registry.BIOME_KEY).getId(b);
                
                return LuaString.valueOf(id.toString());
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
