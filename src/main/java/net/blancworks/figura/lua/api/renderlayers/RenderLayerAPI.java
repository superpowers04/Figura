package net.blancworks.figura.lua.api.renderlayers;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.blancworks.figura.lua.CustomScript;
import net.blancworks.figura.lua.api.ReadOnlyLuaTable;
import net.blancworks.figura.models.FiguraTexture;
import net.blancworks.figura.models.shaders.FiguraRenderLayer;
import net.blancworks.figura.models.shaders.FiguraShader;
import net.blancworks.figura.models.shaders.FiguraVertexConsumerProvider;
import net.blancworks.figura.trust.TrustContainer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.WindowFramebuffer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Style;
import net.minecraft.text.TextColor;
import net.minecraft.util.Identifier;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.*;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class RenderLayerAPI {

    public static boolean canCallGLFunctions = false;

    public static Identifier getId() {
        return new Identifier("default", "renderlayers");
    }

    public static ReadOnlyLuaTable getForScript(CustomScript script) {
        return new ReadOnlyLuaTable(new LuaTable() {{
            set("registerShader", new VarArgFunction() {
                @Override
                public Varargs invoke(Varargs args) {
                    if (!script.avatarData.canRenderCustomLayers())
                        return NIL;
                    if (script.shaders.size() >= CustomScript.maxShaders)
                        throw new LuaError("You've registered too many (" + CustomScript.maxShaders + ") shaders. Ignoring further ones.");
                    String name = args.checkjstring(1);

                    VertexFormat vertexFormat;
                    if (args.isnil(2)) {
                        vertexFormat = VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL;
                    } else {
                        String formatString = args.checkjstring(2);
                        if (!vertexFormatMap.containsKey(formatString))
                            throw new LuaError("Invalid vertex format: " + formatString);
                        vertexFormat = vertexFormatMap.get(formatString);
                    }

                    String vertexSource = args.checkjstring(3);
                    String fragmentSource = args.checkjstring(4);
                    int numSamplers = args.checkint(5);

                    ArrayList<String> uniformNames = new ArrayList<>();
                    ArrayList<String> uniformTypes = new ArrayList<>();
                    if (!args.isnil(6)) {
                        LuaTable customUniforms = args.checktable(6);
                        LuaValue k = LuaValue.NIL;
                        while ( true ) {
                            Varargs n = customUniforms.next(k);
                            if ( (k = n.arg1()).isnil() )
                                break;
                            LuaValue v = n.arg(2);
                            uniformNames.add(k.checkjstring());
                            uniformTypes.add(v.checkjstring());
                        }
                    }

                    RenderSystem.recordRenderCall(()->{
                        try {
                            FiguraShader newShader = FiguraShader.create(vertexFormat, vertexSource, fragmentSource, numSamplers, uniformNames, uniformTypes);
                            script.shaders.put(name, newShader);
                        } catch (IOException e) {
                            if (script.avatarData.isLocalAvatar)
                                CustomScript.sendChatMessage(new LiteralText(e.getMessage()).setStyle(Style.EMPTY.withColor(TextColor.parse("red"))));
                            e.printStackTrace();
                        } catch (Exception e) {
                            script.handleError(e);
                        }
                    });
                    return NIL;
                }
            });
            set("registerRenderLayer", new VarArgFunction() {
                @Override
                public Varargs invoke(Varargs args) {
                    if (!script.avatarData.canRenderCustomLayers())
                        return NIL;
                    if (script.customVCP != null && !script.customVCP.canAddLayer())
                        throw new LuaError("Cannot add anymore render layers, already reached cap (" + script.customVCP.maxSize + ").");
                    String name = args.checkjstring(1);
                    LuaTable params = args.checktable(2);
                    LuaFunction startFunc = args.checkfunction(3);
                    LuaFunction endFunc = args.checkfunction(4);

                    //Process the params
                    VertexFormat vertexFormat = luaGetOrDefault(params, LuaString.valueOf("vertexFormat"), VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, val->vertexFormatMap.get(val.checkjstring()));
                    VertexFormat.DrawMode drawMode = luaGetOrDefault(params, LuaString.valueOf("drawMode"), VertexFormat.DrawMode.QUADS, val->drawModeMap.get(val.checkjstring()));
                    int expectedBufferSize = luaGetOrDefault(params, LuaString.valueOf("expectedBufferSize"), 256, LuaValue::checkint);
                    boolean hasCrumbling = luaGetOrDefault(params, LuaString.valueOf("hasCrumbling"), true, LuaValue::checkboolean);
                    boolean translucent = luaGetOrDefault(params, LuaString.valueOf("translucent"), true, LuaValue::checkboolean);
                    if (vertexFormat == null)
                        throw new LuaError("Invalid vertex format!");
                    if (drawMode == null)
                        throw new LuaError("Invalid draw mode!");
                    Runnable preDraw = () -> {
                        canCallGLFunctions = true;
                        script.setInstructionLimitPermission(TrustContainer.Trust.RENDER_INST, script.renderInstructionCount);
                        try {
                            startFunc.call();
                        } catch (Exception err) {
                            script.handleError(err);
                            if (script.customVCP != null)
                                if (script.customVCP.getLayer(name) != null) //These shouldn't ever be null, putting this here just in case
                                    script.customVCP.getLayer(name).disabled = true;
                        }
                        canCallGLFunctions = false;
                    };
                    Runnable postDraw = () -> {
                        canCallGLFunctions = true;
                        try {
                            endFunc.call();
                        } catch (Exception err) {
                            script.handleError(err);
                            if (script.customVCP != null)
                                if (script.customVCP.getLayer(name) != null)
                                    script.customVCP.getLayer(name).disabled = true;
                        }
                        script.renderInstructionCount += script.scriptGlobals.running.state.bytecodes;
                        canCallGLFunctions = false;
                    };
                    FiguraRenderLayer newLayer = new FiguraRenderLayer(name, vertexFormat, drawMode, expectedBufferSize, hasCrumbling, translucent, preDraw, postDraw);

                    if (script.customVCP == null)
                        script.customVCP = new FiguraVertexConsumerProvider();

                    if (script.customVCP.canAddLayer())
                        script.customVCP.addLayer(newLayer);

                    return NIL;
                }
            });
            set("setUniform", new ThreeArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1, LuaValue arg2, LuaValue arg3) {
                    if (!RenderSystem.isOnRenderThread())
                        throw new LuaError("setUniform can only be called inside render() or a renderlayer function!");
                    FiguraShader shader = script.shaders.get(arg1.checkjstring());
                    if (shader != null) {
                        if (shader.hasUniform(arg2.checkjstring())) {
                            arg3.checknotnil();
                            shader.setUniformFromLua(arg2, arg3);
                        } else {
                            throw new LuaError("No uniform with name: " + arg2.checkjstring());
                        }
                    }
                    return NIL;
                }
            });
            set("setPriority", new TwoArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1, LuaValue arg2) {
                    if (script.customVCP != null) {
                        FiguraRenderLayer layer = script.customVCP.getLayer(arg1.checkjstring());
                        script.customVCP.setPriority(layer, arg2.checkint());
                    }
                    return NIL;
                }
            });
            set("getPriority", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    if (script.customVCP != null) {
                        FiguraRenderLayer layer = script.customVCP.getLayer(arg.checkjstring());
                        return LuaNumber.valueOf(layer.priority);
                    }
                    return NIL;
                }
            });
            set("isShaderReady", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    return LuaBoolean.valueOf(script.shaders.containsKey(arg.checkjstring()));
                }
            });
            set("useShader", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    checkValidCall();
                    FiguraShader shader = script.shaders.get(arg.checkjstring());
                    if(shader != null)
                        RenderSystem.setShader(()->shader);
                    return NIL;
                }
            });
            set("setTexture", new TwoArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1, LuaValue arg2) {
                    checkValidCall();
                    int loc = arg1.checkint();
                    checkInRange(loc, 0, 7, "Invalid sampler index " + loc + ", must be 0-7.");
                    String textureStr = arg2.checkjstring();
                    RenderSystem.enableTexture();
                    switch (textureStr) {
                        case "MY_TEXTURE" -> RenderSystem.setShaderTexture(loc, script.avatarData.texture.id);
                        case "MY_TEXTURE_EMISSIVE" -> {
                            List<FiguraTexture> textureList = script.avatarData.extraTextures;
                            if (textureList.size() > 0) {
                                RenderSystem.setShaderTexture(loc, textureList.get(0).id);
                            } else {
                                throw new LuaError("Emissive texture doesn't exist!");
                            }
                        }
                        case "MAIN_FRAMEBUFFER" -> {
                            blitMainFramebuffer(mainFramebufferCopy);
                            RenderSystem.setShaderTexture(loc, mainFramebufferCopy.getColorAttachment());
                        }
                        case "LAST_FRAMEBUFFER" -> {
                            FiguraVertexConsumerProvider.isUsingLastFramebuffer = true;
                            RenderSystem.setShaderTexture(loc, lastFramebufferCopy.getColorAttachment());
                        }
                        default -> {
                            checkValidId(textureStr, "Invalid texture name: ");
                            Identifier id = new Identifier(textureStr);
                            if (MinecraftClient.getInstance().getTextureManager().getOrDefault(id, null) == null)
                                throw new LuaError("Texture " + textureStr + " does not exist.");
                            RenderSystem.setShaderTexture(loc, id);
                        }
                    }

                    return NIL;
                }
            });
            set("enableLightmap", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    checkValidCall();
                    MinecraftClient.getInstance().gameRenderer.getLightmapTextureManager().enable();
                    return NIL;
                }
            });
            set("disableLightmap", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    checkValidCall();
                    MinecraftClient.getInstance().gameRenderer.getLightmapTextureManager().disable();
                    return NIL;
                }
            });
            set("enableOverlay", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    checkValidCall();
                    MinecraftClient.getInstance().gameRenderer.getOverlayTexture().setupOverlayColor();
                    return NIL;
                }
            });
            set("disableOverlay", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    checkValidCall();
                    MinecraftClient.getInstance().gameRenderer.getOverlayTexture().teardownOverlayColor();
                    return NIL;
                }
            });
            set("enableCull", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    checkValidCall();
                    RenderSystem.enableCull();
                    return NIL;
                }
            });
            set("disableCull", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    checkValidCall();
                    RenderSystem.disableCull();
                    return NIL;
                }
            });
            set("enableDepthTest", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    checkValidCall();
                    RenderSystem.enableDepthTest();
                    return NIL;
                }
            });
            set("disableDepthTest", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    checkValidCall();
                    RenderSystem.disableDepthTest();
                    return NIL;
                }
            });
            set("depthFunc", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    checkValidCall();
                    int i = arg.checkint();
                    checkInRange(i, 512, 519, "Invalid depth flag: " + i);
                    RenderSystem.depthFunc(i);
                    return NIL;
                }
            });
            set("depthMask", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    checkValidCall();
                    boolean b = arg.checkboolean();
                    RenderSystem.depthMask(b);
                    return NIL;
                }
            });
            set("enableBlend", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    checkValidCall();
                    RenderSystem.enableBlend();
                    return NIL;
                }
            });
            set("disableBlend", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    checkValidCall();
                    RenderSystem.disableBlend();
                    return NIL;
                }
            });
            set("blendFunc", new TwoArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1, LuaValue arg2) {
                    checkValidCall();
                    int src = arg1.checkint();
                    int dst = arg2.checkint();
                    checkArg(src, "Invalid Source Factor: " + src, GL30.GL_ZERO, GL30.GL_ONE, GL30.GL_SRC_COLOR,
                            GL30.GL_ONE_MINUS_SRC_COLOR, GL30.GL_DST_COLOR, GL30.GL_ONE_MINUS_DST_COLOR, GL30.GL_SRC_ALPHA,
                            GL30.GL_ONE_MINUS_SRC_ALPHA, GL30.GL_DST_ALPHA, GL30.GL_ONE_MINUS_DST_ALPHA, GL30.GL_CONSTANT_COLOR,
                            GL30.GL_ONE_MINUS_CONSTANT_COLOR, GL30.GL_CONSTANT_ALPHA, GL30.GL_ONE_MINUS_CONSTANT_ALPHA);
                    checkArg(dst, "Invalid Dest Factor: " + dst, GL30.GL_ZERO, GL30.GL_ONE, GL30.GL_SRC_COLOR,
                            GL30.GL_ONE_MINUS_SRC_COLOR, GL30.GL_DST_COLOR, GL30.GL_ONE_MINUS_DST_COLOR, GL30.GL_SRC_ALPHA,
                            GL30.GL_ONE_MINUS_SRC_ALPHA, GL30.GL_DST_ALPHA, GL30.GL_ONE_MINUS_DST_ALPHA, GL30.GL_CONSTANT_COLOR,
                            GL30.GL_ONE_MINUS_CONSTANT_COLOR, GL30.GL_CONSTANT_ALPHA, GL30.GL_ONE_MINUS_CONSTANT_ALPHA);
                    RenderSystem.blendFunc(src, dst);
                    return NIL;
                }
            });
            set("blendFuncSeparate", new VarArgFunction() {
                @Override
                public LuaValue invoke(Varargs args) {
                    checkValidCall();
                    int srcRGB = args.checkint(1);
                    int dstRGB = args.checkint(2);
                    int srcAlpha = args.checkint(3);
                    int dstAlpha = args.checkint(4);
                    //I'm so sorry
                    checkArg(srcRGB, "Invalid Source RGB Factor: " + srcRGB, GL30.GL_ZERO, GL30.GL_ONE, GL30.GL_SRC_COLOR,
                            GL30.GL_ONE_MINUS_SRC_COLOR, GL30.GL_DST_COLOR, GL30.GL_ONE_MINUS_DST_COLOR, GL30.GL_SRC_ALPHA,
                            GL30.GL_ONE_MINUS_SRC_ALPHA, GL30.GL_DST_ALPHA, GL30.GL_ONE_MINUS_DST_ALPHA, GL30.GL_CONSTANT_COLOR,
                            GL30.GL_ONE_MINUS_CONSTANT_COLOR, GL30.GL_CONSTANT_ALPHA, GL30.GL_ONE_MINUS_CONSTANT_ALPHA);
                    checkArg(dstRGB, "Invalid Dest RGB Factor: " + dstRGB, GL30.GL_ZERO, GL30.GL_ONE, GL30.GL_SRC_COLOR,
                            GL30.GL_ONE_MINUS_SRC_COLOR, GL30.GL_DST_COLOR, GL30.GL_ONE_MINUS_DST_COLOR, GL30.GL_SRC_ALPHA,
                            GL30.GL_ONE_MINUS_SRC_ALPHA, GL30.GL_DST_ALPHA, GL30.GL_ONE_MINUS_DST_ALPHA, GL30.GL_CONSTANT_COLOR,
                            GL30.GL_ONE_MINUS_CONSTANT_COLOR, GL30.GL_CONSTANT_ALPHA, GL30.GL_ONE_MINUS_CONSTANT_ALPHA);
                    checkArg(srcAlpha, "Invalid Source Alpha: " + srcAlpha, GL30.GL_ZERO, GL30.GL_ONE, GL30.GL_SRC_COLOR,
                            GL30.GL_ONE_MINUS_SRC_COLOR, GL30.GL_DST_COLOR, GL30.GL_ONE_MINUS_DST_COLOR, GL30.GL_SRC_ALPHA,
                            GL30.GL_ONE_MINUS_SRC_ALPHA, GL30.GL_DST_ALPHA, GL30.GL_ONE_MINUS_DST_ALPHA, GL30.GL_CONSTANT_COLOR,
                            GL30.GL_ONE_MINUS_CONSTANT_COLOR, GL30.GL_CONSTANT_ALPHA, GL30.GL_ONE_MINUS_CONSTANT_ALPHA);
                    checkArg(dstAlpha, "Invalid Dest Alpha Factor: " + dstAlpha, GL30.GL_ZERO, GL30.GL_ONE, GL30.GL_SRC_COLOR,
                            GL30.GL_ONE_MINUS_SRC_COLOR, GL30.GL_DST_COLOR, GL30.GL_ONE_MINUS_DST_COLOR, GL30.GL_SRC_ALPHA,
                            GL30.GL_ONE_MINUS_SRC_ALPHA, GL30.GL_DST_ALPHA, GL30.GL_ONE_MINUS_DST_ALPHA, GL30.GL_CONSTANT_COLOR,
                            GL30.GL_ONE_MINUS_CONSTANT_COLOR, GL30.GL_CONSTANT_ALPHA, GL30.GL_ONE_MINUS_CONSTANT_ALPHA);
                    RenderSystem.blendFuncSeparate(srcRGB, dstRGB, srcAlpha, dstAlpha);
                    return NIL;
                }
            });
            set("defaultBlendFunc", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    checkValidCall();
                    RenderSystem.defaultBlendFunc();
                    return NIL;
                }
            });
            set("blendEquation", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    checkValidCall();
                    int eq = arg.checkint();
                    checkArg(eq, "Invalid Blend Equation: " + eq, GL30.GL_FUNC_ADD, GL30.GL_FUNC_SUBTRACT,
                            GL30.GL_FUNC_REVERSE_SUBTRACT, GL30.GL_MAX, GL30.GL_MIN);
                    RenderSystem.blendEquation(arg.checkint());
                    return NIL;
                }
            });
            set("enableColorLogicOp", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    checkValidCall();
                    RenderSystem.enableColorLogicOp();
                    return NIL;
                }
            });
            set("disableColorLogicOp", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    checkValidCall();
                    RenderSystem.disableColorLogicOp();
                    return NIL;
                }
            });
            set("logicOp", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    checkValidCall();
                    int op = arg.checkint();
                    checkInRange(op, GL30.GL_CLEAR, GL30.GL_SET, "Invalid Logic Op: " + op);
                    GlStateManager._logicOp(op);
                    return NIL;
                }
            });
            set("colorMask", new VarArgFunction() {
                @Override
                public Varargs invoke(Varargs args) {
                    checkValidCall();
                    RenderSystem.colorMask(args.checkboolean(1), args.checkboolean(2), args.checkboolean(3), args.checkboolean(4));
                    return NIL;
                }
            });
            set("enableStencil", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    checkValidCall();
                    GL11.glEnable(GL30.GL_STENCIL_TEST);
                    return NIL;
                }
            });
            set("disableStencil", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    checkValidCall();
                    GL11.glDisable(GL30.GL_STENCIL_TEST);
                    return NIL;
                }
            });
            set("stencilMask", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    checkValidCall();
                    int mask = arg.checkint();
                    checkInRange(mask, 0, 255, "Invalid stencil mask value: " + mask);
                    RenderSystem.stencilMask(mask);
                    return NIL;
                }
            });
            set("stencilFunc", new ThreeArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1, LuaValue arg2, LuaValue arg3) {
                    checkValidCall();
                    int func = arg1.checkint();
                    int ref = arg2.checkint();
                    int mask = arg3.checkint();
                    checkInRange(func, GL30.GL_NEVER, GL30.GL_ALWAYS, "Invalid Function: " + func);
                    checkInRange(ref, 0, 255, "Invalid Reference Value: " + ref);
                    checkInRange(mask, 0, 255, "Invalid Mask Value: " + mask);
                    RenderSystem.stencilFunc(arg1.checkint(), arg2.checkint(), arg3.checkint());
                    return NIL;
                }
            });
            set("stencilOp", new ThreeArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1, LuaValue arg2, LuaValue arg3) {
                    checkValidCall();
                    int sfail = arg1.checkint();
                    int dpfail = arg2.checkint();
                    int dppass = arg3.checkint();
                    checkArg(sfail, "Invalid Stencil Fail Op: " + sfail, GL30.GL_KEEP, GL30.GL_ZERO,
                            GL30.GL_REPLACE, GL30.GL_INCR, GL30.GL_INCR_WRAP, GL30.GL_DECR, GL30.GL_DECR_WRAP, GL30.GL_INVERT);
                    checkArg(dpfail, "Invalid Depth Fail Op: " + dpfail, GL30.GL_KEEP, GL30.GL_ZERO,
                            GL30.GL_REPLACE, GL30.GL_INCR, GL30.GL_INCR_WRAP, GL30.GL_DECR, GL30.GL_DECR_WRAP, GL30.GL_INVERT);
                    checkArg(dppass, "Invalid Depth Pass Op: " + dppass, GL30.GL_KEEP, GL30.GL_ZERO,
                            GL30.GL_REPLACE, GL30.GL_INCR, GL30.GL_INCR_WRAP, GL30.GL_DECR, GL30.GL_DECR_WRAP, GL30.GL_INVERT);
                    RenderSystem.stencilOp(sfail, dpfail, dppass);
                    return NIL;
                }
            });
            set("enableScissors", new VarArgFunction() {
                @Override
                public LuaValue invoke(Varargs args) {
                    checkValidCall();
                    int x = args.checkint(1);
                    int y = args.checkint(2);
                    int w = args.checkint(3);
                    int h = args.checkint(4);
                    checkInRange(w, 0, Integer.MAX_VALUE, "Negative width not allowed");
                    checkInRange(h, 0, Integer.MAX_VALUE, "Negative height not allowed");
                    RenderSystem.enableScissor(x, y, w, h);
                    return NIL;
                }
            });
            set("disableScissors", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    checkValidCall();
                    RenderSystem.disableScissor();
                    return NIL;
                }
            });
            set("lineWidth", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    checkValidCall();
                    RenderSystem.lineWidth(arg.tofloat());
                    return NIL;
                }
            });
            set("restoreDefaults", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    checkValidCall();
                    restoreDefaults();
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
                set("GL_"+entry.name(),entry.value);
            for(GlStateManager.DstFactor entry : GlStateManager.DstFactor.values())
                set("GL_"+entry.name(),entry.value);
            for(GlStateManager.LogicOp entry : GlStateManager.LogicOp.values())
                set("GL_"+entry.name(),entry.value);
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

    public static void restoreDefaults() {
        RenderSystem.disableDepthTest();
        RenderSystem.depthFunc(GL30.GL_LEQUAL);
        RenderSystem.enableCull();
        MinecraftClient.getInstance().gameRenderer.getLightmapTextureManager().disable();
        MinecraftClient.getInstance().gameRenderer.getOverlayTexture().teardownOverlayColor();
        RenderSystem.resetTextureMatrix();
        RenderSystem.depthMask(true);
        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableColorLogicOp();
        RenderSystem.disableScissor();
        RenderSystem.lineWidth(1.0F);
        GL11.glDisable(GL30.GL_STENCIL_TEST);
    }

    private static void checkValidId(String toCheck, String message) {
        try {
            new Identifier(toCheck);
        } catch (Exception e) {
            throw new LuaError(message + toCheck + ", possibly because of capitals or spaces");
        }
    }

    private static void checkValidCall() {
        if (!canCallGLFunctions)
            throw new LuaError("Can only call this function inside a registered renderlayer!");
    }

    private static void checkInRange(int arg, int min, int max, String message) {
        if (arg < min || arg > max)
            throw new LuaError(message);
    }

    private static void checkArg(int arg, String message, int... options) {
        for (int option : options)
            if (arg == option)
                return;
        throw new LuaError(message);
    }

    public static <E> E luaGetOrDefault(LuaTable table, LuaValue key, E defaultVal, Function<LuaValue, E> converter) {
        if (table.get(key).isnil())
            return defaultVal;
        return converter.apply(table.get(key));
    }

    public static Framebuffer mainFramebufferCopy;
    public static Framebuffer lastFramebufferCopy;

    public static void blitMainFramebuffer(Framebuffer target) {
        if (target == null) return;
        int readId = GL30.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int writeId = GL30.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        Framebuffer mainFramebuffer = MinecraftClient.getInstance().getFramebuffer();
        target.resize(mainFramebuffer.textureWidth, mainFramebuffer.textureHeight, MinecraftClient.IS_SYSTEM_MAC);
        RenderSystem.assertOnRenderThreadOrInit();
        GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, mainFramebuffer.fbo);
        GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, target.fbo);
        GlStateManager._glBlitFrameBuffer(0, 0, mainFramebuffer.textureWidth, mainFramebuffer.textureHeight, 0, 0, target.textureWidth, target.textureHeight, GL30.GL_COLOR_BUFFER_BIT, GL30.GL_NEAREST);
        GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readId);
        GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, writeId);
    }

    public static boolean areIrisShadersEnabled() {
        return FabricLoader.getInstance().isModLoaded("iris") && !net.coderbot.iris.Iris.getCurrentPack().isEmpty();
    }

    public static final Map<String, VertexFormat> vertexFormatMap;
    public static final Map<String, VertexFormat.DrawMode> drawModeMap;

    static {
        vertexFormatMap = new HashMap<>();
        vertexFormatMap.put("BLIT_SCREEN", VertexFormats.BLIT_SCREEN);
        vertexFormatMap.put("POSITION_COLOR_TEXTURE_LIGHT_NORMAL", VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL);
        vertexFormatMap.put("POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL); //Default
        vertexFormatMap.put("POSITION_TEXTURE_COLOR_LIGHT", VertexFormats.POSITION_TEXTURE_COLOR_LIGHT);
        vertexFormatMap.put("POSITION", VertexFormats.POSITION);
        vertexFormatMap.put("POSITION_COLOR", VertexFormats.POSITION_COLOR);
        vertexFormatMap.put("LINES", VertexFormats.LINES);
        vertexFormatMap.put("POSITION_COLOR_LIGHT", VertexFormats.POSITION_COLOR_LIGHT);
        vertexFormatMap.put("POSITION_TEXTURE", VertexFormats.POSITION_TEXTURE);
        vertexFormatMap.put("POSITION_COLOR_TEXTURE", VertexFormats.POSITION_COLOR_TEXTURE);
        vertexFormatMap.put("POSITION_TEXTURE_COLOR", VertexFormats.POSITION_TEXTURE_COLOR);
        vertexFormatMap.put("POSITION_COLOR_TEXTURE_LIGHT", VertexFormats.POSITION_COLOR_TEXTURE_LIGHT);
        vertexFormatMap.put("POSITION_TEXTURE_LIGHT_COLOR", VertexFormats.POSITION_TEXTURE_LIGHT_COLOR);
        vertexFormatMap.put("POSITION_TEXTURE_COLOR_NORMAL", VertexFormats.POSITION_TEXTURE_COLOR_NORMAL);

        drawModeMap = new HashMap<>();
        drawModeMap.put("LINES", VertexFormat.DrawMode.LINES);
        drawModeMap.put("LINE_STRIP", VertexFormat.DrawMode.LINE_STRIP);
        drawModeMap.put("DEBUG_LINES", VertexFormat.DrawMode.DEBUG_LINES);
        drawModeMap.put("DEBUG_LINE_STRIP", VertexFormat.DrawMode.DEBUG_LINE_STRIP);
        drawModeMap.put("TRIANGLES", VertexFormat.DrawMode.TRIANGLES);
        drawModeMap.put("TRIANGLE_STRIP", VertexFormat.DrawMode.TRIANGLE_STRIP);
        drawModeMap.put("TRIANGLE_FAN", VertexFormat.DrawMode.TRIANGLE_FAN);
        drawModeMap.put("QUADS", VertexFormat.DrawMode.QUADS); //Default

        RenderSystem.recordRenderCall(()->{
            Framebuffer mainFramebuffer = MinecraftClient.getInstance().getFramebuffer();
            mainFramebufferCopy = new WindowFramebuffer(mainFramebuffer.textureWidth, mainFramebuffer.textureHeight);
            mainFramebufferCopy.setClearColor(0,0,0,0);
            lastFramebufferCopy = new WindowFramebuffer(mainFramebuffer.textureWidth, mainFramebuffer.textureHeight);
            lastFramebufferCopy.setClearColor(0,0,0,0);
        });
    }

}
