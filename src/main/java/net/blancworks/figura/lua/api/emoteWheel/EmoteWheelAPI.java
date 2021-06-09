package net.blancworks.figura.lua.api.emoteWheel;

import net.blancworks.figura.lua.CustomScript;
import net.blancworks.figura.lua.api.ReadOnlyLuaTable;
import net.blancworks.figura.lua.api.ScriptLocalAPITable;
import net.blancworks.figura.lua.api.item.ItemStackAPI;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;

public class EmoteWheelAPI {
    public static final String SLOT_1 = "SLOT_1";
    public static final String SLOT_2 = "SLOT_2";
    public static final String SLOT_3 = "SLOT_3";
    public static final String SLOT_4 = "SLOT_4";
    public static final String SLOT_5 = "SLOT_5";
    public static final String SLOT_6 = "SLOT_6";
    public static final String SLOT_7 = "SLOT_7";
    public static final String SLOT_8 = "SLOT_8";

    public static Identifier getID() {
        return new Identifier("default", "emote_wheel");
    }

    public static ReadOnlyLuaTable getForScript(CustomScript script) {
        return new ScriptLocalAPITable(script, new LuaTable() {{
            set(SLOT_1, getTableForPart(SLOT_1, script));
            set(SLOT_2, getTableForPart(SLOT_2, script));
            set(SLOT_3, getTableForPart(SLOT_3, script));
            set(SLOT_4, getTableForPart(SLOT_4, script));
            set(SLOT_5, getTableForPart(SLOT_5, script));
            set(SLOT_6, getTableForPart(SLOT_6, script));
            set(SLOT_7, getTableForPart(SLOT_7, script));
            set(SLOT_8, getTableForPart(SLOT_8, script));
        }});
    }

    public static ReadOnlyLuaTable getTableForPart(String accessor, CustomScript script) {
        return new EmoteWheelTable(accessor, script);
    }

    private static class EmoteWheelTable extends ScriptLocalAPITable {
        String accessor;

        public EmoteWheelTable(String accessor, CustomScript script) {
            super(script);
            this.accessor = accessor;
            super.setTable(getTable());
        }

        public LuaTable getTable() {
            LuaTable ret = new LuaTable();
            ret.set("getFunction", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return targetScript.getOrMakeEmoteWheelCustomization(accessor).function;
                }
            });

            ret.set("setFunction", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1) {
                    targetScript.getOrMakeEmoteWheelCustomization(accessor).function = arg1.checkfunction();
                    return NIL;
                }
            });

            ret.set("getItem", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return ItemStackAPI.getTable(targetScript.getOrMakeEmoteWheelCustomization(accessor).item);
                }
            });

            ret.set("setItem", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1) {
                    targetScript.getOrMakeEmoteWheelCustomization(accessor).item = Registry.ITEM.get(Identifier.tryParse(arg1.checkjstring())).getDefaultStack();
                    return NIL;
                }
            });

            ret.set("getTitle", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(targetScript.getOrMakeEmoteWheelCustomization(accessor).title);
                }
            });

            ret.set("setTitle", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1) {
                    targetScript.getOrMakeEmoteWheelCustomization(accessor).title = arg1.checkjstring();
                    return NIL;
                }
            });

            return ret;
        }
    }
}
