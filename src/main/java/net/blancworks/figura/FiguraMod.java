package net.blancworks.figura;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.blancworks.figura.lua.FiguraLuaManager;
import net.blancworks.figura.models.CustomModel;
import net.blancworks.figura.models.CustomModelPart;
import net.blancworks.figura.models.parsers.BlockbenchModelDeserializer;
import net.blancworks.figura.network.FiguraNetworkManager;
import net.blancworks.figura.trust.PlayerTrustManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchKey;
import java.util.concurrent.CompletableFuture;

public class FiguraMod implements ClientModInitializer {

    public static final Gson GSON = new GsonBuilder().registerTypeAdapter(CustomModel.class, new BlockbenchModelDeserializer())
            .setPrettyPrinting().create();

    public static final Logger LOGGER = LogManager.getLogger();

    //Loading

    //This task is what's used to manage all loading requests in the whole mod.
    //If an asset is set to load, it will attach to this if it exists, or create a new one if it doesn't.
    private static CompletableFuture globalLoadTask;

    private PlayerDataManager dataManagerInstance;


    //Used during rendering.
    public static AbstractClientPlayerEntity currentPlayer;
    public static PlayerData currentData;
    public static VertexConsumerProvider vertexConsumerProvider;
    public static float deltaTime;

    private static final boolean USE_DEBUG_MODEL = true;
    private static WatchKey watchKey;
    private static Path path;

    //Methods

    //Set current player.
    //If there is a model loaded for the player, it'll be assigned here to the current model.
    //Otherwise, sends the model to the request list.
    public static void setRenderingData(AbstractClientPlayerEntity player, VertexConsumerProvider vertexConsumerProvider, PlayerEntityModel mdl, float dt) {
        currentPlayer = player;
        currentData = PlayerDataManager.getDataForPlayer(player.getUuid());
        currentData.vanillaModel = mdl;
        FiguraMod.vertexConsumerProvider = vertexConsumerProvider;
        deltaTime = dt;
    }

    public static void clearRenderingData() {
        currentPlayer = null;
        currentData = null;
        deltaTime = 0;
    }

    @Override
    public void onInitializeClient() {
        FiguraLuaManager.initialize();
        PlayerTrustManager.init();
        Config.initialize();

        ClientTickEvents.END_CLIENT_TICK.register(FiguraMod::ClientEndTick);
        WorldRenderEvents.AFTER_ENTITIES.register(FiguraMod::renderFirstPersonWorldParts);

        dataManagerInstance = new PlayerDataManager();
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override
            public Identifier getFabricId() {
                return new Identifier("figura", "reloadtextures");
            }

            @Override
            public void apply(ResourceManager manager) {
                PlayerDataManager.reloadAllTextures();
            }
        });

        getModContentDirectory();
    }

    //Client-side ticks.
    public static void ClientEndTick(MinecraftClient client) {
        PlayerDataManager.tick();
        FiguraNetworkManager.tickNetwork();
    }

    public static Path getModContentDirectory() {
        Path p = FabricLoader.getInstance().getGameDir().normalize().resolve("figura");
        try {
            Files.createDirectories(p);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return p;
    }

    public static CompletableFuture doTask(Runnable toRun) {
        return doTask(toRun, null);
    }

    public static CompletableFuture doTask(Runnable toRun, @Nullable Runnable onFinished) {
        //If the global load task doesn't exist, create it.
        if (globalLoadTask == null || globalLoadTask.isDone()) {
            globalLoadTask = CompletableFuture.runAsync(
                    () -> {
                        runTask(toRun, onFinished);
                    }
            );
        } else {
            //Otherwise, queue up next task.
            globalLoadTask = globalLoadTask.thenRunAsync(
                    () -> {
                        runTask(toRun, onFinished);
                    }
            );
        }

        return globalLoadTask;
    }

    private static void runTask(Runnable toRun, @Nullable Runnable onFinished) {
        toRun.run();

        if (onFinished != null)
            onFinished.run();
    }


    private static void renderFirstPersonWorldParts(WorldRenderContext context) {
        try {
            if (!context.camera().isThirdPerson()) {
                PlayerData data = PlayerDataManager.localPlayer;

                if (data.lastEntity != null) {

                    FiguraMod.currentData = data;

                    context.matrixStack().push();
                    context.matrixStack().translate(-context.camera().getPos().x, -context.camera().getPos().y, -context.camera().getPos().z);
                    context.matrixStack().scale(-1, -1, 1);

                    try {
                        if (data != null && data.model != null) {
                            for (CustomModelPart part : data.model.worldParts) {
                                part.renderUsingAllTextures(data, context.matrixStack(), FiguraMod.vertexConsumerProvider, MinecraftClient.getInstance().getEntityRenderDispatcher().getLight(data.lastEntity, context.tickDelta()), OverlayTexture.DEFAULT_UV);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    context.matrixStack().pop();

                    FiguraMod.clearRenderingData();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
