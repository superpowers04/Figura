package net.blancworks.figura.lua.api.network;

import net.blancworks.figura.avatar.AvatarDataManager;
import net.blancworks.figura.lua.CustomScript;
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

    public static LuaTable getForScript(CustomScript targetScript) {
        return new LuaTable() {{
            set("registerPing", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    String s = arg.checkjstring();

                    targetScript.registerPingName(s);

                    return NIL;
                }
            });

            set("ping", new TwoArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1, LuaValue arg2) {
                    //Only allow sending pings on local player avatar.
                    if(targetScript.avatarData != AvatarDataManager.localPlayer)
                        return NIL;

                    try {
                        String s = arg1.checkjstring();

                        //Get ID from string.
                        short id = targetScript.oldFunctionIDMap.inverse().get(s);

                        //Script handles local ping immediately.
                        CustomScript.LuaPing ping = targetScript.handlePing(id, arg2, null);

                        if (ping != null && !targetScript.avatarData.isLocalAvatar)
                            targetScript.outgoingPingQueue.add(ping);
                    } catch (Exception e) {
                        e.printStackTrace();
                        throw new LuaError("Failed to send ping! Make sure the ping is registered before sending it!");
                    }

                    return NIL;
                }
            });
        }};
    }
}
