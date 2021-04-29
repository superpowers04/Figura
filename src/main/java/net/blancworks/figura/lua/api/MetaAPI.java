package net.blancworks.figura.lua.api;

import net.blancworks.figura.lua.CustomScript;
import net.blancworks.figura.trust.PlayerTrustManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.client.util.math.Vector4f;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.world.World;
import org.luaj.vm2.LuaBoolean;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.ThreeArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;

import java.util.ArrayList;

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
        }});
    
        return producedTable;
    }
}
