package net.blancworks.figura.lua.api.model;

import net.blancworks.figura.PlayerData;
import net.blancworks.figura.lua.CustomScript;
import net.blancworks.figura.lua.api.ReadOnlyLuaTable;
import net.blancworks.figura.lua.api.ScriptLocalAPITable;
import net.blancworks.figura.lua.api.math.LuaVector;
import net.blancworks.figura.models.CustomModelPart;
import net.blancworks.figura.models.CustomModelPartCuboid;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtFloat;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.Identifier;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.*;
import net.minecraft.util.math.*;

public class CustomModelAPI {

    public static Identifier getID() {
        return new Identifier("default", "model");
    }

    public static ReadOnlyLuaTable getForScript(CustomScript script) {
        return new ScriptLocalAPITable(script, new LuaTable() {{
            for (CustomModelPart part : script.playerData.model.allParts) {
                set(part.name, new CustomModelPartTable(part, script.playerData));
            }
        }});
    }

    private static class CustomModelPartTable extends ReadOnlyLuaTable {
        CustomModelPart targetPart;
        PlayerData partOwner;

        public CustomModelPartTable(CustomModelPart part, PlayerData owner) {
            super();
            targetPart = part;
            partOwner = owner;
            super.setTable(getTable());
        }

        public LuaTable getTable() {
            LuaTable ret = new LuaTable();

            int index = 1;
            for (CustomModelPart child : targetPart.children) {
                CustomModelPartTable tbl = new CustomModelPartTable(child, partOwner);
                ret.set(child.name, tbl);
                ret.set(index++, tbl);
            }

            ret.set("addPart", new TwoArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1, LuaValue arg2) {
                    LuaVector size = LuaVector.checkOrNew(arg1);
                    LuaVector uv = LuaVector.checkOrNew(arg2);

                    CustomModelPartCuboid part = new CustomModelPartCuboid();
                    part.parentType = CustomModelPart.ParentType.Model;
                    part.name = "aa";

                    NbtCompound cuboidPropertiesTag = new NbtCompound();

                    cuboidPropertiesTag.put("f", new NbtList() {{
                        add(NbtFloat.of(0.0f));
                        add(NbtFloat.of(0.0f));
                        add(NbtFloat.of(0.0f));
                    }});

                    cuboidPropertiesTag.put("t", new NbtList() {{
                        add(NbtFloat.of(size.x()));
                        add(NbtFloat.of(size.y()));
                        add(NbtFloat.of(size.z()));
                    }});

                    cuboidPropertiesTag.put("tw", NbtFloat.of(partOwner.model.texWidth));
                    cuboidPropertiesTag.put("th", NbtFloat.of(partOwner.model.texHeight));

                    NbtList uvNbt = new NbtList();
                    uvNbt.add(NbtFloat.of(Math.min(uv.x(), 1) * partOwner.model.texWidth));
                    uvNbt.add(NbtFloat.of(Math.min(uv.y(), 1) * partOwner.model.texHeight));
                    uvNbt.add(NbtFloat.of(Math.min(uv.z(), 1) * partOwner.model.texWidth));
                    uvNbt.add(NbtFloat.of(Math.min(uv.w(), 1) * partOwner.model.texHeight));

                    NbtCompound faceComponent = new NbtCompound() {{
                        put("uv", uvNbt);
                        put("texture", NbtFloat.of(0));
                    }};

                    cuboidPropertiesTag.put("n", faceComponent);
                    cuboidPropertiesTag.put("s", faceComponent);
                    cuboidPropertiesTag.put("e", faceComponent);
                    cuboidPropertiesTag.put("w", faceComponent);
                    cuboidPropertiesTag.put("u", faceComponent);
                    cuboidPropertiesTag.put("d", faceComponent);

                    part.cuboidProperties = cuboidPropertiesTag;
                    part.rebuild();

                    targetPart.children.add(part);

                    return new CustomModelPartTable(part, partOwner);
                }
            });

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

            ret.set("getUV", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    Vec3f uv = new Vec3f(targetPart.uOffset, targetPart.vOffset, 0);
                    return LuaVector.of(uv);
                }
            });

            ret.set("setUV", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1) {
                    LuaVector v = LuaVector.checkOrNew(arg1);
                    targetPart.uOffset = v.x() % 1;
                    targetPart.vOffset = v.y() % 1;
                    if (targetPart.uOffset < 0) targetPart.uOffset++;
                    if (targetPart.vOffset < 0) targetPart.vOffset++;

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
                    targetPart.parentType = CustomModelPart.ParentType.valueOf(arg1.checkjstring());

                    if (targetPart.isParentSpecial())
                        partOwner.model.sortAllParts();

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

            ret.set("getHidden", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaBoolean.valueOf(targetPart.isHidden);
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
                    targetPart.shaderType = CustomModelPart.ShaderType.valueOf(arg1.checkjstring());
                    return NIL;
                }
            });

            ret.set("partToWorldPos", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1) {
                    LuaVector v = LuaVector.checkOrNew(arg1);

                    Vector4f v4f = new Vector4f(v.x() / 16.0f, -(v.y() / 16.0f), v.z() / 16.0f, 1.0f);

                    v4f.transform(targetPart.lastModelMatrix);

                    return LuaVector.of(new Vec3f(v4f.getX(), v4f.getY(), v4f.getZ()));
                }
            });

            ret.set("partToWorldDir", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1) {
                    LuaVector v = LuaVector.checkOrNew(arg1);

                    Vec3f v3f = new Vec3f(v.x(), -(v.y()), v.z());

                    v3f.transform(targetPart.lastNormalMatrix);

                    return LuaVector.of(v3f);
                }
            });

            ret.set("worldToPartPos", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1) {
                    LuaVector v = LuaVector.checkOrNew(arg1);

                    Vec3f v3f = new Vec3f(v.x() / 16.0f, -(v.y()) / 16.0f, v.z() / 16.0f);

                    v3f.transform(targetPart.lastNormalMatrix);

                    return LuaVector.of(v3f);
                }
            });

            ret.set("worldToPartDir", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1) {
                    LuaVector v = LuaVector.checkOrNew(arg1);

                    Vec3f v3f = new Vec3f(v.x(), -(v.y()), v.z());

                    v3f.transform(targetPart.lastNormalMatrix);

                    return LuaVector.of(v3f);
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

            return ret;
        }
    }
}
