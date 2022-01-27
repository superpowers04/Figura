package net.blancworks.figura.lua.api.model;

import net.blancworks.figura.lua.CustomScript;
import net.minecraft.util.Identifier;
import org.luaj.vm2.LuaTable;

public class ElytraModelAPI {

    public static final String VANILLA_LEFT_WING = "LEFT_WING";
    public static final String VANILLA_RIGHT_WING = "RIGHT_WING";

    public static final Identifier VANILLA_LEFT_WING_ID = new Identifier("figura", "left_wing");
    public static final Identifier VANILLA_RIGHT_WING_ID = new Identifier("figura", "right_wing");
    
    public static Identifier getID() {
        return new Identifier("default", "elytra_model");
    }

    public static LuaTable getForScript(CustomScript script) {
        return new LuaTable() {{
            set(VANILLA_LEFT_WING, VanillaModelPartCustomization.getTableForPart(VANILLA_LEFT_WING, script));
            set(VANILLA_RIGHT_WING, VanillaModelPartCustomization.getTableForPart(VANILLA_RIGHT_WING, script));
        }};
    }
}
