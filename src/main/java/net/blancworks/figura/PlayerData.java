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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;


/**
 * Responsible for storing all the data associated with the player on this client.
 */
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

    public final List<FiguraTexture> extraTextures = new ArrayList<>();

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

    /**
     * Writes to the NBT this player data.
     *
     * @param nbt the nbt to write to
     * @return {@code true} if the player data was written into the NBT, otherwise {@code false}
     */
    public boolean writeNbt(CompoundTag nbt) {
        //You cannot save a model that is incomplete.
        if (model == null || texture == null)
            return false;

        nbt.putIntArray("version", CURRENT_VERSION);

        //Put ID.
        nbt.putUuid("id", playerId);

        //Put Model.
        CompoundTag modelNbt = new CompoundTag();
        model.writeNbt(modelNbt);
        nbt.put("model", modelNbt);

        //Put Texture.
        try {
            CompoundTag textureNbt = new CompoundTag();
            texture.writeNbt(textureNbt);
            nbt.put("texture", textureNbt);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        if (script != null) {
            //Put Script.
            CompoundTag scriptNbt = new CompoundTag();
            script.toNBT(scriptNbt);
            nbt.put("script", scriptNbt);
        }

        if (extraTextures.size() > 0) {
            ListTag texList = new ListTag();

            for (FiguraTexture extraTexture : extraTextures) {
                CompoundTag elytraTextureNbt = new CompoundTag();
                extraTexture.writeNbt(elytraTextureNbt);
                texList.add(elytraTextureNbt);
            }

            nbt.put("exTexs", texList);
        }

        return true;
    }

    /**
     * Reads a player data from the given NBT.
     *
     * @param nbt the nbt to read
     */
    public void readNbt(CompoundTag nbt) {
        int[] version = nbt.getIntArray("version");

        playerId = nbt.getUuid("id");

        //VERSION CHECKING.
        if (version != null) {
            boolean success = compareVersions(version);

            if (!success)
                return;
        }

        try {
            CompoundTag modelNbt = (CompoundTag) nbt.get("model");
            model = new CustomModel();
            model.readNbt(modelNbt);
            model.owner = this;
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            CompoundTag textureNbt = (CompoundTag) nbt.get("texture");
            texture = new FiguraTexture();
            texture.id = new Identifier("figura", playerId.toString());
            getTextureManager().registerTexture(texture.id, texture);
            texture.readNbt(textureNbt);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            if (nbt.contains("script")) {
                CompoundTag scriptNbt = (CompoundTag) nbt.get("script");

                script = new CustomScript();
                script.fromNBT(this, scriptNbt);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            if (nbt.contains("exTexs")) {
                ListTag textureList = (ListTag) nbt.get("exTexs");

                for (Tag element : textureList) {
                    FiguraTexture newTexture = new FiguraTexture();
                    newTexture.id = new Identifier("figura", playerId.toString() + newTexture.type.toString());
                    newTexture.readNbt((CompoundTag) element);
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
        CompoundTag writtenNbt = new CompoundTag();
        this.writeNbt(writtenNbt);

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            DataOutputStream w = new DataOutputStream(out);

            NbtIo.writeCompressed(writtenNbt, w);

            this.model.totalSize = w.size();
            return w.size();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return -1;
    }

    //Ticks from client.
    public void tick() {
        if (!this.isLoaded) {
            this.tickLoads();
            return;
        }

        if (this.isInvalidated)
            PlayerDataManager.clearPlayer(playerId);

        PlayerEntity newEnt = MinecraftClient.getInstance().world.getPlayerByUuid(this.playerId);
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

    /**
     * Attempts to load assets locally of disk.
     * @throws Exception
     */
    protected void loadLocal() throws Exception {
        Path localPath = FabricLoader.getInstance().getGameDir().getParent().resolve("model_files").resolve("cache").resolve(playerId.toString() + ".nbt");

        //If no local cache exists for the player in question, we switch to server loading.
        if (!Files.exists(localPath)) {
            loadType = LoadType.SERVER;
            return;
        }

        loadFromNbtFile(localPath);
        isLoaded = true;
    }

    /**
     * Attempts to load assets off of the server
     */
    protected void loadServer() {
        loadType = LoadType.NONE;
        isLoaded = true;
    }


    public void loadFromNbtFile(Path path) throws Exception {
        DataInputStream input = new DataInputStream(new FileInputStream(path.toFile()));
        loadFromNbt(input);
    }

    public void loadFromNbt(DataInputStream input) throws Exception {
        CompoundTag nbt = NbtIo.readCompressed(input);

        this.readNbt(nbt);

        getFileSize();
    }


    //VERSION
    //FORMAT IS
    //0 = mega version for huge api changes to the fundamentals of the loading system
    //1 = major version, for compatibility-breaking api changes
    //2 = minor version, for non-compat breaking api changes
    static final int[] CURRENT_VERSION = new int[3];

    static {
        CURRENT_VERSION[0] = 0;
        CURRENT_VERSION[1] = 0;
        CURRENT_VERSION[2] = 1;
    }

    public boolean compareVersions(int[] version) {
        if (version[0] != CURRENT_VERSION[0]) {
            System.out.printf("MEGA VERSION DIFFERENCE BETWEEN FILE VERSION (%i-%i-%i) AND MOD VERSION (%i-%i-%i)", version[0], version[1], version[2], CURRENT_VERSION[0], CURRENT_VERSION[1], CURRENT_VERSION[2]);
            return false;
        }
        if (version[1] != CURRENT_VERSION[1]) {
            System.out.printf("MAJOR VERSION DIFFERENCE BETWEEN FILE VERSION (%i-%i-%i) AND MOD VERSION (%i-%i-%i)", version[0], version[1], version[2], CURRENT_VERSION[0], CURRENT_VERSION[1], CURRENT_VERSION[2]);
            return false;
        }
        return true;
    }

    public TrustContainer getTrustContainer() {
        return PlayerTrustManager.getContainer(getTrustIdentifier());
    }

    public enum LoadType {
        NONE,
        LOCAL,
        SERVER
    }
}
