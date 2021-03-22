package net.blancworks.figura.lua;

import net.blancworks.figura.FiguraMod;
import net.blancworks.figura.PlayerData;
import net.blancworks.figura.PlayerDataManager;
import net.blancworks.figura.access.ModelPartAccess;
import net.blancworks.figura.access.PlayerEntityModelAccess;
import net.blancworks.figura.lua.api.model.VanillaModelPartCustomization;
import net.blancworks.figura.trust.PlayerTrustManager;
import net.blancworks.figura.trust.TrustContainer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.Level;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.*;
import org.luaj.vm2.lib.jse.JseBaseLib;
import org.luaj.vm2.lib.jse.JseMathLib;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;

public class CustomScript {

    public PlayerData playerData;

    public Globals scriptGlobals = new Globals();
    public LuaValue setHook;
    public LuaValue hookFunction;

    public String source;

    private CompletableFuture curr_task;
    private Queue<String> queued_tasks = new ArrayDeque<String>();
    private LuaThread scriptThread;

    public VanillaModelPartCustomization[] vanillaModifications = new VanillaModelPartCustomization[12];

    public CustomScript() {
    }

    public CustomScript(PlayerData data, String content) {
        load(data, content);
    }


    public void load(PlayerData data, String src) {
        playerData = data;

        source = src;

        scriptGlobals.load(new JseBaseLib());
        scriptGlobals.load(new PackageLib());
        scriptGlobals.load(new Bit32Lib());
        scriptGlobals.load(new TableLib());
        scriptGlobals.load(new StringLib());
        scriptGlobals.load(new JseMathLib());

        scriptGlobals.load(new DebugLib());
        setHook = scriptGlobals.get("debug").get("sethook");
        scriptGlobals.set("debug", LuaValue.NIL);

        setupGlobals();

        LuaValue chunk = FiguraLuaManager.modGlobals.load(source, "main", scriptGlobals);
        scriptThread = new LuaThread(scriptGlobals, chunk);

        hookFunction = new ZeroArgFunction() {
            public LuaValue call() {
                // A simple lua error may be caught by the script, but a
                // Java Error will pass through to top and stop the script.
                FiguraMod.LOGGER.log(Level.WARN, "Script overran resource limits.");
                throw new Error("Script overran resource limits.");
            }
        };

        curr_task = CompletableFuture.runAsync(
                () -> {
                    setInstructionLimit(getTrustInstructionLimit(PlayerTrustManager.maxInitID));
                    Varargs result = scriptThread.resume(LuaValue.NIL);
                    curr_task = null;
                }
        );
    }

    public void runFunction(String name, int max_lua_instructions) {
        if (curr_task != null)
            return;

        runFunctionAsync(name, max_lua_instructions);
    }

    public int getTrustInstructionLimit(Identifier settingID) {
        TrustContainer tc = playerData.getTrustContainer();

        return tc.getIntSetting(settingID);
    }

    public void runFunctionImmediate(String name, int max_lua_instructions) {
        runFunctionImmediate(name, max_lua_instructions, LuaValue.NIL);
    }

    public void runFunctionImmediate(String name, int max_lua_instructions, LuaValue args) {
        try {
            scriptGlobals.running.state.bytecodes = 0;
            LuaValue function = scriptGlobals.get(name);
            LuaFunction func = function.checkfunction();
            setInstructionLimit(max_lua_instructions);
            if (function.isnil() == false && function.isfunction() == true) {
                function.call(args);
            }
        } catch (Throwable e) {
            FiguraMod.LOGGER.log(Level.ERROR, e);
        }
    }

    public void queueTask(String name, int max_lua_instructions) {
        if (curr_task == null) {
            curr_task = runFunctionAsync(name, max_lua_instructions);
        } else {
            queued_tasks.add(name);
        }
    }

