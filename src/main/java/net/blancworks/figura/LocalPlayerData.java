package net.blancworks.figura;


import com.google.common.io.CharStreams;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.blancworks.figura.lua.CustomScript;
import net.blancworks.figura.models.CustomModel;
import net.blancworks.figura.models.FiguraTexture;
import net.blancworks.figura.models.parsers.BlockbenchModelDeserializer;
import net.blancworks.figura.models.shaders.FiguraVertexConsumerProvider;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.util.Identifier;

import java.io.*;
import java.nio.file.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * This is the specific class used for the LOCAL player.
 * This is in place to allow users to freely modify their model based on files loaded from disk,
 * and allow for easier editing.
 */
public class LocalPlayerData extends PlayerData {
    public String loadedName;
    private final Map<String, WatchKey> watchKeys = new Object2ObjectOpenHashMap<>();
    private final Set<String> watchedFiles = new HashSet<>();
    public static WatchService ws;
    public NbtCompound modelData;

    static {
        try {
            ws = FileSystems.getDefault().newWatchService();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void tick() {
        if (this.loadedName != null)
            this.lastHash = "";
        super.tick();

        this.tickFileWatchers();
    }

    @Override
    public long getFileSize() {
        if (modelData == null)
            return super.getFileSize();

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            DataOutputStream w = new DataOutputStream(out);

            NbtIo.writeCompressed(modelData, w);
            return w.size();
        } catch (Exception ignored) {}

        return 0;
    }

    public static Path getContentDirectory() {
        return FiguraMod.getModContentDirectory().resolve("model_files");
    }

    /**
     * Loads a model file at a specific directory.
     *
     * @param fileName - the file to load
     */
    public void loadModelFile(String fileName) {
        //clear current data
        this.model = null;
        this.texture = null;
        this.script = null;

        extraTextures.clear();

        KeyBinding.updateKeysByCode();
        watchedFiles.clear();

        if (fileName == null) {
            packAvatarData();
            return;
        }

        //create root directory
        Path contentDirectory = getContentDirectory();

        try {
            Files.createDirectories(contentDirectory);
        } catch (Exception e) {
            e.printStackTrace();
        }

        //check file type
        boolean isZip = fileName.endsWith(".zip");

        //avatar file
        File file = new File(contentDirectory.resolve(fileName).toString());

        //loading stuff
        //1 - model | 2 - player model | 4 - texture | 8 - script | 16 - render layers
        byte data = 0;
        HashMap<String, Path> avatarPaths = new HashMap<>();

        //zip
        if (isZip) {
            //add zip to watched files, even if you cant edit opened zip files, you might be able to
            watchedFiles.add(file.toString());

            try {
                ZipFile zipFile = new ZipFile(file.getPath());

                if (zipFile.getEntry("model.bbmodel") != null) data = (byte) (data | 1);
                if (zipFile.getEntry("player_model.bbmodel") != null) data = (byte) (data | 2);
                if (zipFile.getEntry("texture.png") != null) data = (byte) (data | 4);
                if (zipFile.getEntry("script.lua") != null) data = (byte) (data | 8);
                if (zipFile.getEntry("render_layers.json") != null) data = (byte) (data | 16);
            } catch (Exception e) {
                e.printStackTrace();
                data = 0;
            }
        }
        //folder
        else {
            //set root directory
            contentDirectory = file.toPath();

            //set paths
            Path modelPath = contentDirectory.resolve("model.bbmodel");
            Path playerModelPath = contentDirectory.resolve("player_model.bbmodel");
            Path texturePath = contentDirectory.resolve("texture.png");
            Path scriptPath = contentDirectory.resolve("script.lua");
            Path renderLayersPath = contentDirectory.resolve("render_layers.json");

            //add watchedfiles
            watchedFiles.add(modelPath.toString());
            watchedFiles.add(playerModelPath.toString());
            watchedFiles.add(texturePath.toString());
            watchedFiles.add(scriptPath.toString());
            watchedFiles.add(renderLayersPath.toString());

            //load!
            if (Files.exists(modelPath)) data = (byte) (data | 1);
            if (Files.exists(playerModelPath)) data = (byte) (data | 2);
            if (Files.exists(texturePath)) data = (byte) (data | 4);
            if (Files.exists(scriptPath)) data = (byte) (data | 8);
            if (Files.exists(renderLayersPath)) data = (byte) (data | 16);

            //add to hash map
            avatarPaths.put("model", modelPath);
            avatarPaths.put("player_model", playerModelPath);
            avatarPaths.put("texture", texturePath);
            avatarPaths.put("script", scriptPath);
            avatarPaths.put("render_layers", renderLayersPath);
        }

        //log and clear player model
        if (data == 0 || data == 4) {
            FiguraMod.LOGGER.warn("Failed to load model " + fileName);
            PlayerDataManager.clearLocalPlayer();
            return;
        }

        //add directory to watched files
        if (!watchKeys.containsKey(contentDirectory.toString())) {
            try {
                watchKeys.put(contentDirectory.toString(), contentDirectory.register(ws, StandardWatchEventKinds.ENTRY_MODIFY));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        //set loaded name
        this.loadedName = fileName;

        //Set up ZIP file.
        ZipFile modelZip = null;

        try {
            if (isZip)
                modelZip = new ZipFile(file);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        //try to load JSON model
        if ((data & 1) == 1 || (data & 2) == 2) loadModel((data & 1) == 1, avatarPaths, isZip, modelZip);

        //try to load main texture
        if ((data & 4) == 4) loadTexture(avatarPaths.get("texture"), isZip, modelZip);

        //try to load render_layers
        if ((data & 16) == 16) loadRenderLayers(avatarPaths.get("render_layers"), isZip, modelZip, file.toPath());

        //try to load script
        if ((data & 8) == 8) loadScript(avatarPaths.get("script"), isZip, modelZip);

        //try to load extra textures
        loadExtraTextures(file, isZip, modelZip);

        //Close ZIP stream.
        try {
            if (isZip)
                modelZip.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        packAvatarData();
    }

    public void loadModel(boolean model, HashMap<String, Path> paths, boolean isZip, ZipFile modelZip) {
        InputStream inputStream = null;

        try {
            //get input stream, either from zip, or directory
            if (isZip) {
                ZipEntry modelEntry;
                if (model)
                    modelEntry = modelZip.getEntry("model.bbmodel");
                else {
                    modelEntry = modelZip.getEntry("player_model.bbmodel");
                    BlockbenchModelDeserializer.overrideAsPlayerModel = true;
                }

                inputStream = modelZip.getInputStream(modelEntry);
            } else {
                if (model)
                    inputStream = new FileInputStream(paths.get("model").toFile());
                else {
                    inputStream = new FileInputStream(paths.get("player_model").toFile());
                    BlockbenchModelDeserializer.overrideAsPlayerModel = true;
                }
            }

            //Try to read from input stream
            String modelJsonText;
            try (final Reader reader = new InputStreamReader(inputStream)) {
                modelJsonText = CharStreams.toString(reader);
            }

            //Finalize string for lambda
            String finalModelJsonText = modelJsonText;
            //Load model from GSON in off-thread.
            FiguraMod.doTask(() -> {
                this.model = FiguraMod.GSON.fromJson(finalModelJsonText, CustomModel.class);
                this.model.owner = this;
                this.model.isDone = true;
                FiguraMod.LOGGER.info("Model Loading Finished");
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

        //Close previously used stream, regardless of what it was (zip or filestream)
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void loadTexture(Path texturePath, boolean isZip, ZipFile modelZip) {
        try {
            //Generate Identifier for texture.
            Identifier id = new Identifier("figura", playerId.toString());

            //Create new texture, and register it.
            this.texture = new FiguraTexture();

            //load texture, if any
            this.texture.id = id;
            getTextureManager().registerTexture(id, texture);

            //Get input stream, either from file, or from zip.
            InputStream inputStream;
            if (isZip)
                inputStream = modelZip.getInputStream(modelZip.getEntry("texture.png"));
            else
                inputStream = new FileInputStream(texturePath.toFile());

            //Load texture (tasks are managed by the texture itself)
            texture.loadFromStream(inputStream);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadScript(Path scriptPath, boolean isZip, ZipFile modelZip) {
        InputStream inputStream = null;

        try {
            //Get input stream, either from file, or from zip.
            if (isZip) {
                //Get entry
                ZipEntry fileEntry = modelZip.getEntry("script.lua");
                //If there is an script entry
                if (fileEntry != null)
                    inputStream = modelZip.getInputStream(fileEntry);
            } else if (Files.exists(scriptPath)) {
                inputStream = new FileInputStream(scriptPath.toFile());
            }

            //If there is a script, try to load it
            if (inputStream != null) {
                //Try to read from input stream
                String scriptSource;
                try (final Reader reader = new InputStreamReader(inputStream)) {
                    scriptSource = CharStreams.toString(reader);
                }

                //Create script.
                this.script = new CustomScript();

                //Finalize script source for lambda.
                String finalScriptSource = scriptSource;
                //Load script on off-thread.
                FiguraMod.doTask(() -> this.script.load(this, finalScriptSource));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        //Close previously used stream, regardless of what it was (zip or filestream)
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void loadRenderLayers(Path renderLayersPath, boolean isZip, ZipFile modelZip, Path avatarFolder) {
        //Code mostly copied from above function, loadScript()
        InputStream inputStream = null;
        try {
            if (isZip) {
                ZipEntry fileEntry = modelZip.getEntry("render_layers.json");
                if (fileEntry != null)
                    inputStream = modelZip.getInputStream(fileEntry);
            } else if (Files.exists(renderLayersPath)) {
                inputStream = new FileInputStream(renderLayersPath.toFile());
            }

            InputStream finalInputStream = inputStream;
            if (inputStream != null) {
                FiguraMod.doTask(() -> {
                    FiguraVertexConsumerProvider.parseLocal(this, finalInputStream, avatarFolder);
                    try {
                        finalInputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadExtraTextures(File file, boolean isZip, ZipFile modelZip) {
        try {
            InputStream inputStream = null;

            for (FiguraTexture.TextureType textureType : FiguraTexture.EXTRA_TEXTURE_TO_RENDER_LAYER.keySet()) {
                Path location = null;

                //If this is a zip file
                if (isZip) {
                    //Get entry
                    ZipEntry fileEntry = modelZip.getEntry("texture" + textureType.toString() + ".png");
                    //If there is an entry that matches this texture
                    if (fileEntry != null)
                        inputStream = modelZip.getInputStream(fileEntry);
                }
                //then its a directory
                else {
                    location = file.toPath().resolve("texture" + textureType.toString() + ".png");

                    //If file exists at that location, make a stream for it, and set it to be watched.
                    if (Files.exists(location)) {
                        inputStream = new FileInputStream(location.toFile());
                        watchedFiles.add(location.toString());
                    }
                }

                //If there IS a stream for this extra texture
                if (inputStream != null) {
                    FiguraTexture extraTexture = new FiguraTexture();
                    extraTexture.id = new Identifier("figura", playerId.toString() + textureType);
                    extraTexture.filePath = location;
                    getTextureManager().registerTexture(extraTexture.id, extraTexture);
                    extraTexture.type = textureType;

                    extraTextures.add(extraTexture);

                    extraTexture.loadFromStream(inputStream);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void tickFileWatchers() {

        boolean doReload = false;

        for (Map.Entry<String, WatchKey> entry : watchKeys.entrySet()) {
            WatchKey key = watchKeys.get(entry.getKey());

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

                Path parentPath = FileSystems.getDefault().getPath(entry.getKey());
                Path child = parentPath.resolve(filename);
                String realName = child.getFileName().toString();

                try {

                    if (watchedFiles.contains(child.toString()))
                        doReload = true;

                    if (realName.equals(loadedName) && !doReload)
                        doReload = true;

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        if (doReload) {
            watchKeys.clear();

            PlayerDataManager.lastLoadedFileName = loadedName;
            loadModelFile(loadedName);
            isLocalAvatar = true;
        }
    }

    public void packAvatarData() {
        //pack avatar on load
        FiguraMod.doTask(() -> {
            NbtCompound nbt = new NbtCompound();
            this.modelData = this.writeNbt(nbt) ? nbt : null;
            getFileSize();
        });
    }
}
