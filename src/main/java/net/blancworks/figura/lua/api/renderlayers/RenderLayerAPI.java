package net.blancworks.figura.lua.api.renderlayers;

import com.mojang.blaze3d.platform.GlStateManager;
import net.blancworks.figura.lua.CustomScript;
import net.blancworks.figura.lua.api.ReadOnlyLuaTable;
import net.minecraft.util.Identifier;
import org.luaj.vm2.LuaBoolean;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.*;
import org.lwjgl.opengl.GL30;

//aka the NIL api
public class RenderLayerAPI {

    public static Identifier getId() {
        return new Identifier("default", "renderlayers");
    }

    public static ReadOnlyLuaTable getForScript(CustomScript script) {
        return new ReadOnlyLuaTable(new LuaTable() {{
            set("registerShader", new VarArgFunction() {
                @Override
                public Varargs invoke(Varargs args) {
                    return NIL;
                }
            });
            set("registerRenderLayer", new VarArgFunction() {
                @Override
                public Varargs invoke(Varargs args) {
                    return NIL;
                }
            });
            set("setUniform", new ThreeArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1, LuaValue arg2, LuaValue arg3) {
                    return NIL;
                }
            });
            set("setPriority", new TwoArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1, LuaValue arg2) {
                    return NIL;
                }
            });
            set("getPriority", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    return NIL;
                }
            });
            set("isShaderReady", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    return LuaBoolean.FALSE;
                }
            });
            set("useShader", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    return NIL;
                }
            });
            set("setTexture", new TwoArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1, LuaValue arg2) {
                    return NIL;
                }
            });
            set("enableLightmap", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return NIL;
                }
            });
            set("disableLightmap", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return NIL;
                }
            });
            set("enableOverlay", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return NIL;
                }
            });
            set("disableOverlay", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return NIL;
                }
            });
            set("enableCull", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return NIL;
                }
            });
            set("disableCull", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return NIL;
                }
            });
            set("enableDepthTest", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return NIL;
                }
            });
            set("disableDepthTest", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return NIL;
                }
            });
            set("depthFunc", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    return NIL;
                }
            });
            set("depthMask", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    return NIL;
                }
            });
            set("enableBlend", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return NIL;
                }
            });
            set("disableBlend", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return NIL;
                }
            });
            set("blendFunc", new TwoArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1, LuaValue arg2) {
                    return NIL;
                }
            });
            set("blendFuncSeparate", new VarArgFunction() {
                @Override
                public LuaValue invoke(Varargs args) {
                    return NIL;
                }
            });
            set("defaultBlendFunc", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return NIL;
                }
            });
            set("blendEquation", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    return NIL;
                }
            });
            set("enableColorLogicOp", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return NIL;
                }
            });
            set("disableColorLogicOp", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return NIL;
                }
            });
            set("logicOp", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    return NIL;
                }
            });
            set("colorMask", new VarArgFunction() {
                @Override
                public Varargs invoke(Varargs args) {
                    return NIL;
                }
            });
            set("enableStencil", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return NIL;
                }
            });
            set("disableStencil", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return NIL;
                }
            });
            set("stencilMask", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    return NIL;
                }
            });
            set("stencilFunc", new ThreeArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1, LuaValue arg2, LuaValue arg3) {
                    return NIL;
                }
            });
            set("stencilOp", new ThreeArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1, LuaValue arg2, LuaValue arg3) {
                    return NIL;
                }
            });
            set("enableScissors", new VarArgFunction() {
                @Override
                public LuaValue invoke(Varargs args) {
                    return NIL;
                }
            });
            set("disableScissors", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return NIL;
                }
            });
            set("lineWidth", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    return NIL;
                }
            });
            set("restoreDefaults", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return NIL;
                }
            });

            //Constants
            set("GL_NEVER", GL30.GL_NEVER);
            set("GL_LESS", GL30.GL_LESS);
            set("GL_EQUAL", GL30.GL_EQUAL);
            set("GL_LEQUAL",GL30.GL_LEQUAL);
            set("GL_GREATER",GL30.GL_GREATER);
            set("GL_NOTEQUAL",GL30.GL_NOTEQUAL);
            set("GL_GEQUAL",GL30.GL_GEQUAL);
            set("GL_ALWAYS",GL30.GL_ALWAYS);
            for(GlStateManager.SrcFactor entry : GlStateManager.SrcFactor.values())
                set("GL_" + entry.name(), entry.field_22545);
            for(GlStateManager.DstFactor entry : GlStateManager.DstFactor.values())
                set("GL_" + entry.name(), entry.field_22528);
            for(GlStateManager.LogicOp entry : GlStateManager.LogicOp.values())
                set("GL_" + entry.name(), entry.value);
            set("GL_FUNC_ADD", GL30.GL_FUNC_ADD);
            set("GL_FUNC_SUBTRACT", GL30.GL_FUNC_SUBTRACT);
            set("GL_FUNC_REVERSE_SUBTRACT", GL30.GL_FUNC_REVERSE_SUBTRACT);
            set("GL_MAX", GL30.GL_MAX);
            set("GL_MIN", GL30.GL_MIN);
            set("GL_KEEP", GL30.GL_KEEP);
            set("GL_REPLACE", GL30.GL_REPLACE);
            set("GL_INCR", GL30.GL_INCR);
            set("GL_INCR_WRAP", GL30.GL_INCR_WRAP);
            set("GL_DECR", GL30.GL_DECR);
            set("GL_DECR_WRAP", GL30.GL_DECR_WRAP);
            set("GL_INVERT", GL30.GL_INVERT);
        }});
    }
}
