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
        Path contentDirectory = getContentDirectory();

        Path jsonPath = null;
        texturePath = null;
        texturePath = null;
        Path scriptPath = null;
        Path metadataPath = null;

        try {
            Files.createDirectories(contentDirectory);
        } catch (Exception e) {
            e.printStackTrace();
        }

        //remove "*" from file name if its .bbmodel
        if (fileName.endsWith("*")) {
            fileName = fileName.substring(0, fileName.length() - 1);
            fileName += ".bbmodel";
        }

        File file = new File(contentDirectory.resolve(fileName).toString());
        boolean isZip = file.getName().endsWith(".zip");

        //folder data
        if (file.isDirectory()) {
            jsonPath = file.toPath().resolve("model.bbmodel");
            texturePath = file.toPath().resolve("texture.png");
            scriptPath = file.toPath().resolve("script.lua");
            metadataPath = file.toPath().resolve("metadata.nbt");
        }
        //then must be a .bbmodel
        else if (!isZip) {
            fileName = fileName.substring(0, fileName.length() - 8);
            jsonPath = contentDirectory.resolve(fileName + ".bbmodel");
            texturePath = contentDirectory.resolve(fileName + ".png");
            scriptPath = contentDirectory.resolve(fileName + ".lua");
            metadataPath = contentDirectory.resolve(fileName + ".nbt");
        }

        if (!watchKeys.containsKey(contentDirectory.toString())) {
            try {
                watchKeys.put(contentDirectory.toString(), contentDirectory.register(ws, StandardWatchEventKinds.ENTRY_MODIFY));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        loadedName = fileName;

        //load JSON file for model
        model = null;
        String text;
        try {
            InputStream stream;
            if (!isZip) {
                stream = new FileInputStream(jsonPath.toFile());
            }
            else {
                ZipFile zipFile = new ZipFile(file);
                ZipEntry modelEntry = zipFile.getEntry("model.bbmodel");
                stream = zipFile.getInputStream(modelEntry);
            }

            try (final Reader reader = new InputStreamReader(stream)) {
                text = CharStreams.toString(reader);
            }

            CustomModel mdl = FiguraMod.builder.fromJson(text, CustomModel.class);
            model = mdl;
            mdl.owner = this;
        } catch (Exception e) {
            e.printStackTrace();
        }

        //load texture
        texture = null;
        try {
            Identifier id = new Identifier("figura", playerId.toString());
            texture = new FiguraTexture();
            texture.id = id;

            if (isZip) {
                ZipFile zipFile = new ZipFile(file);
                ZipEntry textureEntry = zipFile.getEntry("texture.png");
                InputStream stream = zipFile.getInputStream(textureEntry);

                Path tempDir = contentDirectory.getParent().resolve("temp");
                if (!Files.exists(tempDir)) Files.createDirectory(tempDir);

                Path tempTexture = tempDir.resolve("texture.temp");
                if (Files.exists(tempTexture)) Files.delete(tempTexture);
                Files.copy(stream, tempTexture);

                texturePath = tempTexture;
            }

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

            if (!isZip) {
                if (Files.exists(scriptPath))
                    contents = new String(Files.readAllBytes(scriptPath));
            }
            else {
                ZipFile zipFile = new ZipFile(file);
                ZipEntry scriptEntry = zipFile.getEntry("script.lua");

                if (scriptEntry != null) {
                    InputStream stream = zipFile.getInputStream(scriptEntry);
                    contents = new String(IOUtils.toByteArray(stream));
                }
            }

            if (contents != null)
                script = new CustomScript(this, contents);
            else
                FiguraMod.LOGGER.info("Model \"" + file.getName() + "\" doesn't have any valid scripts!");
        } catch (Exception e) {
            e.printStackTrace();
        }

        extraTextures.clear();
        try {
            for (FiguraTexture.TEXTURE_TYPE textureType : FiguraTexture.extraTexturesToRenderLayers.keySet()) {
                Path location;

                //zip
                if (isZip) {
                    ZipFile zipFile = new ZipFile(file);
                    ZipEntry textureEntry = zipFile.getEntry("texture" + textureType.toString() + ".png");

                    Path tempTexture = contentDirectory.getParent().resolve("temp").resolve("texture" + textureType.toString() + ".temp");
                    if (Files.exists(tempTexture)) Files.delete(tempTexture);

                    if (textureEntry != null) {
                        InputStream stream = zipFile.getInputStream(textureEntry);
                        Files.copy(stream, tempTexture);
                    }

                    location = tempTexture;
                }
                //folder
                else if (file.isDirectory()) {
                    location = file.toPath().resolve("texture" + textureType.toString() + ".png");
                }
                //.bbmodel
                else
                    location = contentDirectory.resolve(fileName + textureType.toString() + ".png");

                if (Files.exists(location)) {
                    FiguraTexture extraTexture = new FiguraTexture();
                    extraTexture.id = new Identifier("figura", playerId.toString() + textureType.toString());
                    extraTexture.filePath = location;
                    getTextureManager().registerTexture(extraTexture.id, extraTexture);
                    extraTexture.type = textureType;

                    extraTextures.add(extraTexture);
                    didTextureLoad = true;
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        extraTextures.clear();
        try {
            for (FiguraTexture.TEXTURE_TYPE textureType : FiguraTexture.extraTexturesToRenderLayers.keySet()) {
                Path location = contentDirectory.resolve(fileName + textureType.toString() + ".png");
                
                if(Files.exists(location)){
                    FiguraTexture extraTexture = new FiguraTexture();
                    extraTexture.id = new Identifier("figura", playerId.toString() + textureType.toString());
                    extraTexture.filePath = location;
                    getTextureManager().registerTexture(extraTexture.id, extraTexture);
                    extraTexture.type = textureType;
                    
                    extraTextures.add(extraTexture);
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

                // Verify that the new
                //  file is a text file.
                // Resolve the filename against the directory.
                // If the filename is "test" and the directory is "foo",
                // the resolved name is "test/foo".
                Path parentPath = FileSystems.getDefault().getPath(entry.getKey());
                Path child = parentPath.resolve(filename);
                String realName = FilenameUtils.removeExtension(child.getFileName().toString());

                try {
                    if (realName.equals(loadedName) && !doReload)
                        doReload = true;

                    if (!doReload) {
                        for (FiguraTexture extraTexture : extraTextures) {
                            if(realName.equals(loadedName + extraTexture.type)){
                                doReload = true;
                                break;
                            }
                        }
                    }
                } catch (Exception e) {
                    System.err.println(e);
                    continue;
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
