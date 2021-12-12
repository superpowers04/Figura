package net.blancworks.figura.lua.api.model;

import net.blancworks.figura.lua.CustomScript;
import net.blancworks.figura.lua.api.ReadOnlyLuaTable;
import net.blancworks.figura.lua.api.ScriptLocalAPITable;
import net.blancworks.figura.lua.api.block.BlockStateAPI;
import net.blancworks.figura.lua.api.item.ItemStackAPI;
import net.blancworks.figura.lua.api.math.LuaVector;
import net.blancworks.figura.models.CustomModel;
import net.blancworks.figura.models.CustomModelPart;
import net.blancworks.figura.models.CustomModelPartGroup;
import net.blancworks.figura.models.shaders.FiguraRenderLayer;
import net.blancworks.figura.models.shaders.FiguraVertexConsumerProvider;
import net.blancworks.figura.models.tasks.BlockRenderTask;
import net.blancworks.figura.models.tasks.ItemRenderTask;
import net.blancworks.figura.models.tasks.TextRenderTask;
import net.blancworks.figura.utils.TextUtils;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3f;
import net.minecraft.util.math.Vector4f;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.VarArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;

public class CustomModelAPI {

    public static Identifier getID() {
        return new Identifier("default", "model");
    }

    public static ReadOnlyLuaTable getForScript(CustomScript script) {
        return new ScriptLocalAPITable(script, new LuaTable() {{
            if (script.playerData.model != null) {
                for (CustomModelPart part : script.playerData.model.allParts) {
                    set(part.name, new CustomModelPartTable(part, script));
                }
            }
        }});
    }

    private static class CustomModelPartTable extends ReadOnlyLuaTable {
        CustomModelPart targetPart;

        public CustomModelPartTable(CustomModelPart part, CustomScript script) {
            super();
            targetPart = part;
            super.setTable(getTable(script));
        }

        public LuaTable getTable(CustomScript script) {
            LuaTable ret = new LuaTable();

            //int index = 1;
            if (targetPart instanceof CustomModelPartGroup group) {
                for (CustomModelPart child : group.children) {
                    CustomModelPartTable tbl = new CustomModelPartTable(child, script);
                    ret.set(child.name, tbl);
                    //disabled due to memory issues
                    //ret.set(index++, tbl);
                }
            }

            ret.set("getPos", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaVector.of(targetPart.pos);
                }
            });

