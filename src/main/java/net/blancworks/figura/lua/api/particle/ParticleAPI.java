package net.blancworks.figura.lua.api.particle;

import net.blancworks.figura.lua.CustomScript;
import net.blancworks.figura.lua.api.ReadOnlyLuaTable;
import net.blancworks.figura.lua.api.math.LuaVector;
import net.blancworks.figura.trust.TrustContainer;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.*;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.Vibration;
import net.minecraft.world.World;
import net.minecraft.world.event.BlockPositionSource;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.VarArgFunction;

import java.util.AbstractMap;
import java.util.HashMap;

public class ParticleAPI {

    public static HashMap<String, ParticleType<?>> particleTypes = new HashMap<>() {{
        for (Identifier id : Registry.PARTICLE_TYPE.getIds()) {
            ParticleType<?> type = Registry.PARTICLE_TYPE.get(id);

            put(id.getPath(), type);
            put(id.toString(), type);
        }
    }};

    public static Identifier getID() {
        return new Identifier("default", "particle");
    }

    public static ReadOnlyLuaTable getForScript(CustomScript script) {
        return new ReadOnlyLuaTable(new LuaTable() {{
            set("addParticle", new VarArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1, LuaValue arg2) {
                    //setup particle
                    AbstractMap.Entry<Identifier, ParticleType<?>> particleType = particleSetup(script, arg1);
                    if (particleType == null || !(particleType.getValue() instanceof DefaultParticleType type)) return NIL;

                    //particle
                    summonParticle(type, LuaVector.checkOrNew(arg2));

                    return NIL;
                }

                @Override
                public LuaValue call(LuaValue arg1, LuaValue arg2, LuaValue arg3) {
                    //setup particle
                    AbstractMap.Entry<Identifier, ParticleType<?>> particleType = particleSetup(script, arg1);
                    if (particleType == null || particleType.getValue() instanceof DefaultParticleType) return NIL;

                    //particle special args
                    ParticleEffect particle;
                    switch (particleType.getKey().toString()) {
                        case "minecraft:dust" -> {
                            LuaVector color = LuaVector.checkOrNew(arg3);
                            particle = new DustParticleEffect(color.asV3f(), color.w());
                        }
                        case "minecraft:falling_dust" -> {
                            BlockState blockState = Registry.BLOCK.get(Identifier.tryParse(arg3.checkjstring())).getDefaultState();
                            particle = new BlockStateParticleEffect(ParticleTypes.FALLING_DUST, blockState);
                        }
                        case "minecraft:block" -> {
                            BlockState blockState = Registry.BLOCK.get(Identifier.tryParse(arg3.checkjstring())).getDefaultState();
                            particle = new BlockStateParticleEffect(ParticleTypes.BLOCK, blockState);
                        }
                        case "minecraft:item" -> {
                            ItemStack itemStack = Registry.ITEM.get(Identifier.tryParse(arg3.checkjstring())).getDefaultStack();
                            particle = new ItemStackParticleEffect(ParticleTypes.ITEM, itemStack);
                        }
                        default -> {
                            return NIL;
                        }
                    }

                    //add particle
                    summonParticle(particle, LuaVector.checkOrNew(arg2));

                    return NIL;
                }

                @Override
                public Varargs onInvoke(Varargs args) {
                    //argos
                    LuaValue arg1 = args.arg(1);
                    LuaValue arg2 = args.arg(2);
                    LuaValue arg3 = args.arg(3);
                    LuaValue arg4 = args.arg(4);

                    //setup particle
                    AbstractMap.Entry<Identifier, ParticleType<?>> particleType = particleSetup(script, arg1);
                    if (particleType == null || particleType.getValue() instanceof DefaultParticleType) return NIL;

                    //particle special args
                    ParticleEffect particle;
                    switch (particleType.getKey().toString()) {
                        case "minecraft:dust_color_transition" -> {
                            LuaVector fromColor = LuaVector.checkOrNew(arg3);
                            LuaVector toColor = LuaVector.checkOrNew(arg4);
                            particle = new DustColorTransitionParticleEffect(fromColor.asV3f(), toColor.asV3f(), fromColor.w());
                        }
                        case "minecraft:vibration" -> {
                            LuaVector start = LuaVector.checkOrNew(arg3);
                            LuaVector end = LuaVector.checkOrNew(arg4);
                            BlockPos startPos = new BlockPos(start.asV3d());
                            BlockPositionSource endPos = new BlockPositionSource(new BlockPos(end.asV3d()));

                            particle = new VibrationParticleEffect(new Vibration(startPos, endPos, (int) start.w()));
                        }
                        default -> {
                            return NIL;
                        }
                    }

                    //add particle
                    summonParticle(particle, LuaVector.checkOrNew(arg2));

                    return NIL;
                }
            });
        }});
    }

    private static AbstractMap.Entry<Identifier, ParticleType<?>> particleSetup(CustomScript script, LuaValue id) {
        //check string or script particle count
        if (!id.isstring() || script.particleSpawnCount > script.playerData.getTrustContainer().getTrust(TrustContainer.Trust.PARTICLES))
            return null;

        //increase particle count
        script.particleSpawnCount++;

        //get particle id and particle type
        Identifier identifier = new Identifier(id.checkjstring());
        ParticleType<?> type = particleTypes.get(identifier.toString());

        //return
        return type == null ? null : new AbstractMap.SimpleEntry<>(identifier, type);
    }

    private static void summonParticle(ParticleEffect particle, LuaVector pos) {
        World w = MinecraftClient.getInstance().world;

        //summon particle
        if (!MinecraftClient.getInstance().isPaused() && w != null) {
            w.addParticle(particle,
                    pos.x(), pos.y(), pos.z(), pos.w(), pos.t(), pos.h()
            );
        }
    }
}
