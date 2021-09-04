package net.blancworks.figura.lua.api;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.blancworks.figura.PlayerDataManager;
import net.blancworks.figura.lua.CustomScript;
import net.blancworks.figura.lua.api.item.ItemStackAPI;
import net.blancworks.figura.lua.api.math.LuaVector;
import net.blancworks.figura.lua.api.model.CustomModelAPI;
import net.blancworks.figura.models.CustomModelPart;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.command.argument.BlockStateArgumentType;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3f;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.VarArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;

public class RendererAPI {

    public static Identifier getID() {
        return new Identifier("default", "renderer");
    }

    public static ReadOnlyLuaTable getForScript(CustomScript script) {
        return new ReadOnlyLuaTable(new LuaTable(){{
            set("setShadowSize", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    script.customShadowSize = arg.isnil() ? null : arg.checknumber().tofloat();
                    return NIL;
                }
            });

            set("getShadowSize", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
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

            set("renderItem", new VarArgFunction() {
                @Override
                public Varargs onInvoke(Varargs args) {

                    ItemStack stack = ItemStackAPI.checkItemStack(args.arg(1));
                    ModelTransformation.Mode mode = !args.arg(2).isnil() ? ModelTransformation.Mode.valueOf(args.arg(2).checkjstring()) : ModelTransformation.Mode.FIXED;
                    CustomModelPart parent = args.arg(3).isnil() ? null : CustomModelAPI.checkCustomModelPart(args.arg(3));
                    Vec3f pos = LuaVector.checkOrNew(args.arg(4)).asV3f();
                    Vec3f rot = LuaVector.checkOrNew(args.arg(5)).asV3f();
                    Vec3f scale  = LuaVector.checkOrNew(args.arg(6)).asV3f();
                    Vec3f origin = LuaVector.checkOrNew(args.arg(7)).asV3f();


                    if (parent != null) {
                        parent.renderTasks.add(new ItemRenderTask(stack, mode, pos, rot, scale, origin));
                    } else {
                        script.renderTasks.add(new ItemRenderTask(stack, mode, pos, rot, scale, origin));
                    }

                    return NIL;
                }
            });

            set("renderBlock", new VarArgFunction() {
                @Override
                public Varargs onInvoke(Varargs args) {

                    try {
                        BlockState state = BlockStateArgumentType.blockState().parse(new StringReader(args.arg(1).checkjstring())).getBlockState();
                        CustomModelPart parent = args.arg(2).isnil() ? null : CustomModelAPI.checkCustomModelPart(args.arg(2));
                        Vec3f pos = LuaVector.checkOrNew(args.arg(3)).asV3f();
                        Vec3f rot = LuaVector.checkOrNew(args.arg(4)).asV3f();
                        Vec3f scale  = LuaVector.checkOrNew(args.arg(5)).asV3f();
                        Vec3f origin = LuaVector.checkOrNew(args.arg(6)).asV3f();

                        if (parent != null) {
                            parent.renderTasks.add(new BlockRenderTask(state, pos, rot, scale, origin));
                        } else {
                            script.renderTasks.add(new BlockRenderTask(state, pos, rot, scale, origin));
                        }
                    } catch (CommandSyntaxException e) {
                        throw new LuaError("Incorrectly formatted BlockState string!");
                    }

                    return NIL;
                }
            });
        }});
    }

    public static abstract class RenderTask {
        public final Vec3f pos;
        public final Vec3f rot;
        public final Vec3f scale;
        public final Vec3f origin;

        protected RenderTask(Vec3f pos, Vec3f rot, Vec3f scale, Vec3f origin) {
            this.pos = pos;
            this.rot = rot;
            this.scale = scale;
            this.origin = origin;
        }
        public abstract int render(MatrixStack matrices, MatrixStack transformStack, VertexConsumerProvider vcp, int light, int overlay, float red, float green, float blue, float alpha, MinecraftClient client);
        public void transform(MatrixStack matrices) {
            matrices.translate(origin.getX(),origin.getY(),origin.getZ());
            matrices.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(rot.getX()+180));
            matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(rot.getY()));
            matrices.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(rot.getZ()));
            matrices.translate(pos.getX(),pos.getY(),pos.getZ());
            matrices.scale(scale.getX(),scale.getY(),scale.getZ());
        }
    }
    public static class ItemRenderTask extends RenderTask {
        public final ItemStack stack;
        public final ModelTransformation.Mode mode;

        public ItemRenderTask(ItemStack stack, ModelTransformation.Mode mode, Vec3f pos, Vec3f rot, Vec3f scale, Vec3f origin) {
            super(pos, rot, scale, origin);
            this.stack = stack;
            this.mode = mode;
        }

        @Override
        public int render(MatrixStack matrices, MatrixStack transformStack, VertexConsumerProvider vcp, int light, int overlay, float red, float green, float blue, float alpha, MinecraftClient client) {
            this.transform(matrices);

            client.getItemRenderer().renderItem(stack, mode, light, overlay, matrices, vcp, 0);
            return 4 * client.getItemRenderer().getHeldItemModel(stack, null, null, 0).getQuads(null, null, client.world.random).size();
        }
    }
    public static class BlockRenderTask extends RenderTask {
        public final BlockState state;

        public BlockRenderTask(BlockState state, Vec3f pos, Vec3f rot, Vec3f scale, Vec3f origin) {
            super(pos, rot, scale, origin);
            this.state = state;
        }

        @Override
        public int render(MatrixStack matrices, MatrixStack transformStack, VertexConsumerProvider vcp, int light, int overlay, float red, float green, float blue, float alpha, MinecraftClient client) {
            this.transform(matrices);
            client.getBlockRenderManager().renderBlock(state, new BlockPos(pos.getX(),pos.getY(),pos.getZ()), client.world, matrices, vcp.getBuffer(RenderLayer.getCutout()), false, client.world.random);

            return 4 * client.getBlockRenderManager().getModel(state).getQuads(state, null, client.world.random).size();
        }
    }

}
