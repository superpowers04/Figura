package net.blancworks.figura.models;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.blancworks.figura.parsers.BedrockModelDeserializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;


//DEPRECATED
//DEPRECATED
//DEPRECATED
//DEPRECATED
//DEPRECATED
//DEPRECATED

@Deprecated
public class FiguraModelManager {

    //GSON instance used to load model json files.
    private static final Gson builder = new GsonBuilder().registerTypeAdapter(CustomModel.class, new BedrockModelDeserializer()).setPrettyPrinting().create();
    private static TextureManager textureManager;

    //Stores player UUID that don't have local models so we don't infinitely check for them.
    //Saves on IO ops.
    private static final HashSet<UUID> localNoModelPlayers = new HashSet<>();
    private static final HashSet<UUID> netNoModelPlayers = new HashSet<>();
    
    public static HashMap<UUID, CustomModel> customModels = new HashMap<UUID, CustomModel>();
    public static HashMap<UUID, Identifier> customTextures = new HashMap<UUID, Identifier>();

    public static CompletableFuture<?> current_model_load_task = null;
    public static CompletableFuture<?> current_texture_load_task = null;


    public static UUID loaded_model_uuid;
    public static CustomModel loaded_model;

    public static UUID loaded_texture_uuid;
    public static Identifier loaded_texture_id;
    public static FiguraTexture loaded_texture;

    public static WatchService ws;

    static {
        try {
            ws = FileSystems.getDefault().newWatchService();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //Keys for watching file changes for local files.
    public static HashMap<String, WatchKey> fileWatchKeys = new HashMap<String, WatchKey>();

    public static TextureManager getTextureManager() {
        if (textureManager == null)
            textureManager = MinecraftClient.getInstance().getTextureManager();
        return textureManager;
    }
}
