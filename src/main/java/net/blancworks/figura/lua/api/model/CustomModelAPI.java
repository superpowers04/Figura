package net.blancworks.figura.lua.api.model;

import net.blancworks.figura.lua.CustomScript;
import net.blancworks.figura.lua.api.math.LuaVector;
import net.blancworks.figura.models.CustomModel;
import net.blancworks.figura.models.CustomModelPart;
import net.blancworks.figura.models.CustomModelPartGroup;
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

    public static LuaTable getForScript(CustomScript script) {
        return new LuaTable() {{
            if (script.avatarData.model != null) {
                for (CustomModelPart part : script.avatarData.model.allParts) {
                    set(part.name, getTableForPart(part, script));
                }
            }
        }};
    }

    public static LuaTable getTableForPart(CustomModelPart targetPart, CustomScript targetScript) {
        LuaTable ret = new LuaTable();

        if (targetPart instanceof CustomModelPartGroup group) {
            for (CustomModelPart child : group.children) {
                ret.set(child.name, getTableForPart(child, targetScript));
            }

            ret.set("getChildren", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    LuaTable tbl = new LuaTable();

                    for (CustomModelPart child : group.children)
                        tbl.set(child.name, getTableForPart(child, targetScript));

                    return tbl;
                }
            });

            ret.set("getAnimPos", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    Vec3f pos = group.animPos.copy();
                    pos.add(group.animPosOverride);

                    return LuaVector.of(pos);
                }
            });

            ret.set("getAnimRot", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaVector.of(group.animRot);
                }
            });

            ret.set("getAnimScale", new ZeroArgFunction() {
                    @Override
                    public LuaValue call() {
                        return LuaVector.of(group.animScale);
                    }
                });
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
                targetPart.uvOffset = new Vec2f(vec.x() % 1, vec.y() % 1);
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
                return NIL;
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
                targetPart.customLayer = targetScript.getCustomLayer(arg1);
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
                return NIL;
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
                return NIL;
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

        ret.set("addRenderTask", new VarArgFunction() {
            @Override
            public Varargs onInvoke(Varargs args) {
                return RenderTaskAPI.addTask(targetPart, targetScript, args);
            }
        });

        ret.set("getRenderTask", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue arg1) {
                RenderTaskAPI.RenderTaskTable tbl = targetPart.renderTasks.get(arg1.checkjstring());
                if (tbl == null) return NIL;
                return tbl.getTable(targetScript);
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

        ret.set("getLight", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                return targetPart.light == null ? NIL : LuaVector.of(targetPart.light);
            }
        });

        ret.set("setLight", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue arg1) {
                targetPart.light = arg1.isnil() ? null : LuaVector.checkOrNew(arg1).asV2f();
                return NIL;
            }
        });

        ret.set("getOverlay", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                return targetPart.overlay == null ? NIL : LuaVector.of(targetPart.overlay);
            }
        });

        ret.set("setOverlay", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue arg1) {
                targetPart.overlay = arg1.isnil() ? null : LuaVector.checkOrNew(arg1).asV2f();
                return NIL;
            }
        });

        return ret;
    }
}
