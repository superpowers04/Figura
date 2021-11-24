package net.blancworks.figura.lua.api.world;

import net.blancworks.figura.FiguraMod;
import net.blancworks.figura.PlayerData;
import net.blancworks.figura.PlayerDataManager;
import net.blancworks.figura.lua.CustomScript;
import net.blancworks.figura.lua.FiguraLuaManager;
import net.blancworks.figura.lua.api.ReadOnlyLuaTable;
import net.blancworks.figura.lua.api.block.BlockStateAPI;
import net.blancworks.figura.lua.api.math.LuaVector;
import net.blancworks.figura.lua.api.world.entity.EntityAPI;
import net.blancworks.figura.lua.api.world.entity.LivingEntityAPI;
import net.blancworks.figura.lua.api.world.entity.PlayerEntityAPI;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MarkerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.LightType;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.ThreeArgFunction;
import org.luaj.vm2.lib.VarArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;

import java.util.function.BiPredicate;
import java.util.function.Predicate;

public class WorldAPI {

    private static World getWorld(){
        return MinecraftClient.getInstance().world;
    }

    private static ReadOnlyLuaTable globalLuaTable;


    public static Identifier getID() {
        return new Identifier("default", "world");
    }

    public static ReadOnlyLuaTable getForScript(CustomScript script) {
        if(globalLuaTable == null)
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
                        PlayerData data = PlayerDataManager.getDataForPlayer(entity.getUuid());
                        if (data == null || data.script == null || !data.script.allowPlayerTargeting)
                            continue;

                        playerList.insert(0, new PlayerEntityAPI.PlayerEntityLuaAPITable(() -> entity).getTable());
                    }

                    return playerList;
                }
            });

            set("raycastBlocks", new VarArgFunction() {
                @Override
                public LuaValue invoke(Varargs args) {
                    Vec3d start = LuaVector.checkOrNew(args.arg(1)).asV3d();
                    Vec3d end = LuaVector.checkOrNew(args.arg(2)).asV3d();
                    int instructionPenalty = (int) end.subtract(start).length() * 2;
                    applyInstructionPenalty(instructionPenalty);
                    String shapeType;
                    String fluidHandling;
                    LuaFunction func;
                    if (args.arg(3).isnil())
                        shapeType = "COLLIDER";
                    else
                        shapeType = args.arg(3).checkjstring();
                    if (args.arg(4).isnil())
                        fluidHandling = "NONE";
                    else
                        fluidHandling = args.arg(4).checkjstring();
                    if (args.arg(5).isnil())
                        func = returnTrue;
                    else
                        func = args.arg(5).checkfunction();

                    FiguraRaycastContext context = FiguraRaycastContext.of(start, end, shapeType, fluidHandling, func);
                    BlockHitResult result = raycastBlocks(getWorld(), context);

                    if (result.getType() == HitResult.Type.MISS)
                        return NIL;

                    LuaTable ret = new LuaTable();
                    ret.set("state", BlockStateAPI.getTable(getWorld().getBlockState(result.getBlockPos()),getWorld(),result.getBlockPos()));
                    ret.set("pos", LuaVector.of(result.getPos()));
                    return ret;
                }
            });

            set("raycastEntities", new ThreeArgFunction() {
                @Override
                public LuaValue call(LuaValue startPos, LuaValue endPos, LuaValue func) {
                    Entity e = new MarkerEntity(EntityType.MARKER, getWorld());
                    Vec3d start = LuaVector.checkOrNew(startPos).asV3d();
                    Vec3d end = LuaVector.checkOrNew(endPos).asV3d();
                    int instructionPenalty = (int) end.subtract(start).length() * 2;
                    applyInstructionPenalty(instructionPenalty);
                    Predicate<Entity> pred;
                    if (func.isnil())
                        pred = entity -> true;
                    else
                        pred = (Entity entity) -> {
                            LuaTable entityTable = EntityAPI.getTableForEntity(entity);
                            return func.checkfunction().call(entityTable).toboolean();
                        };
                    EntityHitResult result = ProjectileUtil.raycast(e, start, end, new Box(start, end), pred, Double.MAX_VALUE);
                    if (result == null) return NIL;
                    return EntityAPI.getTableForEntity(result.getEntity());
                }
            });
        }});
    }

    private static final LuaFunction returnTrue = new ZeroArgFunction() {
        @Override
        public LuaValue call() {
            return TRUE;
        }
    };

    //Copied most of this function from BlockView.java, just had to change the lambda slightly
    public static BlockHitResult raycastBlocks(BlockView view, FiguraRaycastContext context) {
        return BlockView.raycast(context.getStart(), context.getEnd(), context, (contextx, pos) -> {
            BlockState blockState = view.getBlockState(pos);
            FluidState fluidState = view.getFluidState(pos);
            Vec3d vec3d = contextx.getStart();
            Vec3d vec3d2 = contextx.getEnd();
            //The one line I added, allowing people to have better tests for the raycast.
            if (!contextx.predicate.test(blockState, pos)) return null;
            VoxelShape voxelShape = contextx.getBlockShape(blockState, view, pos);
            BlockHitResult blockHitResult = view.raycastBlock(vec3d, vec3d2, pos, voxelShape, blockState);
            VoxelShape voxelShape2 = contextx.getFluidShape(fluidState, view, pos);
            BlockHitResult blockHitResult2 = voxelShape2.raycast(vec3d, vec3d2, pos);
            double d = blockHitResult == null ? Double.MAX_VALUE : contextx.getStart().squaredDistanceTo(blockHitResult.getPos());
            double e = blockHitResult2 == null ? Double.MAX_VALUE : contextx.getStart().squaredDistanceTo(blockHitResult2.getPos());
            return d <= e ? blockHitResult : blockHitResult2;
        }, (contextx) -> {
            Vec3d vec3d = contextx.getStart().subtract(contextx.getEnd());
            return BlockHitResult.createMissed(contextx.getEnd(), Direction.getFacing(vec3d.x, vec3d.y, vec3d.z), new BlockPos(contextx.getEnd()));
        });
    }

    public static void applyInstructionPenalty(int penalty) {
        LuaThread.State state = FiguraMod.currentData.script.scriptGlobals.running.state;
        if (state.bytecodes + penalty >= state.hookcount)
            state.bytecodes = state.hookcount - 1;
        else
            state.bytecodes += penalty;
    }

    public static class FiguraRaycastContext extends RaycastContext {

        private BiPredicate<BlockState, BlockPos> predicate;

        public static FiguraRaycastContext of(Vec3d start, Vec3d end, String shapeType, String fluidHandling, LuaFunction predicate) {
            ShapeType shapes;
            FluidHandling fluids;
            try {
                shapes = ShapeType.valueOf(shapeType);
            } catch (IllegalArgumentException e) {
                throw new LuaError("Invalid shapeType: " + shapeType);
            }
            try {
                fluids = FluidHandling.valueOf(fluidHandling);
            } catch (IllegalArgumentException e) {
                throw new LuaError("Invalid shapeType: " + shapeType);
            }

            //Need some random entity for some reason, it doesn't affect anything in the actual method calls
            FiguraRaycastContext result = new FiguraRaycastContext(start, end, shapes, fluids, new MarkerEntity(EntityType.MARKER, getWorld()));
            result.predicate = (state, pos) -> predicate.call(BlockStateAPI.getTable(state,getWorld(),pos)).toboolean();
            return result;
        }

        public FiguraRaycastContext(Vec3d start, Vec3d end, ShapeType shapeType, FluidHandling fluidHandling, Entity entity) {
            super(start, end, shapeType, fluidHandling, entity);
        }
    }

}
