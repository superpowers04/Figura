package net.blancworks.figura.lua.api.particle;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import net.blancworks.figura.lua.CustomScript;
import net.blancworks.figura.lua.LuaUtils;
import net.blancworks.figura.lua.api.ReadOnlyLuaTable;
import net.blancworks.figura.lua.api.math.LuaVector;
import net.minecraft.client.MinecraftClient;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.particle.ParticleType;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.TwoArgFunction;

import java.util.HashMap;

public class ParticleAPI {
    
    public static HashMap<String, DefaultParticleType> particleTypes = new HashMap<String, DefaultParticleType>(){{
        for (Identifier id : Registry.PARTICLE_TYPE.getIds()) {
            ParticleType<?> type = Registry.PARTICLE_TYPE.get(id);
            if(type instanceof DefaultParticleType) {
                put(id.getPath(), (DefaultParticleType) type);
                put(id.toString(), (DefaultParticleType) type);
            }
        }
    }};
    
    private static final ReadOnlyLuaTable globalLuaTable = new ReadOnlyLuaTable(new LuaTable() {{
        set("addParticle", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue arg1, LuaValue arg2) {
                if(!arg1.isstring())
                    return NIL;

                DefaultParticleType targetType = particleTypes.get(arg1.checkjstring());
                if(targetType == null)
                    return NIL;

                LuaVector vec = LuaVector.checkOrNew(arg2);
                
                World w = MinecraftClient.getInstance().world;

                w.addParticle(targetType,
                        vec.x(), vec.y(), vec.z(), vec.w(), vec.t(), vec.h()
                );

                return NIL;
            }
        });
    }});

    public static Identifier getID() {
        return new Identifier("default", "particle");
    }

    public static ReadOnlyLuaTable getForScript(CustomScript script) {
        return globalLuaTable;
    }
}
