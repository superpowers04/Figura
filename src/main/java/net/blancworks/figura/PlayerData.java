package net.blancworks.figura;

import net.blancworks.figura.lua.CustomScript;
import net.blancworks.figura.models.CustomModel;
import net.blancworks.figura.models.FiguraTexture;
import net.blancworks.figura.trust.PlayerTrustManager;
import net.blancworks.figura.trust.TrustContainer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Identifier;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.lang.reflect.Array;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
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

    public ArrayList<FiguraTexture> extraTextures = new ArrayList<FiguraTexture>();

    public PlayerEntity lastEntity;

    public boolean isLoaded = false;
    public LoadType loadType = LoadType.NONE;

    public Date lastHashCheckTime = new Date();
    public String lastHash = "";
    public boolean isInvalidated = false;

    private Identifier trustIdentifier;

    public Identifier getTrustIdentifier() {
        if (trustIdentifier == null)
            trustIdentifier = new Identifier("players", playerId.toString());
        return trustIdentifier;
    }

    public static TextureManager getTextureManager() {
        if (textureManager == null)
            textureManager = MinecraftClient.getInstance().getTextureManager();
        return textureManager;
    }

    //Turns this PlayerData into an NBT tag.
    //Used when saving to a file to upload, or just be compressed on-disk.
    public boolean toNBT(CompoundTag tag) {

        //You cannot save a model that is incomplete.
        if (model == null || texture == null)
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
            e.printStackTrace();
            return false;
        }

        if (script != null) {
            //Put Script.
            CompoundTag scriptTag = new CompoundTag();
            script.toNBT(scriptTag);
            tag.put("script", scriptTag);
        }

        if (extraTextures.size() > 0) {
            ListTag texList = new ListTag();

            for (FiguraTexture extraTexture : extraTextures) {
                CompoundTag etTag = new CompoundTag();
                extraTexture.toNBT(etTag);
                texList.add(etTag);
            }

            tag.put("exTexs", texList);
        }

        return true;
    }

    //Loads a PlayerData from the given NBT tag.
    public void fromNBT(CompoundTag tag) {

        int[] version = tag.getIntArray("version");

        playerId = tag.getUuid("id");

        //VERSION CHECKING.
        if (version != null) {
            boolean success = compareVersions(version);

            if (!success)
                return;
        }

        try {
            CompoundTag modelTag = (CompoundTag) tag.get("model");
            model = new CustomModel();
            model.fromNBT(modelTag);
            model.owner = this;
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            CompoundTag textureTag = (CompoundTag) tag.get("texture");
            texture = new FiguraTexture();
            texture.id = new Identifier("figura", playerId.toString());
            getTextureManager().registerTexture(texture.id, texture);
            texture.fromNBT(textureTag);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            if (tag.contains("script")) {
                CompoundTag scriptTag = (CompoundTag) tag.get("script");

                script = new CustomScript();
                script.fromNBT(this, scriptTag);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            if (tag.contains("exTexs")) {
                ListTag textureList = (ListTag) tag.get("exTexs");

                for (Tag etTag : textureList) {
                    FiguraTexture newTexture = new FiguraTexture();
                    newTexture.id = new Identifier("figura", playerId.toString() + newTexture.type.toString());
                    newTexture.fromNBT((CompoundTag) etTag);
                    getTextureManager().registerTexture(newTexture.id, newTexture);
                    extraTextures.add(newTexture);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //Returns the file size, in bytes.
    public int getFileSize() {
        CompoundTag writtenTag = new CompoundTag();
        toNBT(writtenTag);

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream w = new DataOutputStream(baos);

            NbtIo.writeCompressed(writtenTag, w);

            model.totalSize = w.size();
            return w.size();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return -1;
    }


    //Ticks from client.
    public void tick() {

        if (!isLoaded) {
            tickLoads();
            return;
        }

        if (isInvalidated)
            PlayerDataManager.clearPlayer(playerId);

        PlayerEntity newEnt = MinecraftClient.getInstance().world.getPlayerByUuid(playerId);
        if (lastEntity != newEnt) {
            lastEntity = newEnt;

            if (lastEntity != null) {
                if (script != null) {
                    CustomScript reloadedScript = new CustomScript();
                    reloadedScript.load(this, script.source);

                    script = reloadedScript;
                }
            }
        }

        if (lastEntity != null) {
            if (script != null) {
                try {
                    script.tick();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
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
        CompoundTag nbtTag = NbtIo.readCompressed(input);

        fromNBT(nbtTag);

        getFileSize();
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

    public boolean compareVersions(int[] version) {
        if (version[0] != current_version[0]) {
            System.out.printf("MEGA VERSION DIFFERENCE BETWEEN FILE VERSION (%i-%i-%i) AND MOD VERSION (%i-%i-%i)", version[0], version[1], version[2], current_version[0], current_version[1], current_version[2]);
            return false;
        }
        if (version[1] != current_version[1]) {
            System.out.printf("MAJOR VERSION DIFFERENCE BETWEEN FILE VERSION (%i-%i-%i) AND MOD VERSION (%i-%i-%i)", version[0], version[1], version[2], current_version[0], current_version[1], current_version[2]);
            return false;
        }
        return true;
    }

    public TrustContainer getTrustContainer() {
        return PlayerTrustManager.getContainer(getTrustIdentifier());
    }

    public PlayerEntity getEntityIfLoaded() {
        return MinecraftClient.getInstance().world.getPlayerByUuid(playerId);
    }

    public enum LoadType {
        NONE,
        LOCAL,
        SERVER
    }
}
