package net.blancworks.figura.lua.api.model;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import net.blancworks.figura.lua.CustomScript;
import net.blancworks.figura.lua.LuaUtils;
import net.blancworks.figura.lua.api.ReadOnlyLuaTable;
import net.blancworks.figura.lua.api.ScriptLocalAPITable;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.util.math.Vec3f;
import net.minecraft.util.Identifier;
import org.luaj.vm2.LuaBoolean;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;

public class VanillaModelAPI {


    public static Identifier getID() {
        return new Identifier("default", "vanilla_model");
    }

    public static ReadOnlyLuaTable getForScript(CustomScript script) {
        ScriptLocalAPITable producedTable = new ScriptLocalAPITable(script, new LuaTable() {{
            PlayerEntityModel mdl = script.playerData.vanillaModel;

            set("HEAD", getTableForPart(mdl.head, 0, script));
            set("TORSO", getTableForPart(mdl.body, 1, script));

            set("LEFT_ARM", getTableForPart(mdl.leftArm, 2, script));
            set("RIGHT_ARM", getTableForPart(mdl.rightArm, 3, script));

            set("LEFT_LEG", getTableForPart(mdl.leftLeg, 4, script));
            set("RIGHT_LEG", getTableForPart(mdl.rightLeg, 5, script));

            set("HAT", getTableForPart(mdl.hat, 6, script));
            set("JACKET", getTableForPart(mdl.jacket, 7, script));

            set("LEFT_SLEEVE", getTableForPart(mdl.leftSleeve, 8, script));
            set("RIGHT_SLEEVE", getTableForPart(mdl.rightSleeve, 9, script));

            set("LEFT_PANTS", getTableForPart(mdl.leftPants, 10, script));
            set("RIGHT_PANTS", getTableForPart(mdl.rightPants, 11, script));
        }});

        return producedTable;
    }

    public static ReadOnlyLuaTable getTableForPart(ModelPart part, int index, CustomScript script) {
        ModelPartTable producedTable = new ModelPartTable(part, index, script);
        return producedTable;
    }

    private static class ModelPartTable extends ScriptLocalAPITable {
        ModelPart targetPart;
        int customizationIndex;

        public ModelPartTable(ModelPart part, int index, CustomScript script) {
            super(script);
            targetPart = part;
            customizationIndex = index;
            super.setTable(getTable());
        }

        public LuaTable getTable() {
            LuaTable ret = new LuaTable();

            ret.set("getPos", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    Vec3f v = targetScript.vanillaModifications[customizationIndex].pos;

                    if(v == null)
                        return NIL;
                    
                    return LuaUtils.getTableFromVector3f(v);
                }
            });

            ret.set("setPos", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1) {
                    FloatArrayList fas = LuaUtils.getFloatsFromTable(arg1.checktable());
                    VanillaModelPartCustomization customization = targetScript.vanillaModifications[customizationIndex];
                    customization.pos = new Vec3f(
                            fas.getFloat(0),
                            fas.getFloat(1),
                            fas.getFloat(2)
                    );

                    return NIL;
                }
            });

            ret.set("getRot", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    Vec3f v = targetScript.vanillaModifications[customizationIndex].rot;
                    
                    if(v == null)
                        return NIL;
                    
                    return LuaUtils.getTableFromVector3f(v);
                }
            });

            ret.set("setRot", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1) {
                    FloatArrayList fas = LuaUtils.getFloatsFromTable(arg1.checktable());
                    VanillaModelPartCustomization customization = targetScript.vanillaModifications[customizationIndex];
                    customization.rot = new Vec3f(
                            fas.getFloat(0),
                            fas.getFloat(1),
                            fas.getFloat(2)
                    );
                    return NIL;
                }
            });
            
            ret.set("getEnabled", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    VanillaModelPartCustomization customization = targetScript.vanillaModifications[customizationIndex];
                    
                    if(customization != null)
                        return LuaBoolean.valueOf(customization.visible);

                    return NIL;
                }
            });
            
            ret.set("setEnabled", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    VanillaModelPartCustomization customization = targetScript.vanillaModifications[customizationIndex];

                    if(arg.isnil()) {
                        customization.visible = null;
                        return NIL;
                    }

                    customization.visible = arg.checkboolean();
                    
                    return NIL;
                }
            });


            ret.set("getOriginPos", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaUtils.getTableFromVector3f(new Vec3f(targetPart.pivotX, targetPart.pivotY, targetPart.pivotZ));
                }
            });

            ret.set("getOriginRot", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaUtils.getTableFromVector3f(new Vec3f(targetPart.pitch, targetPart.yaw, targetPart.roll));
                }
            });

            ret.set("getOriginEnabled", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaBoolean.valueOf(targetPart.visible);
                }
            });
            
            
            return ret;
        }
    }

}
