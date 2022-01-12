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

import java.util.List;

public class RenderTaskAPI {

    public static RenderTaskTable addTask(CustomModelPart part, CustomScript script, Varargs args) {
        ItemStack stack = null;
        ModelTransformation.Mode mode = null;

        BlockState state = null;
        List<Text> text = null;

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
                text = TextUtils.splitText(TextUtils.tryParseJson(textString), "\n");
            }
            default -> throw new LuaError("Invalid task type, expected either \"ITEM\", \"BLOCK\" or \"TEXT\"");
        }

        boolean emissive = !args.arg(++index).isnil() && args.arg(index).checkboolean();
        Vec3f pos = args.arg(++index).isnil() ? null : LuaVector.checkOrNew(args.arg(index)).asV3f();
        Vec3f rot = args.arg(++index).isnil() ? null : LuaVector.checkOrNew(args.arg(index)).asV3f();
        Vec3f scale = args.arg(++index).isnil() ? null : LuaVector.checkOrNew(args.arg(index)).asV3f();
        FiguraRenderLayer customLayer = script.getCustomLayer(args.arg(++index));

        RenderTask task;
        switch (type) {
            case "ITEM" -> task = new ItemRenderTask(stack, mode, emissive, pos, rot, scale, customLayer);
            case "BLOCK" -> task = new BlockRenderTask(state, emissive, pos, rot, scale, customLayer);
            case "TEXT" -> task = new TextRenderTask(text, emissive, pos, rot, scale);
            default -> throw new LuaError("Invalid task type, expected either \"ITEM\", \"BLOCK\" or \"TEXT\"");
        }

        RenderTaskTable taskTable = new RenderTaskTable(task);
        synchronized (part.renderTasks) {
            part.renderTasks.put(name, taskTable);
        }

        return taskTable;
    }

    public static class RenderTaskTable extends ReadOnlyLuaTable {
        public final RenderTask task;

        public RenderTaskTable(RenderTask task) {
            this.task = task;
        }

        public ReadOnlyLuaTable getTable(CustomScript script) {
            LuaTable tbl;

            if (task instanceof TextRenderTask text)
                tbl = getTextTable(text);
            else if (task instanceof BlockRenderTask block)
                tbl = getBlockTable(block, script);
            else if (task instanceof ItemRenderTask item)
                tbl = getItemTable(item, script);
            else
                tbl = new LuaTable();

            tbl.set("setEmissive", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    task.emissive = arg.checkboolean();
                    return NIL;
                }
            });

            tbl.set("setPos", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    task.pos = arg.isnil() ? new LuaVector().asV3f() : LuaVector.checkOrNew(arg).asV3f();
                    return NIL;
                }
            });

            tbl.set("getPos", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaVector.of(task.pos);
                }
            });

            tbl.set("setRot", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    task.rot = arg.isnil() ? new LuaVector().asV3f() : LuaVector.checkOrNew(arg).asV3f();
                    return NIL;
                }
            });

            tbl.set("getRot", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaVector.of(task.rot);
                }
            });

            tbl.set("setScale", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    task.scale = arg.isnil() ? new LuaVector().asV3f() : LuaVector.checkOrNew(arg).asV3f();
                    return NIL;
                }
            });

            tbl.set("getScale", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaVector.of(task.scale);
                }
            });

            return new ReadOnlyLuaTable(tbl);
        }

        private LuaTable getTextTable(TextRenderTask text) {
            return new LuaTable() {{
                set("setText", new OneArgFunction() {
                    @Override
                    public LuaValue call(LuaValue arg) {
                        String textString = TextUtils.noBadges4U(arg.checkjstring()).replaceAll("[\n\r]", " ");
                        if (textString.length() > 65535)
                            throw new LuaError("Text too long - oopsie!");
                        text.text = TextUtils.splitText(TextUtils.tryParseJson(textString), "\n");
                        return NIL;
                    }
                });

                set("setLineSpacing", new OneArgFunction() {
                    @Override
                    public LuaValue call(LuaValue arg) {
                        text.lineSpacing = arg.toint();
                        return NIL;
                    }
                });
            }};
        }

        private LuaTable getBlockTable(BlockRenderTask block, CustomScript script) {
            return new LuaTable() {{
                set("setBlock", new OneArgFunction() {
                    @Override
                    public LuaValue call(LuaValue arg) {
                        block.state = BlockStateAPI.checkOrCreateBlockState(arg);
                        return NIL;
                    }
                });

                set("setRenderLayer", new OneArgFunction() {
                    @Override
                    public LuaValue call(LuaValue arg) {
                        block.customLayer = script.getCustomLayer(arg);
                        return NIL;
                    }
                });
            }};
        }

        private LuaTable getItemTable(ItemRenderTask item, CustomScript script) {
            return new LuaTable() {{
                set("setItemMode", new OneArgFunction() {
                    @Override
                    public LuaValue call(LuaValue arg) {
                        item.mode = !arg.isnil() ? ModelTransformation.Mode.valueOf(arg.checkjstring()) : ModelTransformation.Mode.FIXED;
                        return NIL;
                    }
                });

                set("setItem", new OneArgFunction() {
                    @Override
                    public LuaValue call(LuaValue arg) {
                        item.stack = ItemStackAPI.checkOrCreateItemStack(arg);
                        return NIL;
                    }
                });

                set("setRenderLayer", new OneArgFunction() {
                    @Override
                    public LuaValue call(LuaValue arg) {
                        item.customLayer = script.getCustomLayer(arg);
                        return NIL;
                    }
                });
            }};
        }
    }
}
