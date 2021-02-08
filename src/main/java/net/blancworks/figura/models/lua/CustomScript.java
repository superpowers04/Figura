package net.blancworks.figura.models.lua;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Util;
import net.blancworks.figura.PlayerData;
import net.blancworks.figura.access.PlayerEntityModelAccess;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.*;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;
import org.luaj.vm2.lib.jse.JseBaseLib;
import org.luaj.vm2.lib.jse.JseMathLib;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;

public class CustomScript {
    PlayerData m_data;

    final int max_lua_instructions_tick = 1024 * 2;
    final int max_lua_instructions_normal = 1024 * 8;
    final int max_lua_instructions_init = 1024 * 16;

    //The chunk of data used to hold all the script instructions.
    public LuaValue chunk;

    public Globals scriptGlobals = new Globals();
    public LuaValue setHook;
    public LuaValue hookFunction;

    public String source;

    private CompletableFuture curr_task;
    private Queue<String> queued_tasks = new ArrayDeque<String>();
    private LuaThread scriptThread;

    public CustomScript() { }
    
    public CustomScript(PlayerData data, String content) {
        load(data, content);
    }


    public void load(PlayerData data, String src) {
        m_data = data;

        source = src;

        scriptGlobals.load(new JseBaseLib());
        scriptGlobals.load(new PackageLib());
        scriptGlobals.load(new Bit32Lib());
        scriptGlobals.load(new TableLib());
        scriptGlobals.load(new StringLib());
        scriptGlobals.load(new JseMathLib());

        scriptGlobals.load(new DebugLib());
        setHook = scriptGlobals.get("debug").get("sethook");scriptGlobals.set("debug", LuaValue.NIL);

        setupInterfaceGlobals();

        LuaValue chunk = FiguraLuaManager.modGlobals.load(source, "main", scriptGlobals);
        scriptThread = new LuaThread(scriptGlobals, chunk);

        hookFunction = new ZeroArgFunction() {
            public LuaValue call() {
                // A simple lua error may be caught by the script, but a
                // Java Error will pass through to top and stop the script.
                System.out.println("Script overran resource limits.");
                throw new Error("Script overran resource limits.");
            }
        };

        curr_task = CompletableFuture.runAsync(
                () -> {
                    setInstructionLimit(max_lua_instructions_init);
                    Varargs result = scriptThread.resume(LuaValue.NIL);
                    curr_task = null;
                },
                Util.getMainWorkerExecutor()
        );
    }

    public void runFunction(String name) {
        if(curr_task != null)
            return;

        runFunctionAsync(name);
    }

    public void queueTask(String name) {
        if (curr_task == null) {
            curr_task = runFunctionAsync(name);
        } else {
            queued_tasks.add(name);
        }
    }

    private LuaThread test_thread;
    
    public CompletableFuture runFunctionAsync(String name) {
        return CompletableFuture.runAsync(
                () -> {
                    try {
                        scriptGlobals.running.state.bytecodes = 0;
                        LuaValue function = scriptGlobals.get(name);
                        LuaFunction func = function.checkfunction();
                        setInstructionLimit(max_lua_instructions_tick);
                        if (function.isnil() == false && function.isfunction() == true) {
                            function.call();
                        }

                        curr_task = null;
                        if (queued_tasks.size() > 0) {
                            String nextTask = queued_tasks.remove();
                            runFunctionAsync(nextTask);
                        }
                    }catch (Exception e){
                        System.out.println(e);
                    }
                },
                Util.getMainWorkerExecutor()
        );
    }
    
    public void setInstructionLimit(int max){
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

        LuaTable modelTable = new LuaTable();
        modelTable.set("enablePart", new LuaSetPartEnabled() {{
            owner = m_data;
        }});

        scriptGlobals.set("vanilla_model", modelTable);
    }

    private static class LuaLog extends OneArgFunction {
        @Override
        public LuaValue call(LuaValue arg) {
            System.out.println(arg.tostring().toString());
            return NIL;
        }
    }
    private static class LuaSetPartEnabled extends TwoArgFunction {
        public PlayerData owner;
        public static final HashSet<String> valid_names = new HashSet<String>() {{
            add("HEAD");
            add("TORSO");
            add("LEFT_ARM");
            add("RIGHT_ARM");
            add("LEFT_LEG");
            add("RIGHT_LEG");
        }};

        @Override
        public LuaValue call(LuaValue arg1, LuaValue arg2) {

            if (arg1.isnil() || arg2.isnil())
                return NIL;
            if (arg1.isstring() == false || arg2.isboolean() == false)
                return NIL;

            String target_part_name = arg1.toString();
            if (!valid_names.contains(target_part_name))
                return NIL;

            PlayerEntityModelAccess mix = (PlayerEntityModelAccess) (Object) (owner.vanillaModel);
            HashSet<String> dp = mix.getDisabledParts();

            if (arg2.toboolean() == true) {
                if (dp.contains(target_part_name))
                    dp.remove(target_part_name);
            } else {
                if (!dp.contains(target_part_name))
                    dp.add(target_part_name);
            }

            return NIL;
        }
    }
}