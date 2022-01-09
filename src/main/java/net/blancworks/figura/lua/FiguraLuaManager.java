package net.blancworks.figura.lua;

import net.blancworks.figura.FiguraMod;
import net.blancworks.figura.avatar.AvatarData;
import net.blancworks.figura.lua.api.AnimationAPI;
import net.blancworks.figura.lua.api.LuaEvent;
import net.blancworks.figura.lua.api.MetaAPI;
import net.blancworks.figura.lua.api.actionWheel.ActionWheel2API;
import net.blancworks.figura.lua.api.actionWheel.ActionWheelAPI;
import net.blancworks.figura.lua.api.block.BlockStateAPI;
import net.blancworks.figura.lua.api.chat.ChatAPI;
import net.blancworks.figura.lua.api.client.ClientAPI;
import net.blancworks.figura.lua.api.data.DataAPI;
import net.blancworks.figura.lua.api.item.ItemStackAPI;
import net.blancworks.figura.lua.api.keybind.KeyBindAPI;
import net.blancworks.figura.lua.api.nameplate.NamePlateAPI;
import net.blancworks.figura.lua.api.ReadOnlyLuaTable;
import net.blancworks.figura.lua.api.network.PingsAPI;
import net.blancworks.figura.lua.api.renderer.RendererAPI;
import net.blancworks.figura.lua.api.camera.CameraAPI;
import net.blancworks.figura.lua.api.math.VectorAPI;
import net.blancworks.figura.lua.api.model.*;
import net.blancworks.figura.lua.api.network.NetworkAPI;
import net.blancworks.figura.lua.api.particle.ParticleAPI;
import net.blancworks.figura.lua.api.renderlayers.RenderLayerAPI;
import net.blancworks.figura.lua.api.sound.SoundAPI;
import net.blancworks.figura.lua.api.world.WorldAPI;
import net.blancworks.figura.lua.api.entity.PlayerEntityAPI;
import net.minecraft.util.Identifier;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LoadState;
import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.compiler.LuaC;
import org.luaj.vm2.lib.PackageLib;
import org.luaj.vm2.lib.StringLib;
import org.luaj.vm2.lib.jse.JseBaseLib;
import org.luaj.vm2.lib.jse.JseMathLib;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class FiguraLuaManager {

    public static HashMap<Identifier, Function<CustomScript, ? extends LuaTable>> apiSuppliers = new HashMap<>();
    public static Map<String, Function<String, LuaEvent>> registeredEvents = new HashMap<>();

    //The globals for the entire lua system.
    public static Globals modGlobals;

    public static void initialize() {
        modGlobals = new Globals();
        modGlobals.load(new JseBaseLib());
        modGlobals.load(new PackageLib());
        modGlobals.load(new StringLib());
        modGlobals.load(new JseMathLib());

        LoadState.install(modGlobals);
        LuaC.install(modGlobals);

        LuaString.s_metatable = new ReadOnlyLuaTable(LuaString.s_metatable);

        registerEvents();
        registerAPI();
    }

    public static void registerAPI() {
        apiSuppliers.put(ParticleAPI.getID(), ParticleAPI::getForScript);
        apiSuppliers.put(CustomModelAPI.getID(), CustomModelAPI::getForScript);
        apiSuppliers.put(VanillaModelAPI.getID(), VanillaModelAPI::getForScript);
        apiSuppliers.put(PlayerEntityAPI.getID(), PlayerEntityAPI::getForScript);
        apiSuppliers.put(WorldAPI.getID(), WorldAPI::getForScript);
        apiSuppliers.put(ArmorModelAPI.getID(), ArmorModelAPI::getForScript);
        apiSuppliers.put(ElytraModelAPI.getID(), ElytraModelAPI::getForScript);
        apiSuppliers.put(ItemModelAPI.getID(), ItemModelAPI::getForScript);
        apiSuppliers.put(VectorAPI.getID(), VectorAPI::getForScript);
        apiSuppliers.put(SoundAPI.getID(), SoundAPI::getForScript);
        apiSuppliers.put(NamePlateAPI.getID(), NamePlateAPI::getForScript);
        apiSuppliers.put(MetaAPI.getID(), MetaAPI::getForScript);
        apiSuppliers.put(RendererAPI.getID(), RendererAPI::getForScript);
        apiSuppliers.put(CameraAPI.getID(), CameraAPI::getForScript);
        apiSuppliers.put(ParrotModelAPI.getID(), ParrotModelAPI::getForScript);
        apiSuppliers.put(ActionWheelAPI.getID(), ActionWheelAPI::getForScript);
        apiSuppliers.put(SpyglassModelAPI.getID(), SpyglassModelAPI::getForScript);
        apiSuppliers.put(NetworkAPI.getID(), NetworkAPI::getForScript);
        apiSuppliers.put(ItemStackAPI.getID(), ItemStackAPI::getForScript);
        apiSuppliers.put(KeyBindAPI.getID(), KeyBindAPI::getForScript);
        apiSuppliers.put(ChatAPI.getID(), ChatAPI::getForScript);
        apiSuppliers.put(ClientAPI.getID(), ClientAPI::getForScript);
        apiSuppliers.put(DataAPI.getID(), DataAPI::getForScript);
        apiSuppliers.put(RenderLayerAPI.getId(), RenderLayerAPI::getForScript);
        apiSuppliers.put(PingsAPI.getID(), PingsAPI::getForScript);
        apiSuppliers.put(BlockStateAPI.getID(), BlockStateAPI::getForScript);
        apiSuppliers.put(FirstPersonModelAPI.getID(), FirstPersonModelAPI::getForScript);
        apiSuppliers.put(AnimationAPI.getID(), AnimationAPI::getForScript);
        apiSuppliers.put(ActionWheel2API.getID(), ActionWheel2API::getForScript);

        FiguraMod.CUSTOM_APIS.forEach(api -> apiSuppliers.put(api.getID(), api::getForScript));
    }

    public static void registerEvents() {
        registerEvent("player_init");
        registerEvent("tick");
        registerEvent("world_render");
        registerEvent("render");
        registerEvent("onCommand");
        registerEvent("onDamage");
    }

    public static void loadScript(AvatarData data, String content) {
        data.script = new CustomScript(data, content);
    }

    public static void setupScriptAPI(CustomScript script) {
        for (Map.Entry<Identifier, Function<CustomScript, ? extends LuaTable>> entry : apiSuppliers.entrySet()) {
            try {
                script.scriptGlobals.set(entry.getKey().getPath(), entry.getValue().apply(script));
            } catch (Exception e) {
                System.out.println("Failed to initialize script global " + entry.getKey().toString());
                e.printStackTrace();
            }
        }
    }

    public static void registerEvent(String name) {
        registeredEvents.put(name, LuaEvent::new);
    }

}
