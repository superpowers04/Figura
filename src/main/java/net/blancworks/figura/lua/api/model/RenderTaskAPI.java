package net.blancworks.figura.lua.api.model;

import net.blancworks.figura.lua.CustomScript;
import net.blancworks.figura.lua.api.ReadOnlyLuaTable;
import net.blancworks.figura.lua.api.block.BlockStateAPI;
import net.blancworks.figura.lua.api.item.ItemStackAPI;
import net.blancworks.figura.lua.api.math.LuaVector;
import net.blancworks.figura.models.CustomModelPart;
import net.blancworks.figura.models.shaders.FiguraRenderLayer;
import net.blancworks.figura.models.tasks.BlockRenderTask;
import net.blancworks.figura.models.tasks.ItemRenderTask;
import net.blancworks.figura.models.tasks.RenderTask;
import net.blancworks.figura.models.tasks.TextRenderTask;
import net.blancworks.figura.utils.TextUtils;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3f;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;

public class RenderTaskAPI {

    public static void addTask(CustomModelPart part, CustomScript script, Varargs args) {
        ItemStack stack = null;
        ModelTransformation.Mode mode = null;

        BlockState state = null;
        Text text = null;

        int index = 0;
        String type = args.arg(++index).checkjstring();
        String name = args.arg(++index).checkjstring();

        if (part.renderTasks.containsKey(name))
            throw new LuaError("Render Task already added!");

        switch (type) {
            case "ITEM" -> {
                stack = ItemStackAPI.checkOrCreateItemStack(args.arg(++index));
                mode = !args.arg(++index).isnil() ? ModelTransformation.Mode.valueOf(args.arg(index).checkjstring()) : ModelTransformation.Mode.FIXED;
            }
            case "BLOCK" -> state = BlockStateAPI.checkOrCreateBlockState(args.arg(++index));
            case "TEXT" -> {
                String textString = TextUtils.noBadges4U(args.arg(++index).checkjstring()).replaceAll("[\n\r]", " ");
                if (textString.length() > 65535)
                    throw new LuaError("Text too long - oopsie!");
                text = TextUtils.tryParseJson(textString);
            }
            default -> throw new LuaError("Invalid task type, expected either \"ITEM\", \"BLOCK\" or \"TEXT\"");
        }

        boolean emissive = !args.arg(++index).isnil() && args.arg(index).checkboolean();
        Vec3f pos = args.arg(++index).isnil() ? null : LuaVector.checkOrNew(args.arg(index)).asV3f();
        Vec3f rot = args.arg(++index).isnil() ? null : LuaVector.checkOrNew(args.arg(index)).asV3f();
        Vec3f scale = args.arg(++index).isnil() ? null : LuaVector.checkOrNew(args.arg(index)).asV3f();

        FiguraRenderLayer customLayer = null;
        if (!args.arg(++index).isnil() && script.avatarData.canRenderCustomLayers()) {
            if (script.customVCP != null) {
                customLayer = script.customVCP.getLayer(args.arg(index).checkjstring());
                if (customLayer == null)
                    throw new LuaError("No custom layer named: " + args.arg(index).checkjstring());
            } else
                throw new LuaError("The player has no custom VCP!");
        }

        RenderTask task;
        switch (type) {
            case "ITEM" -> task = new ItemRenderTask(stack, mode, emissive, pos, rot, scale, customLayer);
            case "BLOCK" -> task = new BlockRenderTask(state, emissive, pos, rot, scale, customLayer);
            case "TEXT" -> task = new TextRenderTask(text, emissive, pos, rot, scale);
            default -> throw new LuaError("Invalid task type, expected either \"ITEM\", \"BLOCK\" or \"TEXT\"");
        }

        part.renderTasks.put(name, new RenderTaskTable(task));
    }

    public static class RenderTaskTable extends ReadOnlyLuaTable {
        public final RenderTask task;

