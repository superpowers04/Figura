package net.blancworks.figura.lua.api;

import net.blancworks.figura.lua.CustomScript;
import net.blancworks.figura.trust.PlayerTrustManager;
import net.minecraft.util.Identifier;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.ZeroArgFunction;

public class MetaAPI {
    public static Identifier getID() {
        return new Identifier("default", "meta");
    }

    public static ReadOnlyLuaTable getForScript(CustomScript script) {
        ScriptLocalAPITable producedTable = new ScriptLocalAPITable(script, new LuaTable() {{
            set("getInitLimit", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(script.playerData.getTrustContainer().getIntSetting(PlayerTrustManager.MAX_INIT_ID));
                }
            });
            
            set("getTickLimit", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(script.playerData.getTrustContainer().getIntSetting(PlayerTrustManager.MAX_TICK_ID));
                }
            });
    
            set("getRenderLimit", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(script.playerData.getTrustContainer().getIntSetting(PlayerTrustManager.MAX_RENDER_ID));
                }
            });
    
            set("getCanModifyVanilla", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(script.playerData.getTrustContainer().getBoolSetting(PlayerTrustManager.ALLOW_VANILLA_MOD_ID));
                }
            });
    
            set("getComplexityLimit", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(script.playerData.getTrustContainer().getIntSetting(PlayerTrustManager.MAX_COMPLEXITY_ID));
                }
            });
    
            set("getParticleLimit", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(script.playerData.getTrustContainer().getIntSetting(PlayerTrustManager.MAX_PARTICLES_ID));
                }
            });
            
            set("getSoundLimit", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(script.playerData.getTrustContainer().getIntSetting(PlayerTrustManager.MAX_SOUND_EFFECTS_ID));
                }
            });

            set("getDoesRenderOffscreen", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(script.playerData.getTrustContainer().getBoolSetting(PlayerTrustManager.ALLOW_OFFSCREEN_RENDERING));
                }
            });

            set("getCanModifyNameplate", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(script.playerData.getTrustContainer().getBoolSetting(PlayerTrustManager.ALLOW_NAMEPLATE_MOD_ID));
                }
            });
        }});
    
        return producedTable;
    }
}
