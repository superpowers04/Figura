package net.blancworks.figura.lua.api;

import net.blancworks.figura.lua.CustomScript;
import net.minecraft.util.Identifier;
import org.luaj.vm2.LuaTable;

public interface FiguraAPI {
    Identifier getID();
    LuaTable getForScript(CustomScript script);
}