    public CompletableFuture runFunctionAsync(String name, int max_lua_instructions) {
        return CompletableFuture.runAsync(
                () -> {
                    try {
                        scriptGlobals.running.state.bytecodes = 0;
                        LuaValue function = scriptGlobals.get(name);
                        LuaFunction func = function.checkfunction();
                        setInstructionLimit(max_lua_instructions);
                        if (function.isnil() == false && function.isfunction() == true) {
                            function.call();
                        }

                        curr_task = null;
                        if (queued_tasks.size() > 0) {
                            String nextTask = queued_tasks.remove();
                            runFunctionAsync(nextTask, max_lua_instructions);
                        }
                    } catch (Exception e) {
                        FiguraMod.LOGGER.log(Level.ERROR, e);
                    }
                }
        );
    }

    public void setInstructionLimit(int max) {
        setHook.invoke(
                LuaValue.varargsOf(
                        new LuaValue[]{
                                hookFunction,
                                LuaValue.EMPTYSTRING, LuaValue.valueOf(max)
                        }
                )
        );
    }

    public void toNBT(CompoundTag tag) {
        tag.putString("src", source);
    }

    public void fromNBT(PlayerData data, CompoundTag tag) {
        load(data, tag.getString("src"));
    }

    public void setupGlobals() {

        for (int i = 0; i < vanillaModifications.length; i++) {
            vanillaModifications[i] = new VanillaModelPartCustomization();
        }

        //Log! Only for local player.
        scriptGlobals.set("log", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue arg) {
                if (playerData == PlayerDataManager.localPlayer) {
                    FiguraMod.LOGGER.warn(arg.toString());
                    MinecraftClient.getInstance().player.sendMessage(new LiteralText(arg.toString()), false);
                }
                return NIL;
            }
        });

        scriptGlobals.set("logTableContent", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue arg) {
                LuaTable table = arg.checktable();

                logTableContents(table, 0, "");

                return NIL;
            }
        });

        FiguraLuaManager.setupScriptAPI(this);
    }

    public void logTableContents(LuaTable table, int depth, String depthString) {
        String nextDepthString = depthString + "    ";
        MinecraftClient.getInstance().player.sendMessage(new LiteralText(depthString + "{"), false);

        for (LuaValue key : table.keys()) {
            LuaValue value = table.get(key);
            
            String valString = depthString + '"' + key.toString() + '"' + " : " + value.toString();
            
            if (value.istable()) {
                MinecraftClient.getInstance().player.sendMessage(new LiteralText(valString), false);
                logTableContents(value.checktable(), depth + 1, nextDepthString);
            } else {
                MinecraftClient.getInstance().player.sendMessage(new LiteralText(valString + ","), false);
            }
        }
        MinecraftClient.getInstance().player.sendMessage(new LiteralText(depthString + "},"), false);
    }

    public void tick() {

        if (MinecraftClient.getInstance().isPaused()) {
            return;
        }

        runFunction("tick", getTrustInstructionLimit(PlayerTrustManager.maxTickID));

    }


    public void applyCustomValues(PlayerEntityModel model) {

        PlayerEntityModelAccess access = (PlayerEntityModelAccess) (Object) model;
        HashSet<ModelPart> parts = access.getDisabledParts();

        applyCustomValues(parts, model.head, 0);
        applyCustomValues(parts, model.torso, 1);

        applyCustomValues(parts, model.leftArm, 2);
        applyCustomValues(parts, model.rightArm, 3);

        applyCustomValues(parts, model.leftLeg, 4);
        applyCustomValues(parts, model.rightLeg, 5);

        applyCustomValues(parts, model.helmet, 0);
        applyCustomValues(parts, model.jacket, 1);

        applyCustomValues(parts, model.leftSleeve, 2);
        applyCustomValues(parts, model.rightSleeve, 3);

        applyCustomValues(parts, model.leftPantLeg, 4);
        applyCustomValues(parts, model.rightPantLeg, 5);
    }

    private void applyCustomValues(HashSet<ModelPart> disabledParts, ModelPart part, int index) {
        ModelPartAccess access = (ModelPartAccess) (Object) part;
        VanillaModelPartCustomization customization = vanillaModifications[index];

        if (customization.pos != null)
            access.setAdditionalPos(customization.pos);
        if (customization.rot != null)
            access.setAdditionalRot(customization.rot);
        if (customization.visible != null && !customization.visible)
            disabledParts.add(part);
    }
}