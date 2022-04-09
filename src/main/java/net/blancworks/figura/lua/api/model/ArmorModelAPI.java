package net.blancworks.figura.lua.api.model;

import net.blancworks.figura.lua.CustomScript;
import net.minecraft.util.Identifier;
import org.luaj.vm2.LuaTable;

public class ArmorModelAPI {

    public static final String VANILLA_HELMET = "HELMET";
    public static final String VANILLA_CHESTPLATE = "CHESTPLATE";
    public static final String VANILLA_LEGGINGS = "LEGGINGS";
    public static final String VANILLA_BOOTS = "BOOTS";
    public static final String VANILLA_HEAD_ITEM = "HEAD_ITEM";
    
    public static Identifier getID() {
        return new Identifier("default", "armor_model");
    }

    public static LuaTable getForScript(CustomScript script) {
        return new LuaTable() {{
            set(VANILLA_HELMET, VanillaModelPartCustomization.getTableForPart(VANILLA_HELMET, script));

            set(VANILLA_CHESTPLATE, VanillaModelPartCustomization.getTableForPart(VANILLA_CHESTPLATE, script));
            set(VANILLA_LEGGINGS, VanillaModelPartCustomization.getTableForPart(VANILLA_LEGGINGS, script));

            set(VANILLA_BOOTS, VanillaModelPartCustomization.getTableForPart(VANILLA_BOOTS, script));

            set(VANILLA_HEAD_ITEM, VanillaModelPartCustomization.getTableForPart(VANILLA_HEAD_ITEM, script));
        }};
    }
}
