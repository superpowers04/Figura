package net.blancworks.figura;

import com.mojang.authlib.GameProfile;
import net.blancworks.figura.models.sounds.FiguraChannel;
import net.blancworks.figura.models.sounds.FiguraSound;
import net.blancworks.figura.models.sounds.FiguraSoundManager;
import net.minecraft.block.entity.SkullBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.text.LiteralText;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class PlayerDataManager {
    public static boolean didInitLocalPlayer = false;
    public static final Map<UUID, PlayerData> LOADED_PLAYER_DATA = new HashMap<>();
    public static final Map<UUID, UUID> OFFLINE_SWAP_DATA = new HashMap<>();

    //Players that we're currently queued up to grab data for.
    private static final Set<UUID> SERVER_REQUESTED_PLAYERS = new HashSet<>();
    private static final List<UUID> TO_CLEAR = new ArrayList<>();

    //Hash checking stuff
    public static final Queue<UUID> TO_REFRESH = new ArrayDeque<>();
    public static final Set<UUID> TO_REFRESH_SET = new HashSet<>();

    public static LocalPlayerData localPlayer;

    public static String lastLoadedFileName;

    public static boolean panic = false;

    public static PlayerData getDataForPlayer(UUID id) {
        if (panic) return null;

        if (OFFLINE_SWAP_DATA.containsKey(id)) {
            PlayerData data = LOADED_PLAYER_DATA.get(OFFLINE_SWAP_DATA.get(id));
            if (data != null) {
                data.playerId = id;
                LOADED_PLAYER_DATA.put(id, data);
                OFFLINE_SWAP_DATA.remove(id);
            }
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && id == client.player.getUuid()) {
            if (didInitLocalPlayer) {
                if (client.getNetworkHandler() != null)
                    localPlayer.playerListEntry = client.getNetworkHandler().getPlayerListEntry(localPlayer.playerId);
                return localPlayer;
            }

            localPlayer = new LocalPlayerData();
            localPlayer.playerId = client.player.getUuid();
            LOADED_PLAYER_DATA.put(client.player.getUuid(), localPlayer);
            didInitLocalPlayer = true;

            if (client.getNetworkHandler() != null)
                localPlayer.playerListEntry = client.getNetworkHandler().getPlayerListEntry(localPlayer.playerId);

            if (lastLoadedFileName != null) {
                localPlayer.vanillaModel = ((PlayerEntityRenderer) client.getEntityRenderDispatcher().getRenderer(client.player)).getModel();
                localPlayer.loadModelFile(lastLoadedFileName);
                return localPlayer;
            }

            getPlayerAvatarFromServerOrCache(localPlayer.playerId, localPlayer);

            return localPlayer;
        }

        PlayerData getData;
        if (!LOADED_PLAYER_DATA.containsKey(id)) {
            getData = new PlayerData();
            getData.playerId = id;

            if (client.getNetworkHandler() != null) {
                PlayerListEntry playerEntry = client.getNetworkHandler().getPlayerListEntry(id);
                if (playerEntry != null && playerEntry.getProfile() != null) {
                    String name = playerEntry.getProfile().getName();
                    if (!name.isBlank()) {
                        GameProfile gameProfile = new GameProfile(null, name);
                        SkullBlockEntity.loadProperties(gameProfile, profile -> {
                            UUID profileID = profile.getId();
                            if (id.compareTo(profileID) == 0) return;

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
            getData.playerName = playerName;
        }

        return getData;
    }

    //Attempts to get the data for a player from the server.
    public static void getPlayerAvatarFromServerOrCache(UUID id, PlayerData targetData) {
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
    public static void attemptCacheLoad(UUID id, PlayerData targetData) {
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

                targetData.loadFromNbt(dis);
                targetData.lastHash = hash;

                FiguraMod.LOGGER.debug("Used cached model.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //Loads the model off of the network.
    public static void loadFromNetwork(UUID id, PlayerData targetData) {
        FiguraMod.networkManager.getAvatarData(id);
    }

    public static void clearPlayer(UUID id) {
        if (localPlayer != null && id.compareTo(localPlayer.playerId) == 0) {
            if (!localPlayer.isLocalAvatar)
                clearLocalPlayer();
            else if (localPlayer.loadedPath != null)
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
        lastLoadedFileName = null;
    }

    public static void clearLocalPlayer() {
        if (localPlayer == null) return;
        FiguraSoundManager.getChannel().stopSound(localPlayer.playerId);
        localPlayer.clearData();

        LOADED_PLAYER_DATA.remove(localPlayer.playerId);
        localPlayer = null;
        didInitLocalPlayer = false;
        lastLoadedFileName = null;
    }

    private static int hashCheckCooldown = 0;

    //Tick function for the client. Basically dispatches all the other functions in the mod.
    public static void tick() {
        if (MinecraftClient.getInstance().world == null)
            return;

        synchronized(TO_CLEAR) {
            TO_CLEAR.forEach(uuid -> {
                PlayerData data = getDataForPlayer(uuid);
                if (data != null) data.clearData();
                else FiguraSoundManager.getChannel().stopSound(uuid);
                LOADED_PLAYER_DATA.remove(uuid);
            });
            TO_CLEAR.clear();
        }

        synchronized(LOADED_PLAYER_DATA) {
            LOADED_PLAYER_DATA.values().forEach(PlayerData::tick);
        }
    }

    //Reloads all textures, used for asset reloads in vanilla.
    public static void reloadAllTextures() {
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
