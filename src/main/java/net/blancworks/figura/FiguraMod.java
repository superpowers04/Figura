package net.blancworks.figura;

import net.blancworks.figura.config.ConfigManager;
import net.blancworks.figura.config.ConfigManager.Config;
import net.blancworks.figura.config.ConfigManager.ConfigKeyBind;
import net.blancworks.figura.gui.FiguraToast;
import net.blancworks.figura.lua.FiguraLuaManager;
import net.blancworks.figura.lua.api.FiguraAPI;
import net.blancworks.figura.lua.api.sound.FiguraSoundManager;
import net.blancworks.figura.network.IFiguraNetwork;
import net.blancworks.figura.network.NewFiguraNetworkManager;
import net.blancworks.figura.trust.PlayerTrustManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.fabricmc.fabric.impl.client.keybinding.KeyBindingRegistryImpl;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.SemanticVersion;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.InputUtil;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.UnaryOperator;

public class FiguraMod implements ClientModInitializer {
    public static final Logger LOGGER = LogManager.getLogger();

    public static final Identifier FIGURA_FONT = new Identifier("figura", "default");
    public static final UnaryOperator<Style> ACCENT_COLOR = FiguraMod::getAccentColor;

    public static final String MOD_VERSION = FabricLoader.getInstance().getModContainer("figura").get().getMetadata().getVersion().getFriendlyString();

    public static final boolean IS_CHEESE = LocalDate.now().getDayOfMonth() == 1 && LocalDate.now().getMonthValue() == 4;
    public static NbtCompound cheese;

    public static final ConfigKeyBind ACTION_WHEEL_BUTTON = new ConfigKeyBind("figura.config.action_wheel_button", GLFW.GLFW_KEY_B, ConfigManager.MOD_NAME, Config.ACTION_WHEEL_BUTTON);
    public static final ConfigKeyBind PLAYER_POPUP_BUTTON = new ConfigKeyBind("figura.config.player_popup_button", GLFW.GLFW_KEY_R, ConfigManager.MOD_NAME, Config.PLAYER_POPUP_BUTTON);
    public static final ConfigKeyBind PANIC_BUTTON = new ConfigKeyBind("figura.config.panic_button", GLFW.GLFW_KEY_UNKNOWN, ConfigManager.MOD_NAME, Config.PANIC_BUTTON);

    public static int ticksElapsed;

    public static final String GRADLE_PROPERTIES_LINK = "https://raw.githubusercontent.com/Blancworks/Figura/1.18/gradle.properties";
    public static String latestVersion;
    public static int latestVersionStatus = 0;

    //Loading

    //This task is what's used to manage all loading requests in the whole mod.
    //If an asset is set to load, it will attach to this if it exists, or create a new one if it doesn't.
    private static CompletableFuture<?> globalLoadTask;

    public static IFiguraNetwork networkManager;

    //private static FiguraNetworkManager oldNetworkManager;
    private static NewFiguraNetworkManager newNetworkManager;

    //Used during rendering.
    public static AbstractClientPlayerEntity currentPlayer;
    public static PlayerData currentData;
    public static VertexConsumerProvider vertexConsumerProvider;
    public static VertexConsumerProvider.Immediate immediate;
    public static float deltaTime;

    //script entry points
    public static final List<FiguraAPI> apis = new ArrayList<>();

    //Methods

    //Set current player.
    //If there is a model loaded for the player, it'll be assigned here to the current model.
    //Otherwise, sends the model to the request list.
    public static void setRenderingData(AbstractClientPlayerEntity player, VertexConsumerProvider vcp, PlayerEntityModel<?> mdl, float dt) {
        currentPlayer = player;
        currentData = PlayerDataManager.getDataForPlayer(player.getUuid());
        if (currentData != null)
            currentData.vanillaModel = mdl;
        FiguraMod.vertexConsumerProvider = vcp;
        if (vcp.getClass() == VertexConsumerProvider.Immediate.class) {
            FiguraMod.immediate = (VertexConsumerProvider.Immediate) vcp;
        }
        deltaTime = dt;
    }

