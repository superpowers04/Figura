package net.blancworks.figura;

import net.blancworks.figura.models.CustomModel;
import net.blancworks.figura.models.FiguraTexture;
import net.blancworks.figura.models.lua.CustomScript;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.PositionTracker;
import net.minecraft.util.Identifier;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;


//Responsible for storing all the data associated with the player on this client.
public class PlayerData {

    private static TextureManager textureManager;

    //ID of the player
    public UUID playerId;

    //The custom model associated with the player
    public CustomModel model;
    //The custom texture for the custom model
    public FiguraTexture texture;
    //The custom script for the model.
    public CustomScript script;
    //Vanilla model for the player, in case we need it for something.
    public PlayerEntityModel vanillaModel;

    public boolean isLoaded = false;
    public LoadType loadType = LoadType.NONE;

    public static TextureManager getTextureManager() {
        if (textureManager == null)
            textureManager = MinecraftClient.getInstance().getTextureManager();
        return textureManager;
    }

    //Turns this PlayerData into an NBT tag.
    //Used when saving to a file to upload, or just be compressed on-disk.
    public boolean toNBT(CompoundTag tag) {

        //You cannot save a model that is incomplete.
        if (model == null || texture == null || script == null)
            return false;
        
        tag.putIntArray("version", current_version);

        //Put ID.
        tag.putUuid("id", playerId);

        //Put Model.
        CompoundTag modelTag = new CompoundTag();
        model.toNBT(modelTag);
        tag.put("model", modelTag);

        //Put Texture.
        try {
            CompoundTag textureTag = new CompoundTag();
            texture.toNBT(textureTag);
            tag.put("texture", textureTag);
        } catch (Exception e) {
            System.out.println(e);
            return false;
        }

        //Put Script.
        CompoundTag scriptTag = new CompoundTag();
        script.toNBT(scriptTag);
        tag.put("script", scriptTag);

        return true;
    }

    //Loads a PlayerData from the given NBT tag.
    public void fromNBT(CompoundTag tag) {
        
        int[] version = tag.getIntArray("version");
        
        playerId = tag.getUuid("id");
        
        //VERSION CHECKING.
        if(version != null) {
            boolean success = compareVersions(version);
            
            if(!success)
                return;
        }
        
        try {
            CompoundTag modelTag = (CompoundTag) tag.get("model");
            model = new CustomModel();
            model.fromNBT(modelTag);
            model.owner = this;

            CompoundTag textureTag = (CompoundTag) tag.get("texture");
            texture = new FiguraTexture();
            texture.id = new Identifier("figura", playerId.toString());
            getTextureManager().registerTexture(texture.id, texture);
            texture.fromNBT(textureTag);

            if (tag.contains("script")) {
                CompoundTag scriptTag = (CompoundTag) tag.get("script");
                
                script = new CustomScript();
                script.fromNBT(this, scriptTag);
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }


    //Ticks from client.
    public void tick() {

        if (!isLoaded) {
            tickLoads();
            return;
        }


        if (script != null) {
            script.runFunction("tick", CustomScript.max_lua_instructions_tick);
        }
    }

    public void tickLoads() {

        switch (loadType) {
            case NONE:
                loadType = LoadType.LOCAL;
                break;
            case LOCAL:
                try {
                    loadLocal();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case SERVER:
                try {
                    loadServer();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
        }
    }

    //Attempts to load assets locally off of disk.
    protected void loadLocal() throws Exception {
        Path localPath = FabricLoader.getInstance().getGameDir().getParent().resolve("model_files").resolve("cache").resolve(playerId.toString() + ".nbt");

        //If no local cache exists for the player in question, we switch to server loading.
        if (!Files.exists(localPath)) {
            loadType = LoadType.SERVER;
            return;
        }

        loadFromNBTFile(localPath);
        isLoaded = true;
    }

    //Attempts to load assets off of the server
    protected void loadServer() {
        loadType = LoadType.NONE;


        isLoaded = true;
        return;
    }


    public void loadFromNBTFile(Path path) throws Exception {
        DataInputStream input = new DataInputStream(new FileInputStream(path.toFile()));
        loadFromNBT(input);
    }

    public void loadFromNBT(DataInputStream input) throws Exception {
        PositionTracker tracker = new PositionTracker(99999999);
        CompoundTag tag = CompoundTag.READER.read(input, 0, tracker);
    }
    
    
    //VERSION
    //FORMAT IS
    //0 = mega version for huge api changes to the fundamentals of the loading system
    //1 = major version, for compatibility-breaking api changes
    //2 = minor version, for non-compat breaking api changes
    static final int[] current_version = new int[3];
    
    static {
        current_version[0] = 0;
        current_version[1] = 0;
        current_version[2] = 1;
    }
    
    public boolean compareVersions(int[] version){
        if(version[0] != current_version[0]){
            System.out.printf("MEGA VERSION DIFFERENCE BETWEEN FILE VERSION (%i-%i-%i) AND MOD VERSION (%i-%i-%i)",version[0],version[1],version[2],current_version[0],current_version[1],current_version[2]);
            return false;
        }
        if(version[1] != current_version[1]){
            System.out.printf("MAJOR VERSION DIFFERENCE BETWEEN FILE VERSION (%i-%i-%i) AND MOD VERSION (%i-%i-%i)",version[0],version[1],version[2],current_version[0],current_version[1],current_version[2]);
            return false;
        }
        return true;
    }

    public enum LoadType {
        NONE,
        LOCAL,
        SERVER
    }
}
