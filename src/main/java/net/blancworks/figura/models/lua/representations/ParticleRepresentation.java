package net.blancworks.figura.models.lua.representations;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import net.blancworks.figura.models.lua.CustomScript;
import net.blancworks.figura.models.lua.LuaUtils;
import net.blancworks.figura.trust.PlayerTrustManager;
import net.blancworks.figura.trust.TrustContainer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleType;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import org.luaj.vm2.LuaNumber;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.ThreeArgFunction;

import java.util.HashMap;
import java.util.function.Consumer;

public class ParticleRepresentation extends LuaRepresentation {

    private float particleDensity = 0;
    private float maxParticles = 0;

    public static HashMap<String, Consumer<ParticleArgPair>> particleGenerators = new HashMap<String, Consumer<ParticleArgPair>>() {{
        put("dust", ParticleRepresentation::generateDustParticle);
    }};

    public ParticleRepresentation(CustomScript targetScript) {
        super(targetScript);

        for (Identifier id : Registry.PARTICLE_TYPE.getIds()) {
            ParticleType<?> type = Registry.PARTICLE_TYPE.get(id);
            if(type instanceof DefaultParticleType) {
                Consumer<ParticleArgPair> consumer = (a) -> {
                    generateFromType(a, (DefaultParticleType) type);
                };
                particleGenerators.put(id.getPath(), consumer);
            }
        }
        
        LuaTable particleTable = new LuaTable();
        
        particleTable.set("addParticle", new ThreeArgFunction() {
            @Override
            public LuaValue call(LuaValue arg1, LuaValue arg2, LuaValue arg3) {

                if (particleDensity > maxParticles)
                    return NIL;

                if (!arg1.isstring() || !arg2.istable() || !arg3.istable())
                    return NIL;


                String particleName = arg1.toString();
                Consumer<ParticleArgPair> consumer = particleGenerators.get(particleName);

                if (consumer == null)
                    return NIL;

                consumer.accept(new ParticleArgPair(){{
                    particlePropsArg = arg2.checktable();
                    posVelArg = arg3.checktable();
                }});

                return LuaNumber.valueOf(1);
            }
        });

        targetScript.scriptGlobals.set("particle", particleTable);
    }

    int tickCount = 0;
    
    @Override
    public void tick() {
        particleDensity -= (1.0f / 20) * maxParticles;
        
        tickCount++;
        
        if(tickCount > 20){
            maxParticles = getParticlesPerSecond();
            tickCount = 0;
        }
    }

    public static void generateDustParticle(ParticleArgPair args) {
        FloatArrayList posvel = LuaUtils.getFloatsFromTable(args.posVelArg.checktable());
        FloatArrayList dustProperties = LuaUtils.getFloatsFromTable(args.particlePropsArg);
        
        MinecraftClient.getInstance().particleManager.addParticle(
                new DustParticleEffect(
                        dustProperties.getFloat(0),dustProperties.getFloat(1),dustProperties.getFloat(2),
                        dustProperties.getFloat(3)
                ),
                posvel.getFloat(0),posvel.getFloat(1),posvel.getFloat(2),
                posvel.getFloat(3),posvel.getFloat(4),posvel.getFloat(5)
        );
    }
    
    public static void generateFromType(ParticleArgPair args, DefaultParticleType type){
        FloatArrayList posvel = LuaUtils.getFloatsFromTable(args.posVelArg.checktable());

        MinecraftClient.getInstance().particleManager.addParticle(type,
                posvel.getFloat(0),posvel.getFloat(1),posvel.getFloat(2),
                posvel.getFloat(3),posvel.getFloat(4),posvel.getFloat(5)
        );
    }

    public float getParticlesPerSecond() {
        TrustContainer tc = script.playerData.getTrustContainer();

        return (int) tc.getFloatSetting(PlayerTrustManager.maxParticlesID);
    }
    
    public static class ParticleArgPair{
        public LuaTable particlePropsArg;
        public LuaTable posVelArg;
    }
}
