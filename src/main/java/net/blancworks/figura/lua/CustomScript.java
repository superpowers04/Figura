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
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.Level;
import org.jetbrains.annotations.Nullable;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.*;
import org.luaj.vm2.lib.jse.JseBaseLib;
import org.luaj.vm2.lib.jse.JseMathLib;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;

public class CustomScript {

    public PlayerData playerData;

    public Globals scriptGlobals = new Globals();
    public LuaValue setHook;
    public LuaValue hookFunction;

    public String source;

    private CompletableFuture<Void> curr_task;
    private Queue<LuaFunction> queued_tasks = new ArrayDeque<>();
    private LuaThread scriptThread;

    public int tickInstructionCount = 0;
    public int renderInstructionCount = 0;

    private @Nullable LuaFunction tick = null;
    private @Nullable LuaFunction render = null;

    public VanillaModelPartCustomization[] vanillaModifications = new VanillaModelPartCustomization[16];

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
                    try {
                        setInstructionLimit(getTrustInstructionLimit(PlayerTrustManager.maxInitID));
                        scriptThread.resume(LuaValue.NIL);
                    } catch (LuaError error){
                        error.printStackTrace();
                    }
                    try {
                        tick = scriptGlobals.get("tick").checkfunction();
                    } catch (LuaError error) {
                        error.printStackTrace();
                    }

                    try {
                        render = scriptGlobals.get("render").checkfunction();
                    } catch (LuaError error) {
                        error.printStackTrace();
                    }
                    curr_task = null;
                }
        );

    }

    public void runFunction(LuaFunction func, int max_lua_instructions) {
        if (curr_task != null)
            return;

        runFunctionAsync(func, max_lua_instructions);
    }

    public int getTrustInstructionLimit(Identifier settingID) {
        TrustContainer tc = playerData.getTrustContainer();

        return tc.getIntSetting(settingID);
    }

    private void runFunctionImmediate(LuaFunction func, int max_lua_instructions, LuaValue args) {
        try {
            scriptGlobals.running.state.bytecodes = 0;
            setInstructionLimit(max_lua_instructions);
            if (!func.isnil() && func.isfunction()) {
                func.call(args);
            }

            if (func == tick) {
                tickInstructionCount = scriptGlobals.running.state.bytecodes;
            }

            if (func == render) {
                renderInstructionCount = scriptGlobals.running.state.bytecodes;
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public void queueTask(LuaFunction func, int max_lua_instructions) {
        if (curr_task == null) {
            curr_task = runFunctionAsync(func, max_lua_instructions);
        } else {
            queued_tasks.add(func);
        }
    }

    private CompletableFuture<Void> runFunctionAsync(LuaFunction func, int max_lua_instructions) {
        return CompletableFuture.runAsync(
                () -> {
                    try {
                        scriptGlobals.running.state.bytecodes = 0;

                        setInstructionLimit(max_lua_instructions);
                        if (!func.isnil() && func.isfunction()) {
                            func.call();
                        }

                        if (func == tick) {
                            tickInstructionCount = scriptGlobals.running.state.bytecodes;
                        }

                        if (func == render) {
                            renderInstructionCount = scriptGlobals.running.state.bytecodes;
                        }

                        curr_task = null;
                        if (!queued_tasks.isEmpty()) {
                            LuaFunction nextTask = queued_tasks.remove();
                            runFunctionAsync(nextTask, max_lua_instructions);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
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

    public void toNBT(NbtCompound tag) {
        tag.putString("src", source);
    }

    public void fromNBT(PlayerData data, NbtCompound tag) {
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
                    MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(new LiteralText(arg.toString()));
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

        if (tick != null) {
            runFunction(tick, getTrustInstructionLimit(PlayerTrustManager.maxTickID));
        }
    }

    public void render(float deltaTime) {
        if (render != null) {
            runFunctionImmediate(render, getTrustInstructionLimit(PlayerTrustManager.maxRenderID), LuaValue.valueOf(deltaTime));
        }
    }


    public void applyCustomValues(PlayerEntityModel model) {

        PlayerEntityModelAccess access = (PlayerEntityModelAccess) (Object) model;
        HashSet<ModelPart> parts = access.getDisabledParts();
        parts.clear();

        applyCustomValues(parts, model.head, 0);
        applyCustomValues(parts, model.body, 1);

        applyCustomValues(parts, model.leftArm, 2);
        applyCustomValues(parts, model.rightArm, 3);

        applyCustomValues(parts, model.leftLeg, 4);
        applyCustomValues(parts, model.rightLeg, 5);

        applyCustomValues(parts, model.hat, 6);
        applyCustomValues(parts, model.jacket, 7);

        applyCustomValues(parts, model.leftSleeve, 8);
        applyCustomValues(parts, model.rightSleeve, 9);

        applyCustomValues(parts, model.leftPants, 10);
        applyCustomValues(parts, model.rightPants, 11);
    }

    public void applyArmorValues(BipedEntityModel model, int index) {

        if (index == 12)
            applyCustomValues(model.head, 12);

        if (index == 13) {
            applyCustomValues(model.body, 13);
            applyCustomValues(model.rightArm, 13);
            applyCustomValues(model.leftArm, 13);
        }

        if (index == 14) {
            applyCustomValues(model.body, 14);
            applyCustomValues(model.leftLeg, 14);
            applyCustomValues(model.rightLeg, 14);
        }

        if (index == 15) {
            applyCustomValues(model.leftLeg, 15);
            applyCustomValues(model.rightLeg, 15);
        }

    }

    private void applyCustomValues(HashSet<ModelPart> disabledParts, ModelPart part, int index) {
        ModelPartAccess access = (ModelPartAccess) (Object) part;
        VanillaModelPartCustomization customization = vanillaModifications[index];

        if (customization == null)
            return;

        if (customization.pos != null)
            access.setAdditionalPos(customization.pos);
        if (customization.rot != null)
            access.setAdditionalRot(customization.rot);
        if (customization.visible != null && !customization.visible)
            disabledParts.add(part);
    }

    private void applyCustomValues(ModelPart part, int index) {
        VanillaModelPartCustomization customization = vanillaModifications[index];

        if (customization == null)
            return;

        if (part == null)
            return;

        if (part.visible && customization.visible != null && !customization.visible)
            part.visible = false;
    }


    public HashMap<String, Integer> armorNameToIndex = new HashMap<String, Integer>() {{
        put("HELMET", 12);
        put("CHESTPLATE", 13);
        put("LEGGINGS", 14);
        put("BOOTS", 15);
    }};

    public void setArmorEnabled(String targetPart, boolean state) {
        if (!armorNameToIndex.containsKey(targetPart)) return;
        int index = armorNameToIndex.get(targetPart);
        VanillaModelPartCustomization vpc = vanillaModifications[index];

        vpc.visible = state;
    }

    public void clearArmorEnable(String targetPart) {
        if (!armorNameToIndex.containsKey(targetPart)) return;
        int index = armorNameToIndex.get(targetPart);
        VanillaModelPartCustomization vpc = vanillaModifications[index];

        vpc.visible = null;
    }
}
