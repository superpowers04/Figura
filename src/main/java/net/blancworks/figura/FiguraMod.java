package net.blancworks.figura;

import net.blancworks.figura.models.CustomModel;
import net.blancworks.figura.models.DebugModel;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.blancworks.figura.commands.FiguraCommands;
import net.blancworks.figura.models.lua.FiguraLuaManager;

import java.nio.file.Path;
import java.nio.file.WatchKey;

public class FiguraMod implements ClientModInitializer {

    //Used during rendering.
    public static AbstractClientPlayerEntity curr_player;
    public static PlayerEntityModel curr_model;
    public static VertexConsumerProvider vertex_consumer_provider;
    private static PlayerData curr_data;

    private static final boolean USE_DEBUG_MODEL = true;
    private static final CustomModel debug_model = new DebugModel();
    private static WatchKey watch_key;
    private static Path path;

    //Lua

    //Methods

    //Set current player.
    //If there is a model loaded for the player, it'll be assigned here to the current model.
    //Otherwise, sends the model to the request list.
    public static void setRenderingMode(AbstractClientPlayerEntity player, VertexConsumerProvider vertexConsumerProvider, PlayerEntityModel mdl) {
        curr_player = player;
        curr_data = PlayerDataManager.getDataForPlayer(player.getUuid());
        curr_data.vanillaModel = mdl;
        vertex_consumer_provider = vertexConsumerProvider;
    }

    //Returns the current custom model for rendering. 
    //Set earlier by the player render function, used in the renderer mixin.
    public static PlayerData getCurrData() {
        return curr_data;
    }

    @Override
    public void onInitializeClient() {
        FiguraLuaManager.initialize();
        FiguraCommands.initialize();

        ClientTickEvents.END_CLIENT_TICK.register(FiguraMod::ClientEndTick);
    }


    //Client-side ticks.
    public static void ClientEndTick(MinecraftClient client) {
        PlayerDataManager.tick();
    }
}