            ret.set("setPos", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1) {
                    targetPart.pos = LuaVector.checkOrNew(arg1).asV3f();
                    return NIL;
                }
            });

            ret.set("getPivot", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaVector.of(targetPart.pivot);
                }
            });

            ret.set("setPivot", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1) {
                    targetPart.pivot = LuaVector.checkOrNew(arg1).asV3f();
                    return NIL;
                }
            });

            ret.set("getColor", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaVector.of(targetPart.color);
                }
            });

            ret.set("setColor", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1) {
                    targetPart.color = LuaVector.checkOrNew(arg1).asV3f();
                    return NIL;
                }
            });

            ret.set("getScale", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaVector.of(targetPart.scale);
                }
            });

            ret.set("setScale", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1) {
                    targetPart.scale = LuaVector.checkOrNew(arg1).asV3f();
                    return NIL;
                }
            });

            ret.set("getRot", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaVector.of(targetPart.rot);
                }
            });

            ret.set("setRot", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1) {
                    targetPart.rot = LuaVector.checkOrNew(arg1).asV3f();
                    return NIL;
                }
            });

            ret.set("getUVData", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1) {
                    try {
                        CustomModelPart.UV uv = CustomModelPart.UV.valueOf(arg1.checkjstring());

                        if (uv == CustomModelPart.UV.ALL)
                            throw new LuaError("Cannot get UV data for ALL faces at once");

                        CustomModelPart.uvData data = targetPart.UVCustomizations.get(uv);
                        if (data == null) return NIL;

                        Vec2f offset = data.uvOffset;
                        Vec2f size = data.uvSize;

                        return new LuaVector(offset.x, offset.y, size.x - offset.x, size.y - offset.y);
                    } catch (Exception ignored) {
                        throw new LuaError("UV Face not found!");
                    }
                }
            });

            ret.set("setUVData", new TwoArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1, LuaValue arg2) {
                    try {
                        CustomModelPart.UV uv = CustomModelPart.UV.valueOf(arg1.checkjstring());

                        LuaVector vec = LuaVector.checkOrNew(arg2);
                        Vec2f offset = new Vec2f(vec.x(), vec.y());
                        Vec2f size = new Vec2f(vec.z() + vec.x(), vec.w() + vec.y());

                        if (uv == CustomModelPart.UV.ALL) {
                            CustomModelPart.UV[] uvs = CustomModelPart.UV.values();

                            for (CustomModelPart.UV uvPart : uvs) {
                                CustomModelPart.uvData data = targetPart.UVCustomizations.get(uvPart);
                                if (data == null) {
                                    data = new CustomModelPart.uvData();
                                    targetPart.UVCustomizations.put(uvPart, data);
                                }

                                data.setUVOffset(offset);
                                data.setUVSize(size);
                            }
                        } else {
                            CustomModelPart.uvData data = targetPart.UVCustomizations.get(uv);
                            if (data == null) {
                                data = new CustomModelPart.uvData();
                                targetPart.UVCustomizations.put(uv, data);
                            }

                            data.setUVOffset(offset);
                            data.setUVSize(size);
                        }

                        targetPart.applyUVMods(targetPart.texSize);
                    } catch (Exception ignored) {
                        throw new LuaError("UV Face not found!");
                    }
                    return NIL;
                }
            });

            ret.set("getTextureSize", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    Vec2f uv = targetPart.texSize;
                    return LuaVector.of(uv);
                }
            });

            ret.set("setTextureSize", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    targetPart.applyUVMods(LuaVector.checkOrNew(arg).asV2f());
                    return NIL;
                }
            });

            ret.set("getUV", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    Vec2f uv = targetPart.uvOffset;
                    return LuaVector.of(uv);
                }
            });

            ret.set("setUV", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    LuaVector vec = LuaVector.checkOrNew(arg);
                    targetPart.uvOffset = new Vec2f(vec.x(), vec.y());
                    return NIL;
                }
            });

            ret.set("getParentType", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaString.valueOf(targetPart.parentType.toString());
                }
            });

            ret.set("setParentType", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1) {
                    CustomModel model = targetPart.model;
                    
                    if (targetPart.isSpecial())
                        model.removeSpecialPart(targetPart);

                    try {
                        targetPart.parentType = CustomModelPart.ParentType.valueOf(arg1.checkjstring());

                        if (targetPart.isSpecial())
                            model.addSpecialPart(targetPart);
                    } catch (Exception ignored) {
                        targetPart.parentType = CustomModelPart.ParentType.Model;
                    }

                    return NIL;
                }
            });

            ret.set("getMimicMode", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaBoolean.valueOf(targetPart.isMimicMode);
                }
            });

            ret.set("setMimicMode", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1) {
                    targetPart.isMimicMode = arg1.checkboolean();
                    return NIL;
                }
            });

            ret.set("getEnabled", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaBoolean.valueOf(targetPart.visible);
                }
            });

            ret.set("setEnabled", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    targetPart.visible = arg.checkboolean();
                    return null;
                }
            });

            ret.set("getShader", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaString.valueOf(targetPart.shaderType.toString());
                }
            });

            ret.set("setShader", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1) {
                    try {
                        targetPart.shaderType = CustomModelPart.ShaderType.valueOf(arg1.checkjstring());
                    } catch (Exception ignored) {
                        targetPart.shaderType = CustomModelPart.ShaderType.None;
                    }

                    return NIL;
                }
            });

            ret.set("setRenderLayer", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1) {
                    if (!targetPart.model.owner.canRenderCustomLayers())
                        return NIL;

                    FiguraRenderLayer layer = null;

                    if (!arg1.isnil() && targetPart.model.owner.getVCP() instanceof FiguraVertexConsumerProvider)
                        layer = ((FiguraVertexConsumerProvider)targetPart.model.owner.getVCP()).getLayer(arg1.checkjstring());

                    targetPart.customLayer = layer;
                    return NIL;
                }
            });
            
            ret.set("getTexture", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaString.valueOf(targetPart.textureType.toString());
                }
            });

            ret.set("setTexture", new TwoArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1, LuaValue arg2) {
                    try {
                        targetPart.textureType = CustomModelPart.TextureType.valueOf(arg1.checkjstring());

                        if (targetPart.textureType == CustomModelPart.TextureType.Resource)
                            targetPart.textureVanilla = new Identifier(arg2.checkjstring());
                    } catch (Exception ignored) {
                        targetPart.textureType = CustomModelPart.TextureType.Custom;
                    }

                    return NIL;
                }
            });

            ret.set("getExtraTexEnabled", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaBoolean.valueOf(targetPart.extraTex);
                }
            });

            ret.set("setExtraTexEnabled", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    targetPart.extraTex = arg.checkboolean();
                    return null;
                }
            });

            ret.set("getCullEnabled", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaBoolean.valueOf(targetPart.cull);
                }
            });

            ret.set("setCullEnabled", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    targetPart.cull = arg.checkboolean();
                    return null;
                }
            });

            ret.set("partToWorldPos", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1) {
                    LuaVector v = LuaVector.checkOrNew(arg1);

                    Vector4f v4f = new Vector4f(v.x() / 16f, v.y() / -16f, v.z() / 16f, 1f);

                    v4f.transform(targetPart.lastModelMatrix);

                    return LuaVector.of(new Vec3f(v4f.getX(), v4f.getY(), v4f.getZ()));
                }
            });

            ret.set("partToWorldDir", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1) {
                    LuaVector v = LuaVector.checkOrNew(arg1);

                    Vec3f v3f = new Vec3f(v.x(), -v.y(), v.z());

                    v3f.transform(targetPart.lastNormalMatrix);

                    return LuaVector.of(v3f);
                }
            });

            ret.set("worldToPartPos", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1) {
                    LuaVector v = LuaVector.checkOrNew(arg1);

                    Vector4f v4f = new Vector4f(v.x(), v.y(), v.z(), 1f);

                    v4f.transform(targetPart.lastModelMatrixInverse);

                    return LuaVector.of(new Vec3f(v4f.getX() * 16f, v4f.getY() * -16f, v4f.getZ() * 16f));
                }
            });

            ret.set("worldToPartDir", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1) {
                    LuaVector v = LuaVector.checkOrNew(arg1);

                    Vec3f v3f = new Vec3f(v.x(), v.y(), v.z());

                    v3f.transform(targetPart.lastNormalMatrixInverse);

                    return LuaVector.of(new Vec3f(v3f.getX(), -v3f.getY(), v3f.getZ()));
                }
            });

            ret.set("getOpacity", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaNumber.valueOf(targetPart.alpha);
                }
            });

            ret.set("setOpacity", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1) {
                    targetPart.alpha = Math.max(0f, Math.min(arg1.checknumber().tofloat(), 1f));
                    return NIL;
                }
            });

            ret.set("getType", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(targetPart.getPartType().toString());
                }
            });

            ret.set("getName", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(targetPart.name);
                }
            });

            ret.set("renderItem", new VarArgFunction() {
                @Override
                public Varargs onInvoke(Varargs args) {
                    String name = args.arg(1).checkjstring();
                    ItemStack stack = ItemStackAPI.checkOrCreateItemStack(args.arg(2));
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

                    targetPart.renderTasks.put(name, new ItemRenderTask(stack, mode, emissive, pos, rot, scale, customLayer));
                    return NIL;
                }
            });

            ret.set("renderBlock", new VarArgFunction() {
                @Override
                public Varargs onInvoke(Varargs args) {
                    String name = args.arg(1).checkjstring();
                    BlockState state = BlockStateAPI.checkOrCreateBlockState(args.arg(2));
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

                    targetPart.renderTasks.put(name, new BlockRenderTask(state, emissive, pos, rot, scale, customLayer));
                    return NIL;
                }
            });

            ret.set("renderText", new VarArgFunction() {
                @Override
                public Varargs onInvoke(Varargs args) {
                    String name = args.arg(1).checkjstring();
                    String textString = TextUtils.noBadges4U(args.arg(2).checkjstring()).replaceAll("[\n\r]", " ");

                    if (textString.length() > 65535)
                        throw new LuaError("Text too long - oopsie!");

                    Text text = TextUtils.tryParseJson(textString);
                    boolean emissive = !args.arg(3).isnil() && args.arg(3).checkboolean();
                    Vec3f pos = args.arg(4).isnil() ? null : LuaVector.checkOrNew(args.arg(4)).asV3f();
                    Vec3f rot = args.arg(5).isnil() ? null : LuaVector.checkOrNew(args.arg(5)).asV3f();
                    Vec3f scale = args.arg(6).isnil() ? null : LuaVector.checkOrNew(args.arg(6)).asV3f();

                    targetPart.renderTasks.put(name, new TextRenderTask(text, emissive, pos, rot, scale));
                    return NIL;
                }
            });

            ret.set("removeRenderTask", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1) {
                    synchronized (targetPart.renderTasks) {
                        targetPart.renderTasks.remove(arg1.checkjstring());
                        return NIL;
                    }
                }
            });

            ret.set("clearAllRenderTasks", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    synchronized (targetPart.renderTasks) {
                        targetPart.renderTasks.clear();
                        return NIL;
                    }
                }
            });

            return ret;
        }
    }
}
