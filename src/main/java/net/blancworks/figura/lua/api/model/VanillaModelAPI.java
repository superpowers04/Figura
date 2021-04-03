package net.blancworks.figura.lua.api.model;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import net.blancworks.figura.lua.CustomScript;
import net.blancworks.figura.lua.LuaUtils;
import net.blancworks.figura.lua.api.ReadOnlyLuaTable;
import net.blancworks.figura.lua.api.ScriptLocalAPITable;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.util.Identifier;
import org.luaj.vm2.LuaBoolean;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;

public class VanillaModelAPI {

    //Main body accessors
    public static final String VANILLA_HEAD = "HEAD";
    public static final String VANILLA_TORSO = "TORSO";
    public static final String VANILLA_LEFT_ARM = "LEFT_LEG";
    public static final String VANILLA_RIGHT_ARM = "RIGHT_ARM";
    public static final String VANILLA_LEFT_LEG = "LEFT_LEG";
    public static final String VANILLA_RIGHT_LEG = "RIGHT_LEG";

    //Layered accessors
    public static final String VANILLA_HAT = "HAT";
    public static final String VANILLA_JACKET = "JACKET";
    public static final String VANILLA_LEFT_SLEEVE = "LEFT_SLEEVE";
    public static final String VANILLA_RIGHT_SLEEVE = "RIGHT_SLEEVE";
    public static final String VANILLA_LEFT_PANTS = "LEFT_PANTS";
    public static final String VANILLA_RIGHT_PANTS = "RIGHT_PANTS";



    public static Identifier getID() {
        return new Identifier("default", "vanilla_model");
    }

    public static ReadOnlyLuaTable getForScript(CustomScript script) {
        ScriptLocalAPITable producedTable = new ScriptLocalAPITable(script, new LuaTable() {{
            PlayerEntityModel mdl = script.playerData.vanillaModel;

            set(VANILLA_HEAD, getTableForPart(mdl.head, VANILLA_HEAD, script));
            set(VANILLA_TORSO, getTableForPart(mdl.torso, VANILLA_TORSO, script));

            set(VANILLA_LEFT_ARM, getTableForPart(mdl.leftArm, VANILLA_LEFT_ARM, script));
            set(VANILLA_RIGHT_ARM, getTableForPart(mdl.rightArm, VANILLA_RIGHT_ARM, script));

            set(VANILLA_LEFT_LEG, getTableForPart(mdl.leftLeg, VANILLA_LEFT_LEG, script));
            set(VANILLA_RIGHT_LEG, getTableForPart(mdl.rightLeg, VANILLA_RIGHT_LEG, script));

            set(VANILLA_HAT, getTableForPart(mdl.helmet, VANILLA_HAT, script));
            set(VANILLA_JACKET, getTableForPart(mdl.jacket, VANILLA_JACKET, script));

            set(VANILLA_LEFT_SLEEVE, getTableForPart(mdl.leftSleeve, VANILLA_LEFT_SLEEVE, script));
            set(VANILLA_RIGHT_SLEEVE, getTableForPart(mdl.rightSleeve, VANILLA_RIGHT_SLEEVE, script));

            set(VANILLA_LEFT_PANTS, getTableForPart(mdl.leftPantLeg, VANILLA_LEFT_PANTS, script));
            set(VANILLA_RIGHT_PANTS, getTableForPart(mdl.rightPantLeg, VANILLA_RIGHT_PANTS, script));
        }});

        return producedTable;
    }

    public static ReadOnlyLuaTable getTableForPart(ModelPart part, String accessor, CustomScript script) {
        ModelPartTable producedTable = new ModelPartTable(part, accessor, script);
        return producedTable;
    }

    private static class ModelPartTable extends ScriptLocalAPITable {
        ModelPart targetPart;
        String accessor;

        public ModelPartTable(ModelPart part, String accessor, CustomScript script) {
            super(script);
            targetPart = part;
            this.accessor = accessor;
            super.setTable(getTable());
        }

        public LuaTable getTable() {
            LuaTable ret = new LuaTable();

            ret.set("getPos", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    Vector3f v = targetScript.getOrMakePartCustomization(accessor).pos;

                    if(v == null)
                        return NIL;
                    
                    return LuaUtils.getTableFromVector3f(v);
                }
            });

            ret.set("setPos", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1) {
                    FloatArrayList fas = LuaUtils.getFloatsFromTable(arg1.checktable());
                    VanillaModelPartCustomization customization = targetScript.getOrMakePartCustomization(accessor);
                    customization.pos = new Vector3f(
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
                    Vector3f v = targetScript.getOrMakePartCustomization(accessor).rot;
                    
                    if(v == null)
                        return NIL;
                    
                    return LuaUtils.getTableFromVector3f(v);
                }
            });

            ret.set("setRot", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1) {
                    FloatArrayList fas = LuaUtils.getFloatsFromTable(arg1.checktable());
                    VanillaModelPartCustomization customization = targetScript.getOrMakePartCustomization(accessor);
                    customization.rot = new Vector3f(
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
                    VanillaModelPartCustomization customization = targetScript.getOrMakePartCustomization(accessor);
                    
                    if(customization != null)
                        return LuaBoolean.valueOf(customization.visible);

                    return NIL;
                }
            });
            
            ret.set("setEnabled", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    VanillaModelPartCustomization customization = targetScript.getOrMakePartCustomization(accessor);

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
                    return LuaUtils.getTableFromVector3f(new Vector3f(targetPart.pivotX, targetPart.pivotY, targetPart.pivotZ));
                }
            });

            ret.set("getOriginRot", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaUtils.getTableFromVector3f(new Vector3f(targetPart.pitch, targetPart.yaw, targetPart.roll));
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
