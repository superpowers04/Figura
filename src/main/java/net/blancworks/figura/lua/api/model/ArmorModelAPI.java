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

public class ArmorModelAPI {


    public static Identifier getID() {
        return new Identifier("default", "armor_model");
    }

    public static ReadOnlyLuaTable getForScript(CustomScript script) {
        ScriptLocalAPITable producedTable = new ScriptLocalAPITable(script, new LuaTable() {{
            PlayerEntityModel mdl = script.playerData.vanillaModel;

            set("HELMET", getTableForPart(12, script));

            set("CHESTPLATE", getTableForPart(13, script));
            set("LEGGINGS", getTableForPart(14, script));

            set("BOOTS", getTableForPart(15, script));
        }});

        return producedTable;
    }

    public static ReadOnlyLuaTable getTableForPart(int index, CustomScript script) {
        ArmorPartTable producedTable = new ArmorPartTable(index, script);
        return producedTable;
    }

    private static class ArmorPartTable extends ScriptLocalAPITable {
        int customizationIndex;

        public ArmorPartTable(int index, CustomScript script) {
            super(script);
            customizationIndex = index;
            super.setTable(getTable());
        }

        public LuaTable getTable() {
            LuaTable ret = new LuaTable();
            
            ret.set("setPos", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1) {
                    FloatArrayList fas = LuaUtils.getFloatsFromTable(arg1.checktable());
                    VanillaModelPartCustomization customization = targetScript.vanillaModifications[customizationIndex];
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
                    Vector3f v = targetScript.vanillaModifications[customizationIndex].rot;

                    if (v == null)
                        return NIL;

                    return LuaUtils.getTableFromVector3f(v);
                }
            });

            ret.set("setRot", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1) {
                    FloatArrayList fas = LuaUtils.getFloatsFromTable(arg1.checktable());
                    VanillaModelPartCustomization customization = targetScript.vanillaModifications[customizationIndex];
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
                    VanillaModelPartCustomization customization = targetScript.vanillaModifications[customizationIndex];

                    if (customization != null)
                        return LuaBoolean.valueOf(customization.visible);

                    return NIL;
                }
            });

            ret.set("setEnabled", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    VanillaModelPartCustomization customization = targetScript.vanillaModifications[customizationIndex];

                    if (arg.isnil()) {
                        customization.visible = null;
                        return NIL;
                    }

                    customization.visible = arg.checkboolean();

                    return NIL;
                }
            });

            return ret;
        }
    }

}
