package net.blancworks.figura.lua.api;

import net.blancworks.figura.lua.CustomScript;
import net.blancworks.figura.lua.api.math.LuaVector;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;

public class BiomeAPI {

    public static Identifier getID() {
        return new Identifier("default", "biome");
    }

    public static LuaTable getForScript(CustomScript script) {
        return new LuaTable() {{
            set("getBiome", new TwoArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1, LuaValue arg2) {
                    World world = MinecraftClient.getInstance().world;
                    if (world == null) return NIL;

                    BlockPos pos = LuaVector.checkOrNew(arg2).asBlockPos();
                    Biome biome = world.getRegistryManager().get(Registry.BIOME_KEY).get(new Identifier(arg1.checkjstring()));

                    if (biome == null)
                        throw new LuaError("Biome not found");

                    return getTable(world, pos, biome);
                }
            });
        }};
    }

    public static LuaTable getTable(World world, BlockPos pos) {
        return getTable(world, pos, world.getBiome(pos));
    }

    public static LuaTable getTable(World world, BlockPos pos, Biome biome) {
        if (world == null)
            return new LuaTable();

        return new LuaTable() {{
            set("getID", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    Identifier identifier = world.getRegistryManager().get(Registry.BIOME_KEY).getId(biome);
                    return identifier == null ? NIL : LuaValue.valueOf(identifier.toString());
                }
            });

            set("getCategory", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(biome.getCategory().getName());
                }
            });

            set("getTemperature", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(biome.getTemperature());
                }
            });

            set("getPrecipitation", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(biome.getPrecipitation().name());
                }
            });

            set("getSkyColor", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(biome.getSkyColor());
                }
            });

            set("getFoliageColor", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(biome.getFoliageColor());
                }
            });

            set("getFogColor", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(biome.getFogColor());
                }
            });

            set("getWaterColor", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(biome.getWaterColor());
                }
            });

            set("getWaterFogColor", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(biome.getWaterFogColor());
                }
            });

            set("getDownfall", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(biome.getDownfall());
                }
            });

            set("hasHighHumidity", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(biome.hasHighHumidity());
                }
            });

            set("isHot", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(biome.isHot(pos));
                }
            });

            set("isCold", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(biome.isCold(pos));
                }
            });

            set("canSnow", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(!biome.doesNotSnow(pos));
                }
            });
        }};
    }
}
