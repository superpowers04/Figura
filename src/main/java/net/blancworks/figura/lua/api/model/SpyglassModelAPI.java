package net.blancworks.figura.lua.api.model;

import net.blancworks.figura.lua.CustomScript;
import net.minecraft.util.Identifier;
import org.luaj.vm2.LuaTable;

public class SpyglassModelAPI {

    public static final String VANILLA_LEFT_SPYGLASS = "LEFT_SPYGLASS";
    public static final String VANILLA_RIGHT_SPYGLASS = "RIGHT_SPYGLASS";

    public static final Identifier VANILLA_LEFT_SPYGLASS_ID = new Identifier("figura", "left_spyglass");
    public static final Identifier VANILLA_RIGHT_SPYGLASS_ID = new Identifier("figura", "right_spyglass");

    public static Identifier getID() {
        return new Identifier("default", "spyglass_model");
    }

    public static LuaTable getForScript(CustomScript script) {
        return new LuaTable() {{
            set(VANILLA_LEFT_SPYGLASS, VanillaModelPartCustomization.getTableForPart(VANILLA_LEFT_SPYGLASS, script));
            set(VANILLA_RIGHT_SPYGLASS, VanillaModelPartCustomization.getTableForPart(VANILLA_RIGHT_SPYGLASS, script));
        }};
    }
}
