package net.blancworks.figura;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.blancworks.figura.commands.FiguraCommands;
import net.blancworks.figura.lua.FiguraLuaManager;
import net.blancworks.figura.models.CustomModel;
import net.blancworks.figura.models.parsers.BlockbenchModelDeserializer;
import net.blancworks.figura.network.FiguraNetworkManager;
import net.blancworks.figura.trust.PlayerTrustManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchKey;

public class FiguraMod implements ClientModInitializer {

    public static final Gson GSON = new GsonBuilder().registerTypeAdapter(CustomModel.class, new BlockbenchModelDeserializer())
            .setPrettyPrinting().create();

    public static final Logger LOGGER = LogManager.getLogger();

    //Used during rendering.
    public static AbstractClientPlayerEntity currentPlayer;
    public static PlayerEntityModel currentModel;
    private static PlayerData currentData;
    public static VertexConsumerProvider vertexConsumerProvider;
    public static float deltaTime;
    public static MatrixStack modelMatrix;

    private static final boolean USE_DEBUG_MODEL = true;
    private static WatchKey watchKey;
    private static Path path;

    //Lua

    //Methods

    //Set current player.
    //If there is a model loaded for the player, it'll be assigned here to the current model.
    //Otherwise, sends the model to the request list.
    public static void setRenderingMode(AbstractClientPlayerEntity player, VertexConsumerProvider vertexConsumerProvider, PlayerEntityModel mdl, float dt, MatrixStack mdlMatrix) {
        currentPlayer = player;
        currentData = PlayerDataManager.getDataForPlayer(player.getUuid());
        currentData.vanillaModel = mdl;
        FiguraMod.vertexConsumerProvider = vertexConsumerProvider;
        deltaTime = dt;
        modelMatrix = mdlMatrix;
        currentModel = mdl;
    }

    //Returns the current custom model for rendering. 
    //Set earlier by the player render function, used in the renderer mixin.
    public static PlayerData getCurrentData() {
        PlayerData ret = currentData;
        currentData = null;
        return ret;
    }

    @Override
    public void onInitializeClient() {
        FiguraLuaManager.initialize();
        FiguraCommands.initialize();
        PlayerTrustManager.init();

        ClientTickEvents.END_CLIENT_TICK.register(FiguraMod::ClientEndTick);

        getModContentDirectory();
    }

    //Client-side ticks.
    public static void ClientEndTick(MinecraftClient client) {
        PlayerDataManager.tick();
        FiguraNetworkManager.tickNetwork();
    }

    public static Path getModContentDirectory() {
        Path p = FabricLoader.INSTANCE.getGameDir().normalize().resolve("figura");
        try {
            Files.createDirectories(p);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return p;
    }
}