    public static VertexConsumerProvider tryGetImmediate() {
        return immediate == null ? vertexConsumerProvider : immediate;
    }

    public static void clearRenderingData() {
        currentPlayer = null;
        currentData = null;
        deltaTime = 0;
    }

    @Override
    public void onInitializeClient() {
        //load entrypoints
        FabricLoader.getInstance().getEntrypointContainers("figura", FiguraAPI.class).forEach(entrypoint -> {
            ModMetadata metadata = entrypoint.getProvider().getMetadata();
            String modId = metadata.getId();
            try {
                apis.add(entrypoint.getEntrypoint());
            } catch (Exception e) {
                LOGGER.error("Failed to load entrypoint of mod {}", modId, e);
            }
        });

        //initialise managers
        ConfigManager.initialize();
        FiguraLuaManager.initialize();
        PlayerTrustManager.init();
        LocalAvatarManager.init();

        try {
            SSLFixer.main();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //set keybinds based on config
        ACTION_WHEEL_BUTTON.setBoundKey(InputUtil.Type.KEYSYM.createFromCode((int) Config.ACTION_WHEEL_BUTTON.value));
        PLAYER_POPUP_BUTTON.setBoundKey(InputUtil.Type.KEYSYM.createFromCode((int) Config.PLAYER_POPUP_BUTTON.value));
        PANIC_BUTTON.setBoundKey(InputUtil.Type.KEYSYM.createFromCode((int) Config.PANIC_BUTTON.value));

        KeyBindingRegistryImpl.registerKeyBinding(ACTION_WHEEL_BUTTON);
        KeyBindingRegistryImpl.registerKeyBinding(PLAYER_POPUP_BUTTON);
        KeyBindingRegistryImpl.registerKeyBinding(PANIC_BUTTON);

        //Set up network
        newNetworkManager = new NewFiguraNetworkManager();
        networkManager = newNetworkManager;

        //Register fabric events
        ClientTickEvents.END_CLIENT_TICK.register(FiguraMod::ClientEndTick);
        WorldRenderEvents.AFTER_ENTITIES.register(FiguraMod::renderFirstPersonWorldParts);
        ClientLifecycleEvents.CLIENT_STOPPING.register((v) -> networkManager.onClose());

        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override
            public Identifier getFabricId() {
                return new Identifier("figura", "reloadtextures");
            }

            @Override
            public void reload(ResourceManager manager) {
                PlayerDataManager.reloadAllTextures();

                try {
                    cheese = NbtIo.readCompressed(manager.getResource(new Identifier("figura", "cheese/cheese.nbt")).getInputStream());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        getModContentDirectory();
        getLatestModVersion();
    }

    //Client-side ticks.
    public static void ClientEndTick(MinecraftClient client) {
        try {
            PlayerDataManager.tick();
            FiguraSoundManager.tick();

            networkManager = newNetworkManager;

            if (networkManager != null)
                networkManager.tickNetwork();
        } catch (Exception e) {
            e.printStackTrace();
        }
        ticksElapsed++;
    }

    public static Path getModContentDirectory() {
        String userPath = (String) Config.MODEL_FOLDER_PATH.value;
        try {
            Path p = userPath.isEmpty() ? getDefaultDirectory() : Path.of(userPath);
            if (!Files.exists(p))
                Files.createDirectories(p);

            return p;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return getDefaultDirectory();
    }

    public static Path getDefaultDirectory() {
        Path p = FabricLoader.getInstance().getGameDir().normalize().resolve("figura");
        try {
            if (!Files.exists(p))
                Files.createDirectories(p);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return p;
    }

    public static CompletableFuture<?> doTask(Runnable toRun) {
        try {
            return doTask(toRun, null);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static CompletableFuture<?> doTask(Runnable toRun, @Nullable Runnable onFinished) {
        //If the global load task doesn't exist, create it.
        if (globalLoadTask == null || globalLoadTask.isDone())
            globalLoadTask = CompletableFuture.runAsync(() -> runTask(toRun, onFinished));
        //Otherwise, queue up next task.
        else
            globalLoadTask = globalLoadTask.thenRunAsync(() -> runTask(toRun, onFinished));

        return globalLoadTask;
    }

    private static void runTask(Runnable toRun, @Nullable Runnable onFinished) {
        toRun.run();

        if (onFinished != null)
            onFinished.run();
    }


    private static void renderFirstPersonWorldParts(WorldRenderContext context) {
        if (context.camera().isThirdPerson())
            return;

        PlayerData data = PlayerDataManager.localPlayer;

        if (data != null && data.lastEntity != null) {
            FiguraMod.currentData = data;

            context.matrixStack().push();

            try {
                if (data.model != null) {
                    int prevCount = data.model.leftToRender;
                    data.model.leftToRender = Integer.MAX_VALUE - 100;

                    if (FiguraMod.vertexConsumerProvider != null) {
                        Vec3d camera = context.camera().getPos();
                        data.model.renderWorldParts(data, camera.x, camera.y, camera.z, context.matrixStack(), data.getVCP(), MinecraftClient.getInstance().getEntityRenderDispatcher().getLight(data.lastEntity, context.tickDelta()), OverlayTexture.DEFAULT_UV, 1f);
                    }

                    data.model.leftToRender = prevCount;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            context.matrixStack().pop();
            FiguraMod.clearRenderingData();
        }
    }

    public static void sendToast(Object title, Object message) {
        Text text = title instanceof Text t ? t : new TranslatableText(title.toString());
        Text text2 = message instanceof Text m ? m : new TranslatableText(message.toString());

        MinecraftClient.getInstance().getToastManager().clear();
        MinecraftClient.getInstance().getToastManager().add(new FiguraToast(text, text2));
    }

    public static void getLatestModVersion() {
        //no updates pls
        if ((int) (Config.RELEASE_CHANNEL.value) == 2) {
            latestVersionStatus = 0;
            return;
        }

        doTask(() -> {
            try {
                StringBuilder output = new StringBuilder();
                URLConnection connection = new URL(GRADLE_PROPERTIES_LINK).openConnection();
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line;
                while((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
                reader.close();
                String versionFileContents = output.toString();
                int versionPos = versionFileContents.indexOf("mod_version");
                int nextLinePos = versionFileContents.indexOf("\n", versionPos);
                latestVersion = versionFileContents.substring(versionPos, nextLinePos).replaceAll(" ", "").substring(12);

                //only full releases :3
                if ((int) (Config.RELEASE_CHANNEL.value) == 1)
                    latestVersion = latestVersion.split("-", 2)[0];

                SemanticVersion latest = SemanticVersion.parse(latestVersion);
                SemanticVersion current = SemanticVersion.parse(MOD_VERSION);

                latestVersionStatus = current.compareTo((Version) latest);
            } catch (Exception e) {
                latestVersionStatus = 0;
                e.printStackTrace();
            }
        });
    }

    public static Style getAccentColor(Style style) {
        return style.withColor((int) Config.ACCENT_COLOR.value);
    }

    public final static List<UUID> VIP = List.of(
            UUID.fromString("aa0e3391-e497-4e8e-8afe-b69dfaa46afa"), //salad
            UUID.fromString("da53c608-d17c-4759-94fe-a0317ed63876"), //zandra
            UUID.fromString("66a6c5c4-963b-4b73-a0d9-162faedd8b7f"), //fran
            UUID.fromString("45361fcf-f188-46de-ae96-43d89afd6658"), //money
            UUID.fromString("d47ce8af-b942-47de-8790-f602241531e3"), //omo
            UUID.fromString("0d04770a-9482-4a39-8011-fcbb7c99b8e1"), //lily
            UUID.fromString("10c4a29c-78fd-428c-bf51-f70b93e2ab45")  //rambles
    );
}
