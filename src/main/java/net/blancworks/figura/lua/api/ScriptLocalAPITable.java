package net.blancworks.figura.lua.api;

import net.blancworks.figura.lua.CustomScript;
import org.luaj.vm2.LuaValue;

public class ScriptLocalAPITable extends ReadOnlyLuaTable{
    public CustomScript targetScript;

    public ScriptLocalAPITable(CustomScript script) {
        super();

        targetScript = script;
    }

    public ScriptLocalAPITable(CustomScript script, LuaValue table) {
        super(table);
        
        targetScript = script;
    }
}
