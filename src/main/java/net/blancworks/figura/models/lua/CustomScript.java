package net.blancworks.figura.models.lua;

import com.google.common.collect.HashBiMap;
import net.minecraft.client.model.ModelPart;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Util;
import net.blancworks.figura.PlayerData;
import net.blancworks.figura.access.PlayerEntityModelAccess;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.*;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;
import org.luaj.vm2.lib.jse.JseBaseLib;
import org.luaj.vm2.lib.jse.JseMathLib;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class CustomScript {

    public static final HashSet<String> valid_names = new HashSet<String>() {{
        add("HEAD");
        add("TORSO");
        add("LEFT_ARM");
        add("RIGHT_ARM");
        add("LEFT_LEG");
        add("RIGHT_LEG");

        add("HELMET");
        add("LEFT_SLEEVE");
        add("RIGHT_SLEEVE");
        add("LEFT_PANT_LEG");
        add("RIGHT_PANT_LEG");
        add("JACKET");
    }};

    public static final int max_lua_instructions_render = 1024 * 2;
    public static final int max_lua_instructions_tick = 1024 * 4;
    public static final int max_lua_instructions_init = 1024 * 16;

    //This maps all vanilla model parts to a UUID so we can reference them safely from scripts without passing in a java object.
    private HashBiMap<UUID, ModelPart> partMap = HashBiMap.create();

    PlayerData m_data;

    //The chunk of data used to hold all the script instructions.
    public LuaValue chunk;

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
        m_data = data;

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

    public void runFunction(String name, int max_lua_instructions) {
        if (curr_task != null)
            return;

        runFunctionAsync(name, max_lua_instructions);
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
        } catch (Exception e) {
            System.out.println(e);
        }
    }
    
    public void queueTask(String name, int max_lua_instructions) {
        if (curr_task == null) {
            curr_task = runFunctionAsync(name, max_lua_instructions);
        } else {
            queued_tasks.add(name);
        }
    }

    private LuaThread test_thread;

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
                        System.out.println(e);
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


        //Vanilla Model
        LuaTable vanillaModelTable = new LuaTable();
        vanillaModelTable.set("HEAD", generateTableFromModelPart(m_data.vanillaModel.head));
        vanillaModelTable.set("LEFT_ARM", generateTableFromModelPart(m_data.vanillaModel.leftArm));
        vanillaModelTable.set("RIGHT_ARM", generateTableFromModelPart(m_data.vanillaModel.rightArm));
        vanillaModelTable.set("LEFT_LEG", generateTableFromModelPart(m_data.vanillaModel.leftLeg));
        vanillaModelTable.set("RIGHT_LEG", generateTableFromModelPart(m_data.vanillaModel.rightLeg));
        vanillaModelTable.set("TORSO", generateTableFromModelPart(m_data.vanillaModel.torso));

        vanillaModelTable.set("HELMET", generateTableFromModelPart(m_data.vanillaModel.helmet));
        vanillaModelTable.set("LEFT_SLEEVE", generateTableFromModelPart(m_data.vanillaModel.leftSleeve));
        vanillaModelTable.set("RIGHT_SLEEVE", generateTableFromModelPart(m_data.vanillaModel.rightSleeve));
        vanillaModelTable.set("LEFT_PANT_LEG", generateTableFromModelPart(m_data.vanillaModel.leftPantLeg));
        vanillaModelTable.set("RIGHT_PANT_LEG", generateTableFromModelPart(m_data.vanillaModel.rightPantLeg));
        vanillaModelTable.set("JACKET", generateTableFromModelPart(m_data.vanillaModel.jacket));

        scriptGlobals.set("vanilla_model", vanillaModelTable);


        //Custom Model
        LuaTable customModelTable = new LuaTable();

        scriptGlobals.set("model", customModelTable);
    }

    //Generates a lua table to match a given model part.
    public LuaTable generateTableFromModelPart(ModelPart part) {
        LuaTable newTable = new LuaTable();


        //Get a UUID and push it into the metatable.
        UUID id = null;

        if (partMap.inverse().containsKey(part)) {
            id = partMap.inverse().get(part);
        } else {
            id = UUID.randomUUID();
            partMap.put(id, part);
        }

        newTable.set("uuid", id.toString());


        //setPos for a model part.
        //Pass in self and a table with the target x,y,z positon.
        newTable.set("setPos", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue self, LuaValue data) {

                //Id for the target of this function.
                UUID getId = UUID.fromString(self.get("uuid").tostring().toString());

                //Check if part exists.
                if (!partMap.containsKey(getId))
                    return LuaValue.NIL;
                ModelPart targetPart = partMap.get(getId);
                
                LuaTable dataTable = data.checktable();
                targetPart.setPivot(
                        dataTable.get(1).tofloat(),
                        dataTable.get(2).tofloat(),
                        dataTable.get(3).tofloat()
                );

                return LuaValue.NIL;
            }
        });


        return newTable;
    }

    private static class LuaLog extends OneArgFunction {
        @Override
        public LuaValue call(LuaValue arg) {
            System.out.println(arg.toString());
            return NIL;
        }
    }
}