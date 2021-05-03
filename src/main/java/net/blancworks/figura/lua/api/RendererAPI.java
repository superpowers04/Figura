package net.blancworks.figura.lua.api;

import net.blancworks.figura.lua.CustomScript;
import net.minecraft.util.Identifier;
import org.luaj.vm2.LuaNumber;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;

public class RendererAPI {

    public static Identifier getID() {
        return new Identifier("default", "renderer");
    }

    public static ReadOnlyLuaTable getForScript(CustomScript script) {
        return new ReadOnlyLuaTable(new LuaTable(){{
            set("setShadowSize", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    if(arg == NIL)
                        script.customShadowSize = null;
                    script.customShadowSize = arg.checknumber().tofloat();
                    return NIL;
                }
            });
            
            set("getShadowSize", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    if(script.customShadowSize == null)
                        return NIL;
                    return LuaNumber.valueOf(script.customShadowSize);
                }
            });
        }});
    }
}
