package net.blancworks.figura.avatar;

import com.mojang.authlib.GameProfile;
import net.blancworks.figura.FiguraMod;
import net.blancworks.figura.lua.api.sound.FiguraSoundManager;
import net.minecraft.block.entity.SkullBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.text.LiteralText;
import net.minecraft.util.registry.Registry;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class AvatarDataManager {
    public static boolean didInitLocalPlayer = false;
    public static final Map<UUID, AvatarData> LOADED_PLAYER_DATA = new ConcurrentHashMap<>();
    public static final Map<UUID, EntityAvatarData> LOADED_ENTITY_DATA = new ConcurrentHashMap<>();
    public static final Map<UUID, UUID> OFFLINE_SWAP_DATA = new HashMap<>();

    //Players that we're currently queued up to grab data for.
    private static final Set<UUID> SERVER_REQUESTED_PLAYERS = new HashSet<>();
    private static final List<UUID> TO_CLEAR = new ArrayList<>();

    //Hash checking stuff
    public static final Queue<UUID> TO_REFRESH = new ArrayDeque<>();
    public static final Set<UUID> TO_REFRESH_SET = new HashSet<>();

    public static LocalAvatarData localPlayer;
    public static NbtCompound localPlayerNbt;
    public static String localPlayerPath;

    public static boolean panic = false;

    public static AvatarData getDataForPlayer(UUID id) {
        if (panic || id == null)
            return null;

        if (OFFLINE_SWAP_DATA.containsKey(id)) {
            AvatarData data = LOADED_PLAYER_DATA.get(OFFLINE_SWAP_DATA.get(id));
            if (data != null) {
                AvatarData newData = new AvatarData(id);

                //copy avatar nbt
                NbtCompound nbt = new NbtCompound();
                data.writeNbt(nbt);
                newData.loadFromNbt(nbt);

                LOADED_PLAYER_DATA.put(id, newData);
                OFFLINE_SWAP_DATA.remove(id);
            }
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && id == client.player.getUuid()) {
            if (didInitLocalPlayer) {
                if (client.getNetworkHandler() != null)
                    localPlayer.playerListEntry = client.getNetworkHandler().getPlayerListEntry(localPlayer.entityId);
                return localPlayer;
            }

            localPlayer = new LocalAvatarData(client.player.getUuid());
            LOADED_PLAYER_DATA.put(client.player.getUuid(), localPlayer);
            didInitLocalPlayer = true;

            if (client.getNetworkHandler() != null)
                localPlayer.playerListEntry = client.getNetworkHandler().getPlayerListEntry(localPlayer.entityId);

            if (localPlayerPath != null || localPlayerNbt != null) {
                localPlayer.vanillaModel = ((PlayerEntityRenderer) client.getEntityRenderDispatcher().getRenderer(client.player)).getModel();
                localPlayer.reloadAvatar();
                return localPlayer;
            }

            getPlayerAvatarFromServerOrCache(localPlayer.entityId, localPlayer);

            return localPlayer;
        }

        AvatarData getData;
        if (!LOADED_PLAYER_DATA.containsKey(id)) {
            getData = new AvatarData(id);

            if (client.getNetworkHandler() != null) {
                PlayerListEntry playerEntry = client.getNetworkHandler().getPlayerListEntry(id);
                if (playerEntry != null && playerEntry.getProfile() != null) {
                    String name = playerEntry.getProfile().getName();
                    if (!name.isBlank()) {
                        GameProfile gameProfile = new GameProfile(null, name);
                        SkullBlockEntity.loadProperties(gameProfile, profile -> {
                            UUID profileID = profile.getId();
                            if (profileID == null || id.compareTo(profileID) == 0)
                                return;

                            getPlayerAvatarFromServerOrCache(profileID, getData);
                            OFFLINE_SWAP_DATA.put(id, profileID);
                        });
                    }
                }
            }

            getPlayerAvatarFromServerOrCache(id, getData);

            LOADED_PLAYER_DATA.put(id, getData);
        } else {
            getData = LOADED_PLAYER_DATA.get(id);
        }

        if (getData != null) {
            LiteralText playerName = new LiteralText("");
            if (client.getNetworkHandler() != null) {
                PlayerListEntry playerEntry = client.getNetworkHandler().getPlayerListEntry(id);
                if (playerEntry != null && playerEntry.getProfile() != null)
                    playerName = new LiteralText(playerEntry.getProfile().getName());

                getData.playerListEntry = playerEntry;
            }
            getData.name = playerName;
        }

        return getData;
    }

    public static AvatarData getDataForEntity(Entity entity) {
        if (panic || entity == null)
            return null;

        UUID id = entity.getUuid();
        EntityAvatarData getData;
        if (!LOADED_PLAYER_DATA.containsKey(id)) {
            getData = new EntityAvatarData(id);
            NbtCompound avatar = EntityAvatarData.CEM_MAP.get(Registry.ENTITY_TYPE.getId(entity.getType()));

            if (avatar != null) {
                avatar.putUuid("id", id);
                getData.loadFromNbt(avatar);
            }

            LOADED_ENTITY_DATA.put(id, getData);
        } else {
            getData = LOADED_ENTITY_DATA.get(id);
        }

        //System.out.println(getData.model);
        return getData;
    }

    //Attempts to get the data for a player from the server.
    public static void getPlayerAvatarFromServerOrCache(UUID id, AvatarData targetData) {
        //Prevent this from running more than once at a time per player.
        if (SERVER_REQUESTED_PLAYERS.contains(id))
            return;
        SERVER_REQUESTED_PLAYERS.add(id);

        FiguraMod.doTask(() -> {

            try {
                //TODO - Re-enable cache
                    //Attempt to load from cache first.
                    //attemptCacheLoad(id, targetData);
                    //If cache load fails or is invalid, load from server.
                    loadFromNetwork(id, targetData);
            } catch (Exception e){
                e.printStackTrace();
            }

            SERVER_REQUESTED_PLAYERS.remove(id);
        });
    }

    //Loads the model out of the local cache, if the file for that exists.
    public static void attemptCacheLoad(UUID id, AvatarData targetData) {
        Path destinationPath = FiguraMod.getModContentDirectory().resolve("cache");

        String[] splitID = id.toString().split("-");

        for (int i = 0; i < splitID.length; i++) {
            if (i != splitID.length - 1)
                destinationPath = destinationPath.resolve(splitID[i]);
        }

        Path nbtFilePath = destinationPath.resolve(splitID[splitID.length - 1] + ".nbt");
        Path hashFilePath = destinationPath.resolve(splitID[splitID.length - 1] + ".hsh");

        try {
            if (Files.exists(nbtFilePath) && Files.exists(hashFilePath)) {
                String hash = Files.readAllLines(hashFilePath).get(0);
                FileInputStream fis = new FileInputStream(nbtFilePath.toFile());
                DataInputStream dis = new DataInputStream(fis);

                targetData.loadFromNbt(NbtIo.readCompressed(dis));
                targetData.lastHash = hash;

                FiguraMod.LOGGER.debug("Used cached model.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //Loads the model off of the network.
    public static void loadFromNetwork(UUID id, AvatarData targetData) {
        FiguraMod.networkManager.getAvatarData(id);
    }

    public static void clearPlayer(UUID id) {
        if (localPlayer != null && id.compareTo(localPlayer.entityId) == 0) {
            if (!localPlayer.isLocalAvatar)
                clearLocalPlayer();
            else if (localPlayerPath != null || localPlayerNbt != null)
                localPlayer.reloadAvatar();
        }
        else {
            TO_CLEAR.add(id);
        }
    }

    public static void clearCache() {
        FiguraSoundManager.getChannel().stopAllSounds();
        LOADED_PLAYER_DATA.clear();
        localPlayer = null;
        didInitLocalPlayer = false;
        localPlayerNbt = null;
        localPlayerPath = null;
    }

    public static void clearLocalPlayer() {
        if (localPlayer == null) return;
        localPlayer.clearData();

        LOADED_PLAYER_DATA.remove(localPlayer.entityId);
        localPlayer = null;
        didInitLocalPlayer = false;
        localPlayerNbt = null;
        localPlayerPath = null;
    }

    private static int hashCheckCooldown = 0;

    //Tick function for the client. Basically dispatches all the other functions in the mod.
    public static void tick() {
        if (MinecraftClient.getInstance().world == null)
            return;

        synchronized(TO_CLEAR) {
            TO_CLEAR.forEach(uuid -> {
                AvatarData data = getDataForPlayer(uuid);
                if (data != null) data.clearData();
                else FiguraSoundManager.getChannel().stopSound(uuid);
                LOADED_PLAYER_DATA.remove(uuid);
            });
            TO_CLEAR.clear();
        }

        synchronized(LOADED_PLAYER_DATA) {
            LOADED_PLAYER_DATA.values().forEach(AvatarData::tick);
        }

        synchronized(LOADED_ENTITY_DATA) {
            LOADED_ENTITY_DATA.values().forEach(AvatarData::tick);
        }
    }

    //Reloads all textures, used for asset reloads in vanilla.
    public static void reloadAssets() {
        AvatarDataManager.LOADED_ENTITY_DATA.clear();
        LOADED_PLAYER_DATA.values().forEach(data -> {
            if (data.texture != null) {
                data.texture.registerTexture();
                data.texture.uploadUsingData();
            }

            data.extraTextures.forEach(figuraTexture -> {
                figuraTexture.registerTexture();
                figuraTexture.uploadUsingData();
            });
        });
    }
}