        public RenderTaskTable(RenderTask task) {
            this.task = task;
        }

        public ReadOnlyLuaTable getTable(CustomScript script) {
            return new ReadOnlyLuaTable(new LuaTable() {{
                set("setText", new OneArgFunction() {
                    @Override
                    public LuaValue call(LuaValue arg) {
                        if (task instanceof TextRenderTask text) {
                            String textString = TextUtils.noBadges4U(arg.checkjstring()).replaceAll("[\n\r]", " ");
                            if (textString.length() > 65535)
                                throw new LuaError("Text too long - oopsie!");
                            text.text = TextUtils.tryParseJson(textString);
                        }
                        return NIL;
                    }
                });

                set("setItem", new OneArgFunction() {
                    @Override
                    public LuaValue call(LuaValue arg) {
                        if (task instanceof ItemRenderTask item)
                            item.stack = ItemStackAPI.checkOrCreateItemStack(arg);
                        return NIL;
                    }
                });

                set("setBlock", new OneArgFunction() {
                    @Override
                    public LuaValue call(LuaValue arg) {
                        if (task instanceof BlockRenderTask block)
                            block.state = BlockStateAPI.checkOrCreateBlockState(arg);
                        return NIL;
                    }
                });

                set("setItemMode", new OneArgFunction() {
                    @Override
                    public LuaValue call(LuaValue arg) {
                        if (task instanceof ItemRenderTask item)
                            item.mode = !arg.isnil() ? ModelTransformation.Mode.valueOf(arg.checkjstring()) : ModelTransformation.Mode.FIXED;
                        return NIL;
                    }
                });

                set("setRenderLayer", new OneArgFunction() {
                    @Override
                    public LuaValue call(LuaValue arg) {
                        FiguraRenderLayer customLayer = null;
                        if (!arg.isnil() && script.avatarData.canRenderCustomLayers()) {
                            if (script.customVCP != null) {
                                customLayer = script.customVCP.getLayer(arg.checkjstring());
                                if (customLayer == null)
                                    throw new LuaError("No custom layer named: " + arg.checkjstring());
                            } else
                                throw new LuaError("The player has no custom VCP!");
                        }

                        if (task instanceof ItemRenderTask item)
                            item.customLayer = customLayer;
                        else if (task instanceof BlockRenderTask block)
                            block.customLayer = customLayer;

                        return NIL;
                    }
                });

                set("setEmissive", new OneArgFunction() {
                    @Override
                    public LuaValue call(LuaValue arg) {
                        task.emissive = arg.checkboolean();
                        return NIL;
                    }
                });

                set("setPos", new OneArgFunction() {
                    @Override
                    public LuaValue call(LuaValue arg) {
                        task.pos = arg.isnil() ? new LuaVector().asV3f() : LuaVector.checkOrNew(arg).asV3f();
                        return NIL;
                    }
                });

                set("getPos", new ZeroArgFunction() {
                    @Override
                    public LuaValue call() {
                        return LuaVector.of(task.pos);
                    }
                });

                set("setRot", new OneArgFunction() {
                    @Override
                    public LuaValue call(LuaValue arg) {
                        task.rot = arg.isnil() ? new LuaVector().asV3f() : LuaVector.checkOrNew(arg).asV3f();
                        return NIL;
                    }
                });

                set("getRot", new ZeroArgFunction() {
                    @Override
                    public LuaValue call() {
                        return LuaVector.of(task.rot);
                    }
                });

                set("setScale", new OneArgFunction() {
                    @Override
                    public LuaValue call(LuaValue arg) {
                        task.scale = arg.isnil() ? new LuaVector().asV3f() : LuaVector.checkOrNew(arg).asV3f();
                        return NIL;
                    }
                });

                set("getScale", new ZeroArgFunction() {
                    @Override
                    public LuaValue call() {
                        return LuaVector.of(task.scale);
                    }
                });

            }});
        }
    }
}
