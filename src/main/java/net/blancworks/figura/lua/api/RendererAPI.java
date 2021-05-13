package net.blancworks.figura.lua.api;

import net.blancworks.figura.FiguraMod;
import net.blancworks.figura.PlayerData;
import net.blancworks.figura.PlayerDataManager;
import net.blancworks.figura.lua.CustomScript;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;
import org.luaj.vm2.LuaBoolean;
import org.luaj.vm2.LuaNumber;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;

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
            
            set("isFirstPerson", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    if(PlayerDataManager.localPlayer != script.playerData)
                        return LuaBoolean.FALSE;
                    return MinecraftClient.getInstance().options.getPerspective().isFirstPerson() ? LuaBoolean.TRUE : LuaBoolean.FALSE;
                }
            });
        }});
    }
}
