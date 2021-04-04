package net.blancworks.figura;


import com.google.common.io.CharStreams;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.blancworks.figura.lua.CustomScript;
import net.blancworks.figura.models.CustomModel;
import net.blancworks.figura.models.FiguraTexture;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.PositionTracker;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.file.*;
import java.sql.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
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

    static {
        try {
            ws = FileSystems.getDefault().newWatchService();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void tick() {
        this.isLoaded = true;

        this.lastHashCheckTime = new Date(Long.MAX_VALUE);
        if (this.loadedName != null)
            this.lastHash = "";
        super.tick();

        this.lateLoadTexture();
        this.tickFileWatchers();
    }

    public static Path getContentDirectory() {
        return FiguraMod.getModContentDirectory().resolve("model_files");
    }

    /**
     * Loads a model file at a specific directory.
     * @param fileName
     */
    public void loadModelFile(String fileName) {
        watchedFiles.clear();

        //create root directory
        Path contentDirectory = getContentDirectory();

        try {
            Files.createDirectories(contentDirectory);
        } catch (Exception e) {
            e.printStackTrace();
        }

        //check file type
        boolean isZip = fileName.endsWith(".zip");
        boolean isDirectory = !isZip && !fileName.endsWith("*");

        //reset paths
        Path jsonPath = null;
        Path texturePath = null;
        Path scriptPath = null;
        Path metadataPath;

        //dummy file - must be initialized
        File file = null;

        //folder data
        if (isDirectory) {
            file = new File(contentDirectory.resolve(fileName).toString());

            //set paths
            jsonPath = file.toPath().resolve("model.bbmodel");
            texturePath = file.toPath().resolve("texture.png");
            scriptPath = file.toPath().resolve("script.lua");
            metadataPath = file.toPath().resolve("metadata.nbt");

            //add watchedfiles
            watchedFiles.add(jsonPath.toString());
            watchedFiles.add(texturePath.toString());
            watchedFiles.add(scriptPath.toString());
            watchedFiles.add(metadataPath.toString());

            //set root directory
            contentDirectory = file.toPath();
        }
        //zip data
        else if (isZip) {
            //add zip to watched files, even if you cant edit opened zip files, you might be able to
            file = new File(contentDirectory.resolve(fileName).toString());
            watchedFiles.add(file.toString());
        }
        //then must be a .bbmodel *
        else {
            //remove invalid * from name
            fileName = fileName.substring(0, fileName.length() - 1);

            //set paths
            jsonPath = contentDirectory.resolve(fileName + ".bbmodel");
            texturePath = contentDirectory.resolve(fileName + ".png");
            scriptPath = contentDirectory.resolve(fileName + ".lua");
            metadataPath = contentDirectory.resolve(fileName + ".nbt");

            //add watched files
            watchedFiles.add(jsonPath.toString());
            watchedFiles.add(texturePath.toString());
            watchedFiles.add(scriptPath.toString());
            watchedFiles.add(metadataPath.toString());

            //add * back
            fileName += "*";
        }

        //check if files exists
        boolean cantLoad = !isZip && (!Files.exists(jsonPath) || !Files.exists(texturePath));

        //check for zip files
        if (isZip) {
            try {
                ZipFile zipFile = new ZipFile(file.getPath());

                boolean hasModel = zipFile.getEntry("model.bbmodel") != null;
                boolean hasTexture = zipFile.getEntry("texture.png") != null;

                cantLoad = !hasModel || !hasTexture;
            } catch (Exception e) {
                FiguraMod.LOGGER.debug(e.toString());
                cantLoad = true;
            }
        }

        //log and clear player model
        if (cantLoad) {
            FiguraMod.LOGGER.error("Failed to load model " + fileName);
            PlayerDataManager.clearLocalPlayer();
            return;
        }

        if (!watchKeys.containsKey(contentDirectory.toString())) {
            try {
                watchKeys.put(contentDirectory.toString(), contentDirectory.register(ws, StandardWatchEventKinds.ENTRY_MODIFY));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        //set loaded name
        this.loadedName = fileName;

        //load JSON model
        this.model = null;
        String text;
        try {
            InputStream stream;

            //if zip copy input stream, else, create a new from path file
            if (isZip) {
                ZipFile zipFile = new ZipFile(file);
                ZipEntry modelEntry = zipFile.getEntry("model.bbmodel");
                stream = zipFile.getInputStream(modelEntry);
            } else
                stream = new FileInputStream(jsonPath.toFile());

            //then read the input stream
            try (final Reader reader = new InputStreamReader(stream)) {
                text = CharStreams.toString(reader);
            }

            //close stream
            stream.close();

            CustomModel mdl = FiguraMod.GSON.fromJson(text, CustomModel.class);
            this.model = mdl;
            mdl.owner = this;
        } catch (Exception e) {
            e.printStackTrace();
        }

        //load texture
        this.texture = null;
        try {
            //start texture loading
            Identifier id = new Identifier("figura", playerId.toString());
            this.texture = new FiguraTexture();
            this.texture.id = id;

            //if zip pass the input stream to the texture and nulls the path
            if (isZip) {
                ZipFile zipFile = new ZipFile(file);
                ZipEntry textureEntry = zipFile.getEntry("texture.png");

                this.texture.inputStream = zipFile.getInputStream(textureEntry);
                texturePath = null;
            }

            //finish texture loading
            this.texture.filePath = texturePath;
            getTextureManager().registerTexture(id, texture);

        } catch (Exception e) {
            e.printStackTrace();
        }

        //load script
        this.script = null;
        try {
            String contents = null;

            //try to load script file inside the zip - if have one
            if (isZip) {
                ZipFile zipFile = new ZipFile(file);
                ZipEntry scriptEntry = zipFile.getEntry("script.lua");

                if (scriptEntry != null)
                    contents = new String(IOUtils.toByteArray(zipFile.getInputStream(scriptEntry)));
            }
            //then try to load from path
            else {
                if (Files.exists(scriptPath))
                    contents = new String(Files.readAllBytes(scriptPath));
            }

            //create script if found
            if (contents != null)
                this.script = new CustomScript(this, contents);

        } catch (Exception e) {
            e.printStackTrace();
        }

        //load extra textures
        extraTextures.clear();
        try {
            for (FiguraTexture.TextureType textureType : FiguraTexture.EXTRA_TEXTURE_TO_RENDER_LAYER.keySet()) {
                Path location;

                //zip is special because it only passes an input stream, if have one
                if (isZip) {
                    ZipFile zipFile = new ZipFile(file);
                    ZipEntry textureEntry = zipFile.getEntry("texture" + textureType.toString() + ".png");

                    if (textureEntry != null) {

                        FiguraTexture extraTexture = new FiguraTexture();
                        extraTexture.id = new Identifier("figura", playerId.toString() + textureType.toString());
                        extraTexture.filePath = null;
                        extraTexture.inputStream = zipFile.getInputStream(textureEntry);
                        getTextureManager().registerTexture(extraTexture.id, extraTexture);
                        extraTexture.type = textureType;

                        extraTextures.add(extraTexture);
                    }

                    continue;
                }
                //folder - just load from folder
                else if (isDirectory)
                    location = file.toPath().resolve("texture" + textureType.toString() + ".png");
                //.bbmodel - remove * from name then loads from root folder
                else
                    location = contentDirectory.resolve(fileName.substring(0, fileName.length() - 1) + textureType.toString() + ".png");

                //if location is valid, finish the loading
                if (Files.exists(location)) {
                    FiguraTexture extraTexture = new FiguraTexture();
                    extraTexture.id = new Identifier("figura", playerId.toString() + textureType.toString());
                    extraTexture.filePath = location;
                    getTextureManager().registerTexture(extraTexture.id, extraTexture);
                    extraTexture.type = textureType;

                    extraTextures.add(extraTexture);
                    watchedFiles.add(location.toString());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadModelFileNbt(String fileName) {
        Path contentDirectory = getContentDirectory();
        Path filePath = contentDirectory.resolve(fileName);

        if (!Files.exists(filePath))
            return;

        try {
            FileInputStream fis = new FileInputStream(filePath.toFile());
            DataInputStream dis = new DataInputStream(fis);
            PositionTracker positionTracker = new PositionTracker(999999999);
            CompoundTag nbt = CompoundTag.READER.read(dis, 0, positionTracker);

            readNbt(nbt);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadModelFileNbt(DataInputStream stream) {
        try {
            super.loadFromNbt(stream);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //Loads the texture late, once it's been actually registered.
    public void lateLoadTexture() {
        attemptTextureLoad(texture);

        for (FiguraTexture extraTexture : extraTextures) {
            attemptTextureLoad(extraTexture);
        }
    }

    public void attemptTextureLoad(FiguraTexture texture) {
        if (texture != null) {
            if (!texture.ready && !texture.isLoading) {
                texture.isLoading = true;

                //Create async task to load model.
                CompletableFuture.runAsync(() -> {
                    try {
                        texture.load(texture.filePath);
                        texture.ready = true;
                        texture.isLoading = false;
                    } catch (Exception e) {
                        e.printStackTrace();
                        return;
                    }
                }, Util.getMainWorkerExecutor());
                FiguraMod.LOGGER.debug("LOADED TEXTURE " + texture.id.toString());
            }
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
                    System.err.println(e);
                }
            }
        }

        if (doReload) {
            watchKeys.clear();

            PlayerDataManager.lastLoadedFileName = loadedName;
            loadModelFile(loadedName);
        }
    }
}
