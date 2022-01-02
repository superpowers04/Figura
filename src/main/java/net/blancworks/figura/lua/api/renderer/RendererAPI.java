package net.blancworks.figura.lua.api.renderer;

import net.blancworks.figura.avatar.AvatarDataManager;
import net.blancworks.figura.lua.CustomScript;
import net.blancworks.figura.lua.api.ReadOnlyLuaTable;
import net.blancworks.figura.lua.api.block.BlockStateAPI;
import net.blancworks.figura.lua.api.entity.EntityAPI;
import net.blancworks.figura.lua.api.math.LuaVector;
import net.blancworks.figura.utils.TextUtils;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MarkerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.fluid.FluidState;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.ThreeArgFunction;
import org.luaj.vm2.lib.VarArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;

import java.util.function.BiPredicate;
import java.util.function.Predicate;

public class RendererAPI {

    public static Identifier getID() {
        return new Identifier("default", "renderer");
    }

    public static ReadOnlyLuaTable getForScript(CustomScript script) {
        return new ReadOnlyLuaTable(new LuaTable(){{
            set("setShadowSize", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    script.customShadowSize = arg.isnil() ? null : MathHelper.clamp(arg.checknumber().tofloat(), 0f, 24f);
                    return NIL;
                }
            });

            set("getShadowSize", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return script.customShadowSize == null ? NIL : LuaNumber.valueOf(script.customShadowSize);
                }
            });

            set("isFirstPerson", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    if (AvatarDataManager.localPlayer != script.avatarData)
                        return LuaBoolean.FALSE;

                    return MinecraftClient.getInstance().options.getPerspective().isFirstPerson() ? LuaBoolean.TRUE : LuaBoolean.FALSE;
                }
            });

            set("isCameraBackwards", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    if (AvatarDataManager.localPlayer != script.avatarData)
                        return LuaBoolean.FALSE;

                    return MinecraftClient.getInstance().options.getPerspective().isFrontView() ? LuaBoolean.TRUE : LuaBoolean.FALSE;
                }
            });

            set("getCameraPos", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    //Yes, this IS intended to also be called for non-local players.
                    //This might be exploitable? idk
                    return LuaVector.of(MinecraftClient.getInstance().gameRenderer.getCamera().getPos());
                }
            });

            set("getCameraRot", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaVector.of(MinecraftClient.getInstance().gameRenderer.getCamera().getRotation().toEulerXyzDegrees());
                }
            });

            set("setRenderFire", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    script.shouldRenderFire = arg.isnil() ? null : arg.checkboolean();
                    return NIL;
                }
            });

            set("getRenderFire", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return script.shouldRenderFire == null ? NIL : LuaBoolean.valueOf(script.shouldRenderFire);
                }
            });

            set("getTextWidth", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    String arg1 = TextUtils.noBadges4U(arg.checkjstring()).replaceAll("[\n\r]", " ");
                    Text text = TextUtils.tryParseJson(arg1);
                    return LuaNumber.valueOf(MinecraftClient.getInstance().textRenderer.getWidth(text));
                }
            });

            set("setMountEnabled", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    script.renderMount = arg.checkboolean();
                    return NIL;
                }
            });

            set("isMountEnabled", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaBoolean.valueOf(script.renderMount);
                }
            });

            set("setMountShadowEnabled", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    script.renderMountShadow = arg.checkboolean();
                    return NIL;
                }
            });

            set("isMountShadowEnabled", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaBoolean.valueOf(script.renderMountShadow);
                }
            });

            set("raycastBlocks", new VarArgFunction() {
                @Override
                public LuaValue invoke(Varargs args) {
                    Vec3d start = LuaVector.checkOrNew(args.arg(1)).asV3d();
                    Vec3d end = LuaVector.checkOrNew(args.arg(2)).asV3d();
                    int instructionPenalty = (int) end.subtract(start).length() * 2;
                    applyInstructionPenalty(script, instructionPenalty);
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
                    ret.set("state", BlockStateAPI.getTable(getWorld().getBlockState(result.getBlockPos()), getWorld(), result.getBlockPos()));
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
                    applyInstructionPenalty(script, instructionPenalty);
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
                    LuaTable ret = new LuaTable();
                    ret.set("entity", EntityAPI.getTableForEntity(result.getEntity()));
                    ret.set("pos", LuaVector.of(result.getPos()));
                    return ret;
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

    private static World getWorld(){
        return MinecraftClient.getInstance().world;
    }

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

    public static void applyInstructionPenalty(CustomScript script, int penalty) {
        LuaThread.State state = script.scriptGlobals.running.state;
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
