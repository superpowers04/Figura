package net.blancworks.figura.lua.api.world;

import net.blancworks.figura.avatar.AvatarData;
import net.blancworks.figura.avatar.AvatarDataManager;
import net.blancworks.figura.lua.CustomScript;
import net.blancworks.figura.lua.api.ReadOnlyLuaTable;
import net.blancworks.figura.lua.api.block.BlockStateAPI;
import net.blancworks.figura.lua.api.entity.PlayerEntityAPI;
import net.blancworks.figura.lua.api.math.LuaVector;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;

public class WorldAPI {

    private static World getWorld() {
        return MinecraftClient.getInstance().world;
    }

    private static ReadOnlyLuaTable globalLuaTable;


    public static Identifier getID() {
        return new Identifier("default", "world");
    }

    public static ReadOnlyLuaTable getForScript(CustomScript script) {
        if (globalLuaTable == null)
            updateGlobalTable();
        return globalLuaTable;
    }

    public static void updateGlobalTable(){
        globalLuaTable = new ReadOnlyLuaTable(new LuaTable() {{
            set("getBlockState", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    LuaVector vec = LuaVector.checkOrNew(arg);
                    BlockPos pos = new BlockPos(vec.asV3iFloored());

                    World w = getWorld();
                    if (w.getChunk(pos) == null) return NIL;

                    BlockState state = w.getBlockState(pos);
                    return BlockStateAPI.getTable(state, w, pos);
                }
            });

            set("getRedstonePower", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    LuaVector vec = LuaVector.checkOrNew(arg);
                    BlockPos pos = new BlockPos(vec.asV3iFloored());

                    World w = getWorld();

                    if (w.getChunk(pos) == null) return NIL;

                    return LuaNumber.valueOf(w.getReceivedRedstonePower(pos));
                }
            });

            set("getStrongRedstonePower", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    LuaVector vec = LuaVector.checkOrNew(arg);
                    BlockPos pos = new BlockPos(vec.asV3iFloored());

                    World w = getWorld();

                    if (w.getChunk(pos) == null) return NIL;

                    return LuaNumber.valueOf(w.getReceivedStrongRedstonePower(pos));
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

            //Looks like this just... Gets the time??? Confused on this one.
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

            set("isLightning", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue a) {
                    return LuaBoolean.valueOf(getWorld().isThundering());
                }
            });

            set("getLightLevel", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue a) {
                    LuaVector vec = LuaVector.checkOrNew(a);
                    BlockPos pos = new BlockPos(vec.asV3iFloored());

                    if (getWorld().getChunk(pos) == null) return NIL;

                    getWorld().calculateAmbientDarkness();
                    int dark = getWorld().getAmbientDarkness();
                    int realLight = getWorld().getLightingProvider().getLight(pos, dark);

                    return LuaInteger.valueOf(realLight);
                }
            });

            set("getSkyLightLevel", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue a) {
                    LuaVector vec = LuaVector.checkOrNew(a);
                    BlockPos pos = new BlockPos(vec.asV3iFloored());

                    if (getWorld().getChunk(pos) == null) return NIL;

                    return LuaInteger.valueOf(getWorld().getLightLevel(LightType.SKY, pos));
                }
            });

            set("getBlockLightLevel", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue a) {
                    LuaVector vec = LuaVector.checkOrNew(a);
                    BlockPos pos = new BlockPos(vec.asV3iFloored());

                    if (getWorld().getChunk(pos) == null) return NIL;

                    return LuaInteger.valueOf(getWorld().getLightLevel(LightType.BLOCK, pos));
                }
            });

            set("getBiomeID", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue a) {

                    LuaVector vec = LuaVector.checkOrNew(a);
                    BlockPos pos = new BlockPos(vec.asV3iFloored());

                    if (getWorld().getChunk(pos) == null) return NIL;

                    Biome b = getWorld().getBiome(pos);

                    if (b == null)
                        return NIL;

                    Identifier id = getWorld().getRegistryManager().get(Registry.BIOME_KEY).getId(b);

                    if (id == null)
                        return NIL;

                    return LuaString.valueOf(id.toString());
                }
            });

            set("isOpenSky", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue a) {
                    LuaVector vec = LuaVector.checkOrNew(a);
                    BlockPos pos = new BlockPos(vec.asV3iFloored());

                    if (getWorld().getChunk(pos) == null) return NIL;

                    return LuaBoolean.valueOf(getWorld().isSkyVisible(pos));
                }
            });

            set("getBiomeTemperature", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue a) {
                    LuaVector vec = LuaVector.checkOrNew(a);
                    BlockPos pos = new BlockPos(vec.asV3iFloored());

                    if (getWorld().getChunk(pos) == null) return NIL;

                    Biome b = getWorld().getBiome(pos);

                    if (b == null) return NIL;

                    return LuaNumber.valueOf(b.getTemperature(pos));
                }
            });

            set("getBiomePrecipitation", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue a) {
                    LuaVector vec = LuaVector.checkOrNew(a);
                    BlockPos pos = new BlockPos(vec.asV3iFloored());

                    if (getWorld().getChunk(pos) == null) return NIL;

                    Biome b = getWorld().getBiome(pos);

                    if (b == null) return NIL;

                    return LuaString.valueOf(b.getPrecipitation().name());
                }
            });

            set("getFiguraPlayers", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    LuaTable playerList = new LuaTable();

                    for (PlayerEntity entity : getWorld().getPlayers()) {
                        AvatarData data = AvatarDataManager.getDataForPlayer(entity.getUuid());
                        if (data == null || data.script == null || !data.script.allowPlayerTargeting)
                            continue;

                        playerList.insert(0, new PlayerEntityAPI.PlayerEntityLuaAPITable(() -> entity).getTable());
                    }

                    return playerList;
                }
            });

            set("hasWorld", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return getWorld() == null ? FALSE : TRUE;
                }
            });

        }});
    }
}
