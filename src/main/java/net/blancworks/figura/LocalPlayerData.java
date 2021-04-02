package net.blancworks.figura;


import com.google.common.io.CharStreams;
import net.blancworks.figura.lua.CustomScript;
import net.blancworks.figura.models.CustomModel;
import net.blancworks.figura.models.FiguraTexture;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.PositionTracker;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.file.*;
import java.sql.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

//This is the specific class used for the LOCAL player.
//This is in place to allow users to freely modify their model based on files loaded from disk,
//and allow for easier editing.
public class LocalPlayerData extends PlayerData {


    private Path texturePath = null;
    private boolean didTextureLoad = false;

    public String loadedName;
    private HashMap<String, WatchKey> watchKeys = new HashMap<String, WatchKey>();
    private HashSet<String> watchedFiles = new HashSet<String>();
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
        isLoaded = true;

        lastHashCheckTime = new Date(Long.MAX_VALUE);
        if (loadedName != null)
            lastHash = "";
        super.tick();

        lateLoadTexture();
        tickFileWatchers();
    }

    public static Path getContentDirectory() {
        return FiguraMod.getModContentDirectory().resolve("model_files");
    }

    //Loads a model file at a specific directory.
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
        boolean legacy = fileName.endsWith("*");
        boolean directory = !isZip && !legacy;

        //reset paths
        Path jsonPath = null;
        texturePath = null;
        Path scriptPath = null;
        Path metadataPath = null;

        //dummy file - must be initialized
        File file = null;

        //folder data
        if (directory) {
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

        if (!watchKeys.containsKey(contentDirectory.toString())) {
            try {
                watchKeys.put(contentDirectory.toString(), contentDirectory.register(ws, StandardWatchEventKinds.ENTRY_MODIFY));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        //set loaded name
        loadedName = fileName;

        //load JSON model
        model = null;
        String text;
        try {
            InputStream stream;

            //if zip copy input stream, else, create a new from path file
            if (isZip) {
                ZipFile zipFile = new ZipFile(file);
                ZipEntry modelEntry = zipFile.getEntry("model.bbmodel");
                stream = zipFile.getInputStream(modelEntry);
            }
            else
                stream = new FileInputStream(jsonPath.toFile());

            //then read the input stream
            try (final Reader reader = new InputStreamReader(stream)) {
                text = CharStreams.toString(reader);
            }

            //close stream
            stream.close();

            CustomModel mdl = FiguraMod.builder.fromJson(text, CustomModel.class);
            model = mdl;
            mdl.owner = this;
        } catch (Exception e) {
            e.printStackTrace();
        }

        //load texture
        texture = null;
        try {
            //start texture loading
            Identifier id = new Identifier("figura", playerId.toString());
            texture = new FiguraTexture();
            texture.id = id;

            //if zip pass the input stream to the texture and nulls the path
            if (isZip) {
                ZipFile zipFile = new ZipFile(file);
                ZipEntry textureEntry = zipFile.getEntry("texture.png");

                texture.inputStream = zipFile.getInputStream(textureEntry);
                texturePath = null;
            }

            //finish texture loading
            texture.filePath = texturePath;
            getTextureManager().registerTexture(id, texture);

            didTextureLoad = true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        //load script
        script = null;
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

            //create script if found or log an info that no scripts was loaded
            if (contents != null)
                script = new CustomScript(this, contents);
            else
                FiguraMod.LOGGER.info("Model \"" + fileName + "\" doesn't have any valid scripts!");
        } catch (Exception e) {
            e.printStackTrace();
        }

        //load extra textures
        extraTextures.clear();
        try {
            for (FiguraTexture.TEXTURE_TYPE textureType : FiguraTexture.extraTexturesToRenderLayers.keySet()) {
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
                        didTextureLoad = true;
                    }

                    continue;
                }
                //folder - just load from folder
                else if (directory)
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
                    didTextureLoad = true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadModelFileNBT(String fileName) {
        Path contentDirectory = getContentDirectory();
        Path filePath = contentDirectory.resolve(fileName);

        if (!Files.exists(filePath))
            return;

        try {
            FileInputStream fis = new FileInputStream(filePath.toFile());
            DataInputStream dis = new DataInputStream(fis);
            PositionTracker positionTracker = new PositionTracker(999999999);
            CompoundTag nbtTag = CompoundTag.READER.read(dis, 0, positionTracker);

            fromNBT(nbtTag);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadModelFileNBT(DataInputStream stream) {
        try {
            super.loadFromNBT(stream);
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
    public void attemptTextureLoad(FiguraTexture texture){
        if(texture != null) {
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
                    
                    if(watchedFiles.contains(child.toString()))
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
