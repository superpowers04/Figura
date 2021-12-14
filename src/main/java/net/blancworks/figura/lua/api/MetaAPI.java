package net.blancworks.figura.lua.api;

import net.blancworks.figura.FiguraMod;
import net.blancworks.figura.avatar.AvatarData;
import net.blancworks.figura.lua.CustomScript;
import net.blancworks.figura.network.NewFiguraNetworkManager;
import net.blancworks.figura.trust.TrustContainer;
import net.minecraft.util.Identifier;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.ZeroArgFunction;

public class MetaAPI {
    public static Identifier getID() {
        return new Identifier("default", "meta");
    }

    public static ReadOnlyLuaTable getForScript(CustomScript script) {
        return new ScriptLocalAPITable(script, new LuaTable() {{
            set("getInitLimit", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(script.avatarData.getTrustContainer().getTrust(TrustContainer.Trust.INIT_INST));
                }
            });

            set("getTickLimit", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(script.avatarData.getTrustContainer().getTrust(TrustContainer.Trust.TICK_INST));
                }
            });

            set("getRenderLimit", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(script.avatarData.getTrustContainer().getTrust(TrustContainer.Trust.RENDER_INST));
                }
            });

            set("getCanModifyVanilla", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(script.avatarData.getTrustContainer().getTrust(TrustContainer.Trust.VANILLA_MODEL_EDIT) == 1);
                }
            });

            set("getComplexityLimit", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(script.avatarData.getTrustContainer().getTrust(TrustContainer.Trust.COMPLEXITY));
                }
            });

            set("getParticleLimit", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(script.avatarData.getTrustContainer().getTrust(TrustContainer.Trust.PARTICLES));
                }
            });

            set("getSoundLimit", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(script.avatarData.getTrustContainer().getTrust(TrustContainer.Trust.SOUNDS));
                }
            });

            set("getDoesRenderOffscreen", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(script.avatarData.getTrustContainer().getTrust(TrustContainer.Trust.OFFSCREEN_RENDERING) == 1);
                }
            });

            set("getCanModifyNameplate", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(script.avatarData.getTrustContainer().getTrust(TrustContainer.Trust.NAMEPLATE_EDIT) == 1);
                }
            });

            set("getCanHaveCustomRenderLayer", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(script.avatarData.getTrustContainer().getTrust(TrustContainer.Trust.CUSTOM_RENDER_LAYER) == 1);
                }
            });

            set("getCanHaveCustomSounds", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(script.avatarData.getTrustContainer().getTrust(TrustContainer.Trust.CUSTOM_SOUNDS) == 1);
                }
            });


            set("getCurrentInitCount", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(script.initInstructionCount);
                }
            });

            set("getCurrentTickCount", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(script.tickInstructionCount);
                }
            });

            set("getCurrentRenderCount", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(script.renderInstructionCount);
                }
            });

            set("getCurrentComplexity", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(script.avatarData.getComplexity());
                }
            });

            set("getCurrentParticleCount", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(script.particleSpawnCount);
                }
            });

            set("getCurrentSoundCount", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(script.soundSpawnCount);
                }
            });


            set("getFiguraVersion", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(FiguraMod.MOD_VERSION);
                }
            });


            set("getModelStatus", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    if (script.avatarData.model == null)
                        return LuaValue.valueOf(1);

                    int ret;
                    long fileSize = script.avatarData.getFileSize();

                    if (fileSize >= AvatarData.FILESIZE_LARGE_THRESHOLD)
                        ret = 2;
                    else if (fileSize >= AvatarData.FILESIZE_WARNING_THRESHOLD)
                        ret = 3;
                    else
                        ret = 4;

                    return LuaValue.valueOf(ret);
                }
            });

            set("getScriptStatus", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(script.scriptError ? 2 : 4);
                }
            });

            set("getTextureStatus", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(script.avatarData.texture != null ? 4 : 1);
                }
            });

            set("getBackendStatus", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(NewFiguraNetworkManager.connectionStatus + 1);
                }
            });
        }});
    }
}
