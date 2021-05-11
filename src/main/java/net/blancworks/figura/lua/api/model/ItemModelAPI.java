package net.blancworks.figura.lua.api.model;

import net.blancworks.figura.lua.CustomScript;
import net.blancworks.figura.lua.api.ReadOnlyLuaTable;
import net.blancworks.figura.lua.api.ScriptLocalAPITable;
import net.blancworks.figura.lua.api.math.LuaVector;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.util.Identifier;
import org.luaj.vm2.LuaBoolean;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;

public class ItemModelAPI {

    public static final String VANILLA_LEFT_HAND = "LEFT_HAND";
    public static final String VANILLA_RIGHT_HAND = "RIGHT_HAND";

    public static final Identifier VANILLA_LEFT_HAND_ID = new Identifier("figura", "left_hand");
    public static final Identifier VANILLA_RIGHT_HAND_ID = new Identifier("figura", "right_hand");


    public static Identifier getID() {
        return new Identifier("default", "held_item_model");
    }

    public static ReadOnlyLuaTable getForScript(CustomScript script) {
        ScriptLocalAPITable producedTable = new ScriptLocalAPITable(script, new LuaTable() {{
            PlayerEntityModel mdl = script.playerData.vanillaModel;

            set(VANILLA_LEFT_HAND, getTableForPart(VANILLA_LEFT_HAND, script));
            set(VANILLA_RIGHT_HAND, getTableForPart(VANILLA_RIGHT_HAND, script));
        }});

        return producedTable;
    }

    public static ReadOnlyLuaTable getTableForPart(String accessor, CustomScript script) {
        HeldItemModelTable producedTable = new HeldItemModelTable(accessor, script);
        return producedTable;
    }

    private static class HeldItemModelTable extends ScriptLocalAPITable {
        String accessor;

        public HeldItemModelTable(String accessor, CustomScript script) {
            super(script);
            this.accessor = accessor;
            super.setTable(getTable());
        }

        public LuaTable getTable() {
            LuaTable ret = new LuaTable();

            ret.set("getPos", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaVector.of(targetScript.getOrMakePartCustomization(accessor).pos);
                }
            });
            
            ret.set("setPos", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1) {
                    VanillaModelPartCustomization customization = targetScript.getOrMakePartCustomization(accessor);
                    customization.pos = LuaVector.checkOrNew(arg1).asV3f();

                    return NIL;
                }
            });

            ret.set("getRot", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaVector.of(targetScript.getOrMakePartCustomization(accessor).rot);
                }
            });

            ret.set("setRot", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1) {
                    VanillaModelPartCustomization customization = targetScript.getOrMakePartCustomization(accessor);
                    customization.rot = LuaVector.checkOrNew(arg1).asV3f();

                    return NIL;
                }
            });

            ret.set("getEnabled", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    VanillaModelPartCustomization customization = targetScript.getOrMakePartCustomization(accessor);

                    if (customization != null)
                        return LuaBoolean.valueOf(customization.visible);

                    return NIL;
                }
            });

            ret.set("setEnabled", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    VanillaModelPartCustomization customization = targetScript.getOrMakePartCustomization(accessor);

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
