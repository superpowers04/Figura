package net.blancworks.figura.lua.api.world;

import net.blancworks.figura.lua.api.math.LuaVector;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.BuiltinRegistries;
import net.minecraft.world.biome.Biome;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;

public class BiomeAPI {

    public static LuaTable getTable(Biome biome) {
        return new LuaTable() {{
            set("getID", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    Identifier identifier = BuiltinRegistries.BIOME.getId(biome);
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

            set("isHot", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1) {
                    return LuaValue.valueOf(biome.isHot(LuaVector.checkOrNew(arg1).asBlockPos()));
                }
            });

            set("isCold", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1) {
                    return LuaValue.valueOf(biome.isCold(LuaVector.checkOrNew(arg1).asBlockPos()));
                }
            });

            set("canSnow", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1) {
                    return LuaValue.valueOf(!biome.doesNotSnow(LuaVector.checkOrNew(arg1).asBlockPos()));
                }
            });
        }};
    }
}
