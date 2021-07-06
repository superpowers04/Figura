package net.blancworks.figura.lua.api.particle;

import net.blancworks.figura.lua.CustomScript;
import net.blancworks.figura.lua.api.ReadOnlyLuaTable;
import net.blancworks.figura.lua.api.math.LuaVector;
import net.blancworks.figura.trust.PlayerTrustManager;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.*;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3f;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.Vibration;
import net.minecraft.world.World;
import net.minecraft.world.event.BlockPositionSource;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.TwoArgFunction;

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
            set("addParticle", new TwoArgFunction() {
                //deprecated
                @Deprecated
                @Override
                public LuaValue call(LuaValue arg1, LuaValue arg2) {
                    if(!arg1.isstring())
                        return NIL;

                    if(script.particleSpawnCount > script.playerData.getTrustContainer().getIntSetting(PlayerTrustManager.MAX_PARTICLES_ID))
                        return NIL;
                    script.particleSpawnCount++;
                    

                    ParticleType<?> targetType = particleTypes.get(arg1.checkjstring());
                    if(!(targetType instanceof DefaultParticleType))
                        return NIL;

                    LuaVector vec = LuaVector.checkOrNew(arg2);

                    World w = MinecraftClient.getInstance().world;

                    if (!MinecraftClient.getInstance().isPaused() && w != null) {
                        w.addParticle((DefaultParticleType) targetType,
                                vec.x(), vec.y(), vec.z(), vec.w(), vec.t(), vec.h()
                        );
                    }

                    return NIL;
                }

                @Override
                public LuaValue call(LuaValue arg1, LuaValue arg2, LuaValue arg3) {
                    //check for particle name
                    if(!arg1.isstring())
                        return NIL;

                    //check for trust settings particle count
                    if(script.particleSpawnCount > script.playerData.getTrustContainer().getIntSetting(PlayerTrustManager.MAX_PARTICLES_ID))
                        return NIL;
                    script.particleSpawnCount++;

                    //get particle
                    Identifier id = new Identifier(arg1.checkjstring());
                    ParticleType<?> targetType = particleTypes.get(id.toString());

                    //particle pos and speed
                    LuaVector vec = LuaVector.checkOrNew(arg2);

                    //world
                    World w = MinecraftClient.getInstance().world;

                    //add particle
                    if (!MinecraftClient.getInstance().isPaused() && w != null) {
                        if (targetType instanceof DefaultParticleType) {
                            w.addParticle((DefaultParticleType) targetType,
                                    vec.x(), vec.y(), vec.z(), vec.w(), vec.t(), vec.h()
                            );
                        }
                        else {
                            ParticleEffect particle;

                            switch (id.toString()) {
                                case "minecraft:dust" -> {
                                    LuaVector extra = LuaVector.checkOrNew(arg3);
                                    particle = new DustParticleEffect(new Vec3f(extra.x(), extra.y(), extra.z()), extra.w());
                                }
                                case "minecraft:falling_dust" -> {
                                    BlockState blockState = w.getRegistryManager().get(Registry.BLOCK_KEY).get(new Identifier(arg3.checkjstring())).getDefaultState();
                                    particle = new BlockStateParticleEffect(ParticleTypes.FALLING_DUST, blockState);

                                }
                                case "minecraft:block" -> {
                                    BlockState blockState = w.getRegistryManager().get(Registry.BLOCK_KEY).get(new Identifier(arg3.checkjstring())).getDefaultState();
                                    particle = new BlockStateParticleEffect(ParticleTypes.BLOCK, blockState);
                                }
                                case "minecraft:item" -> {
                                    ItemStack itemStack = w.getRegistryManager().get(Registry.ITEM_KEY).get(new Identifier(arg3.checkjstring())).getDefaultStack();
                                    particle = new ItemStackParticleEffect(ParticleTypes.ITEM, itemStack);
                                }
                                case "minecraft:dust_color_transition" -> {
                                    LuaVector vector = LuaVector.checkOrNew(arg3);
                                    particle = new DustColorTransitionParticleEffect(new Vec3f(vector.x(), vector.y(), vector.z()), new Vec3f(vec.w(), vec.t(), vec.h()), vector.w());
                                }
                                case "minecraft:vibration" -> {
                                    LuaVector vector = LuaVector.checkOrNew(arg3);
                                    BlockPos start = new BlockPos(vector.x(), vector.y(), vector.z());
                                    BlockPositionSource end = new BlockPositionSource(new BlockPos(vec.w(), vec.t(), vec.h()));

                                    particle = new VibrationParticleEffect(new Vibration(start, end, (int) vector.w()));
                                }
                                default -> throw new LuaError("Couldnt find " + id + " particle" + (!id.getNamespace().equals("minecraft") ? "\nOnly vanilla particles are supported" : ""));
                            }

                            w.addParticle(particle,
                                    vec.x(), vec.y(), vec.z(), vec.w(), vec.t(), vec.h()
                            );
                        }
                    }

                    return NIL;
                }
            });
        }});
    }
}
