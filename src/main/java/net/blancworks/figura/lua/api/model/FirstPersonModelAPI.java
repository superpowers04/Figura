package net.blancworks.figura.lua.api.model;

import net.blancworks.figura.lua.CustomScript;
import net.minecraft.util.Identifier;
import org.luaj.vm2.LuaTable;

public class FirstPersonModelAPI {

    public static Identifier getID() {
        return new Identifier("default", "first_person_model");
    }

    public static final String MAIN_HAND = "MAIN_HAND";
    public static final String OFF_HAND = "OFF_HAND";

    public static LuaTable getForScript(CustomScript script) {
        return new LuaTable() {{
            set(MAIN_HAND, VanillaModelPartCustomization.getTableForPart(MAIN_HAND, script));
            set(OFF_HAND, VanillaModelPartCustomization.getTableForPart(OFF_HAND, script));
        }};
    }
}
