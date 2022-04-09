package net.blancworks.figura.lua.api.model;

import net.blancworks.figura.lua.CustomScript;
import net.minecraft.util.Identifier;
import org.luaj.vm2.LuaTable;

public class ItemModelAPI {

    public static final String VANILLA_LEFT_HAND = "LEFT_HAND";
    public static final String VANILLA_RIGHT_HAND = "RIGHT_HAND";

    public static final Identifier VANILLA_LEFT_HAND_ID = new Identifier("figura", "left_hand");
    public static final Identifier VANILLA_RIGHT_HAND_ID = new Identifier("figura", "right_hand");


    public static Identifier getID() {
        return new Identifier("default", "held_item_model");
    }

    public static LuaTable getForScript(CustomScript script) {
        return new LuaTable() {{
            set(VANILLA_LEFT_HAND, VanillaModelPartCustomization.getTableForPart(VANILLA_LEFT_HAND, script));
            set(VANILLA_RIGHT_HAND, VanillaModelPartCustomization.getTableForPart(VANILLA_RIGHT_HAND, script));
        }};
    }
}
