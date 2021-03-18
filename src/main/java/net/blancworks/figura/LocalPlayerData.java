package net.blancworks.figura;


import com.google.common.io.CharStreams;
import net.blancworks.figura.models.CustomModel;
import net.blancworks.figura.models.FiguraTexture;
import net.blancworks.figura.models.lua.CustomScript;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.PositionTracker;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.Level;

import java.io.*;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

//This is the specific class used for the LOCAL player.
//This is in place to allow users to freely modify their model based on files loaded from disk,
//and allow for easier editing.
public class LocalPlayerData extends PlayerData {


    private Path texturePath = null;
    private boolean didTextureLoad = false;

    private String loadedName;
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
        
        if(loadedName != null)
            lastHash = "";
        super.tick();

        lateLoadTexture();
        tickFileWatchers();
    }
    

    public static Path getContentDirectory(){
        return FiguraMod.getModContentDirectory().resolve("model_files");
    }
    
    //Loads a model file at a specific directory.
    public void loadModelFile(String fileName) {
        Path contentDirectory = getContentDirectory();
        Path jsonPath = contentDirectory.resolve(fileName + ".bbmodel");
        texturePath = contentDirectory.resolve(fileName + ".png");
        Path scriptPath = contentDirectory.resolve(fileName + ".lua");
        
        try {
            Files.createDirectories(contentDirectory);
        } catch (Exception e){
            e.printStackTrace();
        }

        //Custom models can exist without scripts, but not without a texture and a json file.
        if (!Files.exists(jsonPath) || !Files.exists(texturePath))
            return;

        if (!watchKeys.containsKey(contentDirectory.toString())) {
            try {
                watchKeys.put(contentDirectory.toString(), contentDirectory.register(ws, StandardWatchEventKinds.ENTRY_MODIFY));
            } catch (Exception e) {
                FiguraMod.LOGGER.log(Level.ERROR, e);
            }
        }

        loadedName = fileName;

        //Load JSON file for model.
        String text = null;
        try {
            InputStream stream = new FileInputStream(jsonPath.toFile());
            try (final Reader reader = new InputStreamReader(stream)) {
                text = CharStreams.toString(reader);
            }

            CustomModel mdl = FiguraMod.builder.fromJson(text, CustomModel.class);
            model = mdl;
            mdl.owner = this;
        } catch (Exception e) {
            e.printStackTrace();
        }


        //Load texture.
        try {
            Identifier id = new Identifier("figura", playerId.toString());
            texture = new FiguraTexture();
            texture.id = id;
            texture.filePath = texturePath;
            getTextureManager().registerTexture(id, texture);

            texturePath = texturePath;
            didTextureLoad = true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        //Load script.
        try {
            String contents = new String(Files.readAllBytes(scriptPath));

            script = new CustomScript(this, contents);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadModelFileNBT(String fileName){
        Path contentDirectory = getContentDirectory();
        Path filePath = contentDirectory.resolve(fileName);
        
        if(!Files.exists(filePath))
            return;

        try {
            FileInputStream fis = new FileInputStream(filePath.toFile());
            DataInputStream dis = new DataInputStream(fis);
            PositionTracker positionTracker = new PositionTracker(999999999);
            CompoundTag nbtTag = CompoundTag.READER.read(dis, 0, positionTracker);
            
            fromNBT(nbtTag);
        } catch (Exception e){
            FiguraMod.LOGGER.log(Level.ERROR, e);
        }
    }

    public void loadModelFileNBT(DataInputStream stream){
        try {
            super.loadFromNBT(stream);
        } catch (Exception e){
            FiguraMod.LOGGER.log(Level.ERROR, e);
        }
    }
    
    //Loads the texture late, once it's been actually registered.
    public void lateLoadTexture() {
        if (didTextureLoad) {
            didTextureLoad = false;
            //Create async task to load model.
            CompletableFuture.runAsync(() -> {
                try {
                    texture.load(texturePath);
                    texture.ready = true;
                } catch (Exception e) {
                    FiguraMod.LOGGER.log(Level.ERROR, e);
                    return;
                }
            }, Util.getMainWorkerExecutor());
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
                    if (realName.equals(loadedName) && doReload == false)
                        doReload = true;
                } catch (Exception e) {
                    System.err.println(e);
                    continue;
                }
            }
        }

        if (doReload == true) {
            watchKeys.clear();

            PlayerDataManager.lastLoadedFileName = loadedName;
            loadModelFile(loadedName);
        }

    }

}
