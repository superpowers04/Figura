package net.blancworks.figura.lua.api.model;

import net.blancworks.figura.lua.CustomScript;
import net.minecraft.util.Identifier;
import org.luaj.vm2.LuaTable;

public class ParrotModelAPI {

    public static final String VANILLA_LEFT_PARROT = "LEFT_PARROT";
    public static final String VANILLA_RIGHT_PARROT = "RIGHT_PARROT";

    public static final Identifier VANILLA_LEFT_PARROT_ID = new Identifier("figura", "left_parrot");
    public static final Identifier VANILLA_RIGHT_PARROT_ID = new Identifier("figura", "right_parrot");

    public static Identifier getID() {
        return new Identifier("default", "parrot_model");
    }

    public static LuaTable getForScript(CustomScript script) {
        return new LuaTable() {{
            set(VANILLA_LEFT_PARROT, VanillaModelPartCustomization.getTableForPart(VANILLA_LEFT_PARROT, script));
            set(VANILLA_RIGHT_PARROT, VanillaModelPartCustomization.getTableForPart(VANILLA_RIGHT_PARROT, script));
        }};
    }
}
