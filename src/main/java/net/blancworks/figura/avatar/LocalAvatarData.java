package net.blancworks.figura.avatar;

import com.google.common.io.CharStreams;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.blancworks.figura.FiguraMod;
import net.blancworks.figura.lua.CustomScript;
import net.blancworks.figura.lua.api.sound.FiguraSoundManager;
import net.blancworks.figura.models.CustomModel;
import net.blancworks.figura.models.FiguraTexture;
import net.blancworks.figura.parsers.BlockbenchModelDeserializer;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.util.Identifier;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * This is the specific class used for the LOCAL player.
 * This is in place to allow users to freely modify their model based on files loaded from disk,
 * and allow for easier editing.
 */
public class LocalAvatarData extends AvatarData {
    public String loadedName;
    private final Map<String, WatchKey> watchKeys = new Object2ObjectOpenHashMap<>();
    private final Set<String> watchedFiles = new HashSet<>();
    public static WatchService ws;

    static {
        try {
            ws = FileSystems.getDefault().newWatchService();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public LocalAvatarData(UUID id) {
        super(id);
    }

    @Override
    public void tick() {
        super.tick();
        this.tickFileWatchers();
    }

    @Override
    public void loadFromNbt(NbtCompound tag) {
        loadedName = null;
        AvatarDataManager.localPlayerPath = null;
        AvatarDataManager.localPlayerNbt = tag;
        super.loadFromNbt(tag);
    }

    public static Path getContentDirectory() {
        return FiguraMod.getModContentDirectory().resolve("model_files");
    }

    /**
     * Loads a model file at a specific directory.
     *
     * @param path - the full file path to load
     */
    public void loadModelFile(String path) {
        //clear current data
        this.model = null;
        this.texture = null;
        this.script = null;
        AvatarDataManager.localPlayerNbt = null;

        watchedFiles.clear();
        clearData();

        if (path == null || path.equals(""))
            return;

        //avatar file
        File file = new File(path);

        //set loaded name
        loadedName = file.getName() + "§r";
        AvatarDataManager.localPlayerPath = path;

        //set root directory
        Path contentDirectory = Path.of(file.getParent());

        //check file type
        boolean isZip = path.endsWith(".zip");

        //loading stuff
        //1 - model | 2 - player model | 4 - texture | 8 - script
        byte data = 0;
        HashMap<String, Path> avatarPaths = new HashMap<>();

        //figura avatar data
        if (path.endsWith(".moon")) {
            watchedFiles.add(file.toString());

            try {
                FileInputStream fis = new FileInputStream(file);
                NbtCompound getTag = NbtIo.readCompressed(fis);

                loadFromNbt(getTag);

                fis.close();
                return;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        //zip
        else if (isZip) {
            //add zip to watched files, even if you cant edit opened zip files, you might be able to
            watchedFiles.add(file.toString());

            try {
                ZipFile zipFile = new ZipFile(file.getPath());

                if (zipFile.getEntry("model.bbmodel") != null) data = (byte) (data | 1);
                if (zipFile.getEntry("player_model.bbmodel") != null) data = (byte) (data | 2);
                if (zipFile.getEntry("texture.png") != null) data = (byte) (data | 4);
                if (zipFile.getEntry("script.lua") != null) data = (byte) (data | 8);
                if (zipFile.getEntry("sounds.json") != null) data = (byte) (data | 16);

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
            Path soundsPath = contentDirectory.resolve("sounds.json");

            //add watched files
            watchedFiles.add(modelPath.toString());
            watchedFiles.add(playerModelPath.toString());
            watchedFiles.add(texturePath.toString());
            watchedFiles.add(scriptPath.toString());
            watchedFiles.add(soundsPath.toString());

            //load!
            if (Files.exists(modelPath)) data = (byte) (data | 1);
            if (Files.exists(playerModelPath)) data = (byte) (data | 2);
            if (Files.exists(texturePath)) data = (byte) (data | 4);
            if (Files.exists(scriptPath)) data = (byte) (data | 8);
            if (Files.exists(soundsPath)) data = (byte) (data | 16);

            //add to hash map
            avatarPaths.put("model", modelPath);
            avatarPaths.put("player_model", playerModelPath);
            avatarPaths.put("texture", texturePath);
            avatarPaths.put("script", scriptPath);
            avatarPaths.put("sounds", soundsPath);

        }

        //log and clear player model
        if (data == 0 || data == 4) {
            FiguraMod.LOGGER.warn("Failed to load model " + path);
            AvatarDataManager.clearLocalPlayer();
            return;
        }

        //add directory to watched files
        if (!watchKeys.containsKey(path)) {
            try {
                watchKeys.put(path, contentDirectory.register(ws, StandardWatchEventKinds.ENTRY_MODIFY));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

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

        //try to load script
        if ((data & 8) == 8) {
            loadScript(avatarPaths.get("script"), isZip, modelZip);

            //try to load custom sounds (requires a script)
            if ((data & 16) == 16) loadCustomSounds(avatarPaths.get("sounds"), isZip, modelZip, file);
        }

        //try to load extra textures
        loadExtraTextures(file, isZip, modelZip);

        //Close ZIP stream.
        if (isZip) {
            ZipFile finalModelZip = modelZip;
            FiguraMod.doTask(() -> {
                try {
                    finalModelZip.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }

    public void loadModel(boolean model, HashMap<String, Path> paths, boolean isZip, ZipFile modelZip) {
        InputStream inputStream = null;

        try {
            boolean overrideAsPlayerModel = false;

            //get input stream, either from zip, or directory
            if (isZip) {
                ZipEntry modelEntry;
                if (model)
                    modelEntry = modelZip.getEntry("model.bbmodel");
                else {
                    modelEntry = modelZip.getEntry("player_model.bbmodel");
                    overrideAsPlayerModel = true;
                }

                inputStream = modelZip.getInputStream(modelEntry);
            } else {
                if (model)
                    inputStream = new FileInputStream(paths.get("model").toFile());
                else {
                    inputStream = new FileInputStream(paths.get("player_model").toFile());
                    overrideAsPlayerModel = true;
                }
            }

            //try to read from input stream
            String modelJsonText;
            try (final Reader reader = new InputStreamReader(inputStream)) {
                modelJsonText = CharStreams.toString(reader);
            }

            //finalize variables for lambda
            String finalModelJsonText = modelJsonText;
            boolean finalOverrideAsPlayerModel = overrideAsPlayerModel;

            //load model from GSON in off-thread
            FiguraMod.doTask(() -> {
                try {
                    this.model = new CustomModel(BlockbenchModelDeserializer.deserialize(finalModelJsonText, finalOverrideAsPlayerModel), this);
                    FiguraMod.LOGGER.info("Model Loading Finished");
                } catch (Exception e) {
                    e.printStackTrace();
                }
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
            Identifier id = new Identifier("figura", entityId.toString());

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
                scriptSource = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

                //Finalize script source for lambda.
                String finalScriptSource = scriptSource;
                //Load script on off-thread.
                FiguraMod.doTask(() -> {
                    try {
                        //Create script.
                        this.script = new CustomScript();
                        this.script.load(this, finalScriptSource);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
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

    public void loadCustomSounds(Path sounds, boolean isZip, ZipFile zip, File modelFile) {
        try {
            JsonElement soundsJson;
            if (isZip)
                soundsJson = JsonParser.parseReader(new InputStreamReader(zip.getInputStream(zip.getEntry("sounds.json"))));
            else
                soundsJson = JsonParser.parseReader(new FileReader(sounds.toFile()));

            JsonArray soundsArray = soundsJson.getAsJsonArray();
            soundsArray.forEach(entry -> {
                String name = entry.getAsString();
                String path = "sounds/" + name + ".ogg";

                FiguraMod.doTask(() -> {
                    try {
                        InputStream str = isZip ? zip.getInputStream(zip.getEntry(path)) : new FileInputStream(modelFile.toPath().resolve(path).toFile());
                        FiguraSoundManager.registerCustomSound(script, name, str.readAllBytes(), false);
                    } catch (Exception e) {
                        FiguraMod.LOGGER.error("failed to load custom song \"" + path + "\"");
                        e.printStackTrace();
                    }
                });
            });
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
                    extraTexture.id = new Identifier("figura", entityId.toString() + textureType);
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

                // The filename is the context of the event
                Path filename = (Path) event.context();

                Path parentPath = FileSystems.getDefault().getPath(entry.getKey());
                Path child = parentPath.resolve(filename);
                String realName = child.getFileName().toString();

                try {
                    if (watchedFiles.contains(child.toString()) || realName.equals(loadedName))
                        doReload = true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        if (doReload) reloadAvatar();
    }

    public void reloadAvatar() {
        watchKeys.clear();
        clearData();

        if (AvatarDataManager.localPlayerPath != null)
            loadModelFile(AvatarDataManager.localPlayerPath);
        else if (AvatarDataManager.localPlayerNbt != null)
            loadFromNbt(AvatarDataManager.localPlayerNbt);
        isLocalAvatar = true;
    }
}
