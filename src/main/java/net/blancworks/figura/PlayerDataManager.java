package net.blancworks.figura;

import net.blancworks.figura.mixin.KeyBindingAccessorMixin;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.option.KeyBinding;
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

    //Players that we're currently queued up to grab data for.
    private static final Set<UUID> SERVER_REQUESTED_PLAYERS = new HashSet<>();
    private static final List<UUID> TO_CLEAR = new ArrayList<>();

    //Hash checking stuff
    public static final Queue<UUID> TO_REFRESH = new ArrayDeque<>();
    public static final Set<UUID> TO_REFRESH_SET = new HashSet<>();

    public static LocalPlayerData localPlayer;

    public static String lastLoadedFileName;

    public static PlayerData getDataForPlayer(UUID id) {
        try {
            if (TO_CLEAR.remove(id))
                LOADED_PLAYER_DATA.remove(id);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (MinecraftClient.getInstance().player != null && id == MinecraftClient.getInstance().player.getUuid()) {
            if (didInitLocalPlayer)
                return localPlayer;

            localPlayer = new LocalPlayerData();
            localPlayer.playerId = MinecraftClient.getInstance().player.getUuid();
            LOADED_PLAYER_DATA.put(MinecraftClient.getInstance().player.getUuid(), localPlayer);
            didInitLocalPlayer = true;

            if (MinecraftClient.getInstance().getNetworkHandler() != null)
                localPlayer.playerListEntry = MinecraftClient.getInstance().getNetworkHandler().getPlayerListEntry(localPlayer.playerId);

            if (lastLoadedFileName != null) {
                localPlayer.vanillaModel = ((PlayerEntityRenderer) MinecraftClient.getInstance().getEntityRenderDispatcher().getRenderer(MinecraftClient.getInstance().player)).getModel();
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

            getPlayerAvatarFromServerOrCache(id, getData);

            LOADED_PLAYER_DATA.put(id, getData);
        } else {
            getData = LOADED_PLAYER_DATA.get(id);
        }

        if (getData != null) {
            LiteralText playerName = new LiteralText("");
            if (MinecraftClient.getInstance().getNetworkHandler() != null) {
                PlayerListEntry playerEntry = MinecraftClient.getInstance().getNetworkHandler().getPlayerListEntry(id);
                if (playerEntry != null && playerEntry.getProfile() != null)
                    playerName = new LiteralText(playerEntry.getProfile().getName());
            }
            getData.playerName = playerName;

            if (MinecraftClient.getInstance().getNetworkHandler() != null)
                getData.playerListEntry = MinecraftClient.getInstance().getNetworkHandler().getPlayerListEntry(id);
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
        LOADED_PLAYER_DATA.clear();
        localPlayer = null;
        didInitLocalPlayer = false;
        lastLoadedFileName = null;
    }

    public static void clearLocalPlayer() {
        if (localPlayer == null) return;

        if (localPlayer.script != null) {
            localPlayer.script.keyBindings.forEach(keyBinding -> KeyBindingAccessorMixin.getKeysById().remove(keyBinding.getTranslationKey()));
            KeyBinding.updateKeysByCode();
        }

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

        for (UUID uuid : TO_CLEAR) {
            LOADED_PLAYER_DATA.remove(uuid);
        }
        TO_CLEAR.clear();

        LOADED_PLAYER_DATA.values().forEach(PlayerData::tick);
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
