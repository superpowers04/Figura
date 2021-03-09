package net.blancworks.figura.models;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.blancworks.figura.models.parsers.BedrockModelDeserializer;
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


    //-----------Model-----------

    
    
    //OLD
    /*
    //Retrieve a custom model for a player entity.
    //Extracts the UUID from the player and uses that as a key to attempt to get them.
    //Returns null if no model is loaded, including if a network model has started but not finished loading.
    public static CustomModel getModelForPlayer(AbstractClientPlayerEntity playerEntity) {
        UUID player_key = playerEntity.getUuid();
        player_key = UUID.fromString("da53c608-d17c-4759-94fe-a0317ed63876");

        updateWatchers();

        //Try to access model from dictionary
        if (customModels.containsKey(player_key))
            return customModels.get(player_key);

        //Try to load a model from disk
        try {
            if (loadModelFromDisk(player_key))
                return customModels.get(player_key);
        } catch (Exception e) {
            FiguraMod.LOGGER.log(e.toString());
        }

        //Load a model from the server
        if (loadModelFromServer(player_key))
            return customModels.get(player_key);

        //Nothing could be loaded, return null.
        return null;
    }

    //Attempt to load the model from disk.
    //Searches for a model file with a name that matches the player UUID.
    public static boolean loadModelFromDisk(UUID uuid) throws IOException {

        //Check skinless cache.
        if (localNoModelPlayers.contains(uuid))
            return false;


        //Check location where models are stored.
        Path target_path = FabricLoader.getInstance().getGameDir().getParent().resolve("model_files").resolve(uuid.toString() + ".json");
        watchDirectory(target_path.getParent());
        if (!Files.exists(target_path)) {
            localNoModelPlayers.add(uuid);
            return false;
        }

        String content = new String(Files.readAllBytes(target_path));

        customModels.put(uuid, parseFromJson(content));

        return true;
    }

    //Attempt to load the model from the server
    //Never returns true, but kick-starts the process by which the model is pushed to memory.
    public static boolean loadModelFromServer(UUID uuid) {

        if (netNoModelPlayers.contains(uuid))
            return false;

        //If there is a load task currently being executed, return. We only load one model at a time.
        if (current_model_load_task != null && current_model_load_task.isDone() == false) {
            customModels.put(loaded_model_uuid, loaded_model);
            loaded_model_uuid = null;
            loaded_model = null;
            return false;
        }

        //No URL right now :/ Need a real place to load from first.
        //String url = "";

        //Create async task to load model.
        current_model_load_task = CompletableFuture.runAsync(() -> {
            HttpURLConnection httpURLConnection = null;

            //Connect to URL.
            try {
                httpURLConnection = (HttpURLConnection) (new URL(url)).openConnection(MinecraftClient.getInstance().getNetworkProxy());
                httpURLConnection.setDoInput(true);
                httpURLConnection.setDoOutput(false);
                httpURLConnection.connect();
                if (httpURLConnection.getResponseCode() / 100 == 2) {
                    //Read the entire page given back to us at this URL.
                    //Should be a json file.
                    loaded_model = parseFromJson(httpURLConnection.getInputStream());
                    loaded_model_uuid = uuid;

                    httpURLConnection.disconnect();
                    return;
                }
            } catch (Exception e) {
                FiguraMod.LOGGER.log(e);
                netNoModelPlayers.add(uuid);
                return;
            } finally {
                if (httpURLConnection != null) {
                    httpURLConnection.disconnect();
                }

            }
        }, Util.getMainWorkerExecutor());

        return false;
    }

    public static CustomModel parseFromJson(InputStream stream) throws IOException {
        String text = null;

        try (final Reader reader = new InputStreamReader(stream)) {
            text = CharStreams.toString(reader);
        }
        return parseFromJson(text);
    }

    public static CustomModel parseFromJson(String content) {
        return builder.fromJson(content, CustomModel.class);
    }*/


    //-----------Texture-----------

    //OLD
    /*
    public static Identifier getTextureForPlayer(AbstractClientPlayerEntity playerEntity) {
        UUID player_key = playerEntity.getUuid();
        player_key = UUID.fromString("da53c608-d17c-4759-94fe-a0317ed63876");
        
        if (customTextures.containsKey(player_key))
            return customTextures.get(player_key);

        try {
            if (loadTextureFromDisk(player_key))
                return customTextures.get(player_key);
        } catch (Exception e) {
            FiguraMod.LOGGER.log(e.toString());
        }

        if (loadTextureFromServer(player_key))
            return customTextures.get(player_key);

        return null;
    }

    public static boolean loadTextureFromDisk(UUID uuid) throws IOException {

        //Check skinless cache.
        if (localNoModelPlayers.contains(uuid))
            return false;

        //Check location where models are stored.
        Path target_path = FabricLoader.getInstance().getGameDir().getParent().resolve("model_files").resolve(uuid.toString() + ".png");
        watchDirectory(target_path.getParent());
        if (!Files.exists(target_path)) {
            localNoModelPlayers.add(uuid);
            return false;
        }

        Identifier id = new Identifier("figura", uuid.toString());
        FiguraTexture result = new FiguraTexture();
        getTextureManager().registerTexture(id, result);
        result.load(target_path);

        //If there's an error loading the image file, this player isn't renderable.
        if (result == null) {
            localNoModelPlayers.add(uuid);
            return false;
        }

        customTextures.put(uuid, id);

        return true;
    }

    public static boolean loadTextureFromServer(UUID uuid) {

        if (netNoModelPlayers.contains(uuid))
            return false;

        //If there is a load task currently being executed, return. We only load one model at a time.
        if (current_texture_load_task != null && current_texture_load_task.isDone() == false) {
            if (loaded_texture_id != null) {
                customTextures.put(loaded_texture_uuid, loaded_texture_id);
                FiguraMod.LOGGER.log("LOADED TEXTURE " + loaded_texture_id.toString() + " FROM NETWORK");
            }

            loaded_texture_uuid = null;
            loaded_texture_id = null;
            loaded_texture = null;
            return false;
        }

        //No URL right now :/ Need a real place to load from first.
        String url = "";

        Identifier id = new Identifier("figura", uuid.toString());
        FiguraTexture result = new FiguraTexture();
        getTextureManager().registerTexture(id, result);

        //Create async task to load model.
        current_texture_load_task = CompletableFuture.runAsync(() -> {
            HttpURLConnection httpURLConnection = null;

            //Connect to URL.
            try {
                httpURLConnection = (HttpURLConnection) (new URL(url)).openConnection(MinecraftClient.getInstance().getNetworkProxy());
                httpURLConnection.setDoInput(true);
                httpURLConnection.setDoOutput(false);
                httpURLConnection.connect();
                if (httpURLConnection.getResponseCode() / 100 == 2) {
                    //Read the entire page given back to us at this URL.
                    //Should be a json file.
                    result.load(httpURLConnection.getInputStream());

                    loaded_texture_uuid = uuid;
                    loaded_texture_id = id;
                    loaded_texture = result;

                    httpURLConnection.disconnect();
                    return;
                }
            } catch (Exception e) {
                FiguraMod.LOGGER.log(e);
                netNoModelPlayers.add(uuid);
                return;
            } finally {
                if (httpURLConnection != null) {
                    httpURLConnection.disconnect();
                }

            }
        }, Util.getMainWorkerExecutor());

        return false;
    }
*/

    //-----------Directory Watchers-----------

    
    //OLD
    /*
    private static void watchDirectory(Path path) throws IOException {
        if (!fileWatchKeys.containsKey(path.toString())) {
            fileWatchKeys.put(path.toString(), path.register(ws, StandardWatchEventKinds.ENTRY_MODIFY));
        }
    }
    
    private static void updateWatchers(){
        
        for(Map.Entry<String, WatchKey> entry : fileWatchKeys.entrySet()) {
            WatchKey key = fileWatchKeys.get(entry.getKey());

            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();

                // This key is registered only
                // for ENTRY_CREATE events,
                // but an OVERFLOW event can
                // occur regardless if events
                // are lost or discarded.
                if (kind == StandardWatchEventKinds.OVERFLOW) {
                    continue;
                }

                // The filename is the
                // context of the event.
                WatchEvent<Path> ev = (WatchEvent<Path>) event;
                Path filename = ev.context();

                // Verify that the new
                //  file is a text file.
                // Resolve the filename against the directory.
                // If the filename is "test" and the directory is "foo",
                // the resolved name is "test/foo".
                Path parentPath = FileSystems.getDefault().getPath(entry.getKey());
                Path child = parentPath.resolve(filename);
                String realName = FilenameUtils.removeExtension(child.getFileName().toString());

                try {
                    UUID id = UUID.fromString(realName);

                    if (customModels.containsKey(id))
                        customModels.remove(id);
                    if (customTextures.containsKey(id))
                        customTextures.remove(id);

                    FiguraMod.LOGGER.log("RELOADED UUID " + id.toString());
                } catch (Exception e) {
                    System.err.println(e);
                    continue;
                }

            }
        }
    }
    */
}
