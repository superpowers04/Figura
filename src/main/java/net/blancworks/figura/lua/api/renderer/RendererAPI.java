package net.blancworks.figura.lua.api.renderer;

import com.mojang.brigadier.StringReader;
import net.blancworks.figura.PlayerDataManager;
import net.blancworks.figura.lua.CustomScript;
import net.blancworks.figura.lua.api.ReadOnlyLuaTable;
import net.blancworks.figura.lua.api.block.BlockStateAPI;
import net.blancworks.figura.lua.api.item.ItemStackAPI;
import net.blancworks.figura.lua.api.math.LuaVector;
import net.blancworks.figura.lua.api.model.CustomModelAPI;
import net.blancworks.figura.lua.api.entity.EntityAPI;
import net.blancworks.figura.models.CustomModelPart;
import net.blancworks.figura.models.shaders.FiguraRenderLayer;
import net.blancworks.figura.models.tasks.BlockRenderTask;
import net.blancworks.figura.models.tasks.ItemRenderTask;
import net.blancworks.figura.models.tasks.TextRenderTask;
import net.blancworks.figura.utils.TextUtils;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MarkerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.ItemStack;
import net.minecraft.text.LiteralText;
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
                    if (PlayerDataManager.localPlayer != script.playerData)
                        return LuaBoolean.FALSE;

                    return MinecraftClient.getInstance().options.getPerspective().isFirstPerson() ? LuaBoolean.TRUE : LuaBoolean.FALSE;
                }
            });

            set("isCameraBackwards", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    if (PlayerDataManager.localPlayer != script.playerData)
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

            set("renderItem", new VarArgFunction() {
                @Override
                public Varargs onInvoke(Varargs args) {

                    ItemStack stack = ItemStackAPI.checkOrCreateItemStack(args.arg(1));
                    CustomModelPart parent = CustomModelAPI.checkCustomModelPart(args.arg(2));
                    ModelTransformation.Mode mode = !args.arg(3).isnil() ? ModelTransformation.Mode.valueOf(args.arg(3).checkjstring()) : ModelTransformation.Mode.FIXED;
                    boolean emissive = !args.arg(4).isnil() && args.arg(4).checkboolean();
                    Vec3f pos = args.arg(5).isnil() ? null : LuaVector.checkOrNew(args.arg(5)).asV3f();
                    Vec3f rot = args.arg(6).isnil() ? null : LuaVector.checkOrNew(args.arg(6)).asV3f();
                    Vec3f scale = args.arg(7).isnil() ? null : LuaVector.checkOrNew(args.arg(7)).asV3f();

                    FiguraRenderLayer customLayer = null;
                    if (!args.arg(8).isnil() && script.playerData.canRenderCustomLayers()) {
                        if (script.customVCP != null) {
                            customLayer = script.customVCP.getLayer(args.arg(8).checkjstring());
                            if (customLayer == null)
                                throw new LuaError("No custom layer named: " + args.arg(8).checkjstring());
                        } else
                            throw new LuaError("The player has no custom VCP!");
                    }

                    parent.renderTasks.add(new ItemRenderTask(stack, mode, emissive, pos, rot, scale, customLayer));
                    return NIL;
                }
            });

            set("renderBlock", new VarArgFunction() {
                @Override
                public Varargs onInvoke(Varargs args) {
                    if (script.renderMode == CustomScript.RenderType.WORLD_RENDER)
                        throw new LuaError("Cannot render block on world render!");

                    BlockState state = BlockStateAPI.checkOrCreateBlockState(args.arg(1));
                    CustomModelPart parent = CustomModelAPI.checkCustomModelPart(args.arg(2));
                    boolean emissive = !args.arg(3).isnil() && args.arg(3).checkboolean();
                    Vec3f pos = args.arg(4).isnil() ? null : LuaVector.checkOrNew(args.arg(4)).asV3f();
                    Vec3f rot = args.arg(5).isnil() ? null : LuaVector.checkOrNew(args.arg(5)).asV3f();
                    Vec3f scale = args.arg(6).isnil() ? null : LuaVector.checkOrNew(args.arg(6)).asV3f();
                    FiguraRenderLayer customLayer = null;
                    if (!args.arg(7).isnil()) {
                        if (script.customVCP != null) {
                            customLayer = script.customVCP.getLayer(args.arg(7).checkjstring());
                            if (customLayer == null)
                                throw new LuaError("No custom layer named: " + args.arg(7).checkjstring());
                        } else
                            throw new LuaError("The player has no custom VCP!");
                    }
                    parent.renderTasks.add(new BlockRenderTask(state, emissive, pos, rot, scale, customLayer));
                    return NIL;
                }
            });

            set("renderText", new VarArgFunction() {
                @Override
                public Varargs onInvoke(Varargs args) {
                    if (script.renderMode == CustomScript.RenderType.WORLD_RENDER)
                        throw new LuaError("Cannot render text on world render!");

                    String arg1 = TextUtils.noBadges4U(args.arg(1).checkjstring()).replaceAll("[\n\r]", " ");

                    if (arg1.length() > 65535)
                        throw new LuaError("Text too long - oopsie!");

                    Text text;
                    try {
                        text = Text.Serializer.fromJson(new StringReader(arg1));

                        if (text == null)
                            throw new Exception("Error parsing JSON string");
                    } catch (Exception ignored) {
                        text = new LiteralText(arg1);
                    }

                    CustomModelPart parent = CustomModelAPI.checkCustomModelPart(args.arg(2));
                    boolean emissive = !args.arg(3).isnil() && args.arg(3).checkboolean();
                    Vec3f pos = args.arg(4).isnil() ? null : LuaVector.checkOrNew(args.arg(4)).asV3f();
                    Vec3f rot = args.arg(5).isnil() ? null : LuaVector.checkOrNew(args.arg(5)).asV3f();
                    Vec3f scale = args.arg(6).isnil() ? null : LuaVector.checkOrNew(args.arg(6)).asV3f();

                    parent.renderTasks.add(new TextRenderTask(text, emissive, pos, rot, scale));

                    return NIL;
                }
            });

            set("getTextWidth", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    String arg1 = TextUtils.noBadges4U(arg.checkjstring()).replaceAll("[\n\r]", " ");
                    Text text;
                    try {
                        text = Text.Serializer.fromJson(new StringReader(arg1));

                        if (text == null)
                            throw new Exception("Error parsing JSON string");
                    } catch (Exception ignored) {
                        text = new LiteralText(arg1);
                    }
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
