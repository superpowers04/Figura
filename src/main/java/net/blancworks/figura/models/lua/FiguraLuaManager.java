package net.blancworks.figura.models.lua;

import net.blancworks.figura.PlayerData;
import org.luaj.vm2.*;
import org.luaj.vm2.compiler.LuaC;
import org.luaj.vm2.lib.PackageLib;
import org.luaj.vm2.lib.StringLib;
import org.luaj.vm2.lib.jse.JseBaseLib;
import org.luaj.vm2.lib.jse.JseMathLib;

public class FiguraLuaManager {
    
    //The globals for the entire lua system.
    public static Globals modGlobals;
    
    public static void initialize(){
        modGlobals = new Globals();
        modGlobals.load(new JseBaseLib());
        modGlobals.load(new PackageLib());
        modGlobals.load(new StringLib());
        modGlobals.load(new JseMathLib());

        LoadState.install(modGlobals);
        LuaC.install(modGlobals);

        LuaString.s_metatable = new ReadOnlyLuaTable(LuaString.s_metatable);
    }
    
    public static void loadScript(PlayerData data, String content){
        CustomScript newScript = new CustomScript(data, content);
        data.script = newScript;
    }
    
    static class ReadOnlyLuaTable extends LuaTable {
        public ReadOnlyLuaTable(LuaValue table) {
            presize(table.length(), 0);
            for (Varargs n = table.next(LuaValue.NIL); !n.arg1().isnil(); n = table
                    .next(n.arg1())) {
                LuaValue key = n.arg1();
                LuaValue value = n.arg(2);
                super.rawset(key, value.istable() ? new ReadOnlyLuaTable(value) : value);
            }
        }
        public LuaValue setmetatable(LuaValue metatable) { return error("table is read-only"); }
        public void set(int key, LuaValue value) { error("table is read-only"); }
        public void rawset(int key, LuaValue value) { error("table is read-only"); }
        public void rawset(LuaValue key, LuaValue value) { error("table is read-only"); }
        public LuaValue remove(int pos) { return error("table is read-only"); }
    }
}
