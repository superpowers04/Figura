package net.blancworks.figura;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.blancworks.figura.models.FiguraTexture;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.options.KeyBinding;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.LiteralText;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class PlayerDataManager {
    public static boolean didInitLocalPlayer = false;
    public static final Map<UUID, PlayerData> LOADED_PLAYER_DATA = new Object2ObjectOpenHashMap<>();

    //Players that we're currently queued up to grab data for.
    private static final Set<UUID> SERVER_REQUESTED_PLAYERS = new HashSet<>();
    private static final List<UUID> TO_CLEAR = new ArrayList<>();

    //Hash checking stuff
    public static final Queue<UUID> TO_REFRESH = new ArrayDeque<>();
    public static final Set<UUID> TO_REFRESH_SET = new HashSet<>();

    public static LocalPlayerData localPlayer;

    public static String lastLoadedFileName;

    public static boolean hasPlayerData(UUID id) {
        return LOADED_PLAYER_DATA.containsKey(id);
    }

    public static PlayerData getDataForPlayer(UUID id) {
        if (TO_CLEAR.remove(id))
            LOADED_PLAYER_DATA.remove(id);

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

            NbtCompound nbt = new NbtCompound();
            localPlayer.modelData = localPlayer.writeNbt(nbt) ? nbt : null;

            return localPlayer;
        }

        PlayerData getData;
        if (!LOADED_PLAYER_DATA.containsKey(id)) {
            getData = new PlayerData();
            getData.playerId = id;

            getPlayerAvatarFromServerOrCache(id, getData);

            LOADED_PLAYER_DATA.put(id, getData);
        }
        else {
            getData = LOADED_PLAYER_DATA.get(id);
        }

        LiteralText playerName = new LiteralText("");
        if (MinecraftClient.getInstance().getNetworkHandler() != null) {
            PlayerListEntry playerEntry = MinecraftClient.getInstance().getNetworkHandler().getPlayerListEntry(id);
            if (playerEntry != null && playerEntry.getProfile() != null)
                playerName = new LiteralText(playerEntry.getProfile().getName());
        }
        getData.playerName = playerName;

        if (MinecraftClient.getInstance().getNetworkHandler() != null)
            getData.playerListEntry = MinecraftClient.getInstance().getNetworkHandler().getPlayerListEntry(id);

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
        TO_CLEAR.add(id);

        if (localPlayer != null) {
            if (id == localPlayer.playerId) {
                localPlayer = null;
                didInitLocalPlayer = false;
            }
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
        LOADED_PLAYER_DATA.remove(localPlayer.playerId);
        localPlayer = null;
        didInitLocalPlayer = false;
        lastLoadedFileName = null;
        KeyBinding.updateKeysByCode();
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

        for (Map.Entry<UUID, PlayerData> entry : LOADED_PLAYER_DATA.entrySet()) {
            entry.getValue().tick();
        }
    }

    //Reloads all textures, used for asset reloads in vanilla.
    public static void reloadAllTextures() {
        for (Map.Entry<UUID, PlayerData> entry : LOADED_PLAYER_DATA.entrySet()) {
            PlayerData pDat = entry.getValue();
            if (pDat.texture != null) {
                pDat.texture.registerTexture();
                pDat.texture.uploadUsingData();
            }

            for (FiguraTexture extraTexture : pDat.extraTextures) {
                extraTexture.registerTexture();
                extraTexture.uploadUsingData();
            }
        }
    }
}
