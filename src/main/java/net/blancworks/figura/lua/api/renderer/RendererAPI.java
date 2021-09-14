package net.blancworks.figura.lua.api.renderer;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.blancworks.figura.PlayerDataManager;
import net.blancworks.figura.lua.CustomScript;
import net.blancworks.figura.lua.api.ReadOnlyLuaTable;
import net.blancworks.figura.lua.api.item.ItemStackAPI;
import net.blancworks.figura.lua.api.math.LuaVector;
import net.blancworks.figura.lua.api.model.CustomModelAPI;
import net.blancworks.figura.lua.api.nameplate.NamePlateAPI;
import net.blancworks.figura.lua.api.renderer.RenderTask.*;
import net.blancworks.figura.models.CustomModelPart;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.command.argument.BlockStateArgumentType;
import net.minecraft.item.ItemStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
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
                    CustomModelPart parent = CustomModelAPI.checkCustomModelPart(args.arg(2));
                    ModelTransformation.Mode mode = !args.arg(3).isnil() ? ModelTransformation.Mode.valueOf(args.arg(3).checkjstring()) : ModelTransformation.Mode.FIXED;
                    boolean emissive = !args.arg(4).isnil() && args.arg(4).checkboolean();
                    Vec3f pos = args.arg(5).isnil() ? null : LuaVector.checkOrNew(args.arg(5)).asV3f();
                    Vec3f rot = args.arg(6).isnil() ? null : LuaVector.checkOrNew(args.arg(6)).asV3f();
                    Vec3f scale = args.arg(7).isnil() ? null : LuaVector.checkOrNew(args.arg(7)).asV3f();

                    parent.renderTasks.add(new ItemRenderTask(stack, mode, emissive, pos, rot, scale));

                    return NIL;
                }
            });

            set("renderBlock", new VarArgFunction() {
                @Override
                public Varargs onInvoke(Varargs args) {

                    try {
                        BlockState state = BlockStateArgumentType.blockState().parse(new StringReader(args.arg(1).checkjstring())).getBlockState();
                        CustomModelPart parent = CustomModelAPI.checkCustomModelPart(args.arg(2));
                        boolean emissive = !args.arg(3).isnil() && args.arg(3).checkboolean();
                        Vec3f pos = args.arg(4).isnil() ? null : LuaVector.checkOrNew(args.arg(4)).asV3f();
                        Vec3f rot = args.arg(5).isnil() ? null : LuaVector.checkOrNew(args.arg(5)).asV3f();
                        Vec3f scale = args.arg(6).isnil() ? null : LuaVector.checkOrNew(args.arg(6)).asV3f();

                        parent.renderTasks.add(new BlockRenderTask(state, emissive, pos, rot, scale));
                    } catch (CommandSyntaxException e) {
                        throw new LuaError("Incorrectly formatted BlockState string!");
                    }

                    return NIL;
                }
            });

            set("renderText", new VarArgFunction() {
                @Override
                public Varargs onInvoke(Varargs args) {
                    String arg1 = NamePlateAPI.noBadges4U(args.arg(1).checkjstring()).replaceAll("[\n\r]", " ");

                    if (arg1.length() > 65535)
                        throw new LuaError("Text too long - oopsie!");

                    Text text;
                    try {
                        text = Text.Serializer.fromJson(new StringReader(arg1));
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

        }});
    }
}
