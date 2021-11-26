package net.blancworks.figura.lua.api.client;

import net.blancworks.figura.PlayerDataManager;
import net.blancworks.figura.lua.CustomScript;
import net.blancworks.figura.lua.api.ReadOnlyLuaTable;
import net.blancworks.figura.lua.api.math.LuaVector;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec2f;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;

public class ClientAPI {

    public static Identifier getID() {
        return new Identifier("default", "client");
    }

    public static ReadOnlyLuaTable getForScript(CustomScript script) {
        MinecraftClient client = MinecraftClient.getInstance();

        return new ReadOnlyLuaTable(new LuaTable() {{
            set("getOpenScreen", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    //always return nil when not the local player
                    if (client.currentScreen == null)
                        return NIL;

                    //get the current screen
                    String screenTitle = client.currentScreen.getTitle().getString();
                    if (screenTitle.equals(""))
                        screenTitle = client.currentScreen.getClass().getSimpleName();

                    return LuaValue.valueOf(screenTitle);
                }
            });

            set("getFPS", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(client.fpsDebugString);
                }
            });

            set("isPaused", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(client.isPaused());
                }
            });

            set("getVersion", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(SharedConstants.getGameVersion().getName());
                }
            });

            set("getVersionType", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(client.getVersionType());
                }
            });

            set("getServerBrand", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(client.getServer() == null ? client.player.getServerBrand() : "Integrated");
                }
            });

            set("getChunksCount", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(client.worldRenderer.getChunksDebugString());
                }
            });

            set("getEntityCount", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(client.worldRenderer.getEntitiesDebugString());
                }
            });

            set("getParticleCount", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(client.particleManager.getDebugString());
                }
            });

            set("getSoundCount", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(client.getSoundManager().getDebugString());
                }
            });

            set("getActiveShader", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    if (client.gameRenderer.getShader() == null)
                        return NIL;

                    return LuaValue.valueOf(client.gameRenderer.getShader().getName());
                }
            });

            set("getJavaVersion", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(System.getProperty("java.version"));
                }
            });

            set("getMemoryInUse", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
                }
            });

            set("getMaxMemory", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(Runtime.getRuntime().maxMemory());
                }
            });

            set("getAllocatedMemory", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(Runtime.getRuntime().totalMemory());
                }
            });

            set("isWindowFocused", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(client.isWindowFocused());
                }
            });

            set("isHudEnabled", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(MinecraftClient.isHudEnabled());
                }
            });

            set("getWindowSize", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaVector.of(new Vec2f(MinecraftClient.getInstance().getWindow().getWidth(), MinecraftClient.getInstance().getWindow().getHeight()));
                }
            });

            set("getGUIScale", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(MinecraftClient.getInstance().options.guiScale);
                }
            });

            set("getFov", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(MinecraftClient.getInstance().options.fov);
                }
            });

            set("setCrosshairPos", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    script.crossHairPos = LuaVector.checkOrNew(arg).asV2f();
                    return NIL;
                }
            });

            set("getCrosshairPos", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaVector.of(script.crossHairPos);
                }
            });

            set("setCrosshairEnabled", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    script.crossHairEnabled = arg.checkboolean();
                    return NIL;
                }
            });

            set("getCrosshairEnabled", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(script.crossHairEnabled);
                }
            });

            set("isHost", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(script.playerData == PlayerDataManager.localPlayer);
                }
            });

            set("getSystemTime", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(System.currentTimeMillis());
                }
            });

        }});
    }
}
