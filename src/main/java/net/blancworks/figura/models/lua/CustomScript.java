package net.blancworks.figura.models.lua;

import net.blancworks.figura.FiguraMod;
import net.blancworks.figura.PlayerData;
import net.blancworks.figura.models.lua.representations.CustomModelRepresentation;
import net.blancworks.figura.models.lua.representations.PlayerRepresentation;
import net.blancworks.figura.models.lua.representations.VanillaModelRepresentation;
import net.blancworks.figura.trust.PlayerTrustManager;
import net.blancworks.figura.trust.TrustContainer;
import net.blancworks.figura.trust.settings.PermissionFloatSetting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.apache.logging.log4j.Level;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.*;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;
import org.luaj.vm2.lib.jse.JseBaseLib;
import org.luaj.vm2.lib.jse.JseMathLib;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;

public class CustomScript {

    //Represents the vanilla model inside the script's code
    public VanillaModelRepresentation vanillaModelRepresentation;
    //Represents the player's data inside the script's code.
    public PlayerRepresentation playerRepresentation;
    public CustomModelRepresentation customModelRepresentation;

    public PlayerData playerData;

    public Globals scriptGlobals = new Globals();
    public LuaValue setHook;
    public LuaValue hookFunction;

    public String source;

    private CompletableFuture curr_task;
    private Queue<String> queued_tasks = new ArrayDeque<String>();
    private LuaThread scriptThread;

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

        setupInterfaceGlobals();

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
                },
                Util.getMainWorkerExecutor()
        );
    }

    public void runFunction(String name, int max_lua_instructions) {
        if (curr_task != null)
            return;

        runFunctionAsync(name, max_lua_instructions);
    }
    
    public int getTrustInstructionLimit(Identifier settingID){
        Identifier playerID = new Identifier("players", playerData.playerId.toString());

        TrustContainer tc = PlayerTrustManager.getContainer(playerID);
        PermissionFloatSetting setting = (PermissionFloatSetting) tc.getSetting(settingID);
        
        return (int)setting.value;
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
                },
                Util.getMainWorkerExecutor()
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

    public void setupInterfaceGlobals() {
        scriptGlobals.set("log", CoerceJavaToLua.coerce(new LuaLog()));

        vanillaModelRepresentation = new VanillaModelRepresentation(this);
        customModelRepresentation = new CustomModelRepresentation(this);
        playerRepresentation = new PlayerRepresentation(this);
    }

    private static class LuaLog extends OneArgFunction {
        @Override
        public LuaValue call(LuaValue arg) {
            FiguraMod.LOGGER.log(Level.DEBUG, arg.toString());
            return NIL;
        }
    }
}