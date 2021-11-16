package net.blancworks.figura.lua.api.network;

import net.blancworks.figura.PlayerDataManager;
import net.blancworks.figura.lua.CustomScript;
import net.blancworks.figura.lua.api.ReadOnlyLuaTable;
import net.minecraft.util.Identifier;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;

@Deprecated
public class NetworkAPI {
    public static Identifier getID() {
        return new Identifier("default", "network");
    }

    public static ReadOnlyLuaTable getForScript(CustomScript script) {
        return new NetworkAPILuaTable(script);
    }
    
    public static class NetworkAPILuaTable extends ReadOnlyLuaTable {
        public CustomScript targetScript;
        
        public NetworkAPILuaTable(CustomScript script) {
            this.targetScript = script;

            LuaTable table = new LuaTable();
            
            table.set("registerPing", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    String s = arg.checkjstring();
                    
                    targetScript.registerPingName(s);
                    
                    return NIL;
                }
            });
            
            table.set("ping", new TwoArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1, LuaValue arg2) {
                    //Only allow sending pings on local player avatar.
                    if(targetScript.playerData != PlayerDataManager.localPlayer)
                        return NIL;

                    try {
                        String s = arg1.checkjstring();

                        //Get ID from string.
                        short id = targetScript.functionIDMap.inverse().get(s);

                        //Script handles local ping immediately.
                        targetScript.handlePing(id, arg2);

                        CustomScript.LuaPing lp = new CustomScript.LuaPing();
                        lp.args = arg2;
                        lp.functionID = id;

                        if (!targetScript.playerData.isLocalAvatar)
                            targetScript.outgoingPingQueue.add(lp);
                    } catch (Exception ignored) {
                        throw new LuaError("Failed to send ping! Make sure the ping is registered before sending it!");
                    }

                    return NIL;
                }
            });
            
            super.setTable(table);
        }
    }
}
