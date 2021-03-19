package net.blancworks.figura;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.blancworks.figura.network.FiguraNetworkManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.util.Util;
import org.apache.logging.log4j.Level;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class PlayerDataManager {

    public static boolean didInitLocalPlayer = false;
    public static HashMap<UUID, PlayerData> loadedPlayerData = new HashMap<UUID, PlayerData>();

    //Players that we're currently queued up to grab data for.
    private static HashSet<UUID> serverRequestedPlayers = new HashSet<UUID>();
    private static ArrayList<UUID> toClear = new ArrayList<>();

    public static LocalPlayerData localPlayer;

    public static String lastLoadedFileName;

    public static boolean hasPlayerData(UUID id) {
        return loadedPlayerData.containsKey(id);
    }

    public static PlayerData getDataForPlayer(UUID id) {

        PlayerData getData = null;

        if (toClear.contains(id)) {
            toClear.remove(id);
            loadedPlayerData.remove(id);
        }

        if (!didInitLocalPlayer) {
            if (id == MinecraftClient.getInstance().player.getUuid()) {
                localPlayer = new LocalPlayerData();
                localPlayer.playerId = MinecraftClient.getInstance().player.getUuid();
                loadedPlayerData.put(MinecraftClient.getInstance().player.getUuid(), localPlayer);
                didInitLocalPlayer = true;

                if (lastLoadedFileName != null) {
                    localPlayer.vanillaModel = ((PlayerEntityRenderer) MinecraftClient.getInstance().getEntityRenderDispatcher().getRenderer(MinecraftClient.getInstance().player)).getModel();
                    localPlayer.loadModelFile(lastLoadedFileName);
                    localPlayer.getFileSize();
                    return localPlayer;
                }

                getPlayerAvatarFromServerOrCache(localPlayer.playerId, localPlayer);
                return localPlayer;
            }
        }

        if (id == MinecraftClient.getInstance().player.getUuid())
            return localPlayer;

        if (loadedPlayerData.containsKey(id) == false) {
            getData = new PlayerData();
            getData.playerId = id;

            getPlayerAvatarFromServerOrCache(id, getData);

            loadedPlayerData.put(id, getData);
        } else {
            getData = loadedPlayerData.get(id);
        }

        return getData;
    }

    //Attempts to get the data for a player from the server.
    public static void getPlayerAvatarFromServerOrCache(UUID id, PlayerData targetData) {

        //Prevent this from running more than once at a time per player.
        if (serverRequestedPlayers.contains(id))
            return;
        serverRequestedPlayers.add(id);

        try {

            URL url = new URL(String.format("%s/api/avatar/%s", FiguraNetworkManager.getServerURL(), id));

            CompletableFuture.runAsync(() -> {

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
                        String serverHash = hash;

                        try {
                            serverHash = FiguraNetworkManager.getAvatarHash(id).get();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        if (serverHash.length() == 0)
                            serverHash = hash;

                        if (serverHash.equals(hash)) {
                            FileInputStream fis = new FileInputStream(nbtFilePath.toFile());
                            DataInputStream dis = new DataInputStream(fis);

                            targetData.loadFromNBT(dis);
                            targetData.lastHash = hash;
                            targetData.lastHashCheckTime = new Date(new Date().getTime() - (1000 * 1000));

                            serverRequestedPlayers.remove(id);

                            return;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                HttpURLConnection httpURLConnection = null;

                //Object to fill data with
                JsonObject targetObject = null;

                try {
                    httpURLConnection = (HttpURLConnection) (url).openConnection(MinecraftClient.getInstance().getNetworkProxy());
                    httpURLConnection.setDoInput(true);
                    httpURLConnection.setDoOutput(false);
                    httpURLConnection.connect();

                    if (httpURLConnection.getResponseCode() / 100 == 2) {

                        //Put JSON into string
                        BufferedReader in = new BufferedReader(
                                new InputStreamReader(httpURLConnection.getInputStream()));
                        String inputLine;
                        StringBuffer content = new StringBuffer();
                        while ((inputLine = in.readLine()) != null) {
                            content.append(inputLine);
                        }
                        in.close();
                        //Parse JSON
                        JsonParser parser = new JsonParser();
                        targetObject = parser.parse(content.toString()).getAsJsonObject();
                    }

                    httpURLConnection.disconnect();


                    //Attempt to load data from the JSON we just got from the server
                    if (targetObject != null) {
                        String dataString = targetObject.get("data").getAsString();
                        if (dataString.length() != 0) {
                            byte[] dataAsBytes = Base64.getDecoder().decode(dataString);
                            InputStream dataAsStream = new ByteArrayInputStream(dataAsBytes);
                            DataInputStream receivedDataToStream = new DataInputStream(dataAsStream);
                            receivedDataToStream.reset();

                            targetData.loadFromNBT(receivedDataToStream);
                            targetData.lastHash = FiguraNetworkManager.getAvatarHash(id).get();
                            targetData.lastHashCheckTime = new Date(new Date().getTime() - (1000 * 1000));


                            while (targetData.texture.ready == false)
                                Thread.sleep(50);
                            saveToCache(targetData, nbtFilePath, hashFilePath);
                        }
                    }
                } catch (Exception e) {
                    FiguraMod.LOGGER.log(Level.ERROR, e);
                    httpURLConnection.disconnect();
                }

                serverRequestedPlayers.remove(id);

            }, Util.getMainWorkerExecutor());
        } catch (Exception e) {
            FiguraMod.LOGGER.log(Level.ERROR, e);
        }
    }

    public static void saveToCache(PlayerData data, Path targetPath, Path targetHashPath) {
        try {
            CompoundTag targetTag = new CompoundTag();
            data.toNBT(targetTag);

            Files.createDirectories(targetPath.getParent());
            NbtIo.writeCompressed(targetTag, new FileOutputStream(targetPath.toFile()));
            Files.write(targetHashPath, data.lastHash.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void clearPlayer(UUID id) {
        toClear.add(id);

        if (localPlayer != null) {
            if (id == localPlayer.playerId) {
                localPlayer = null;
                didInitLocalPlayer = false;
            }
        }
    }

    public static void clearCache() {
        loadedPlayerData.clear();
        localPlayer = null;
        didInitLocalPlayer = false;
        lastLoadedFileName = null;
    }

    private static int hashCheckCooldown = 0;

    //Tick function for the client. Basically dispatches all the other functions in the mod.
    public static void tick() {
        if (MinecraftClient.getInstance().world == null)
            return;

        for (UUID uuid : toClear) {
            loadedPlayerData.remove(uuid);
        }
        toClear.clear();

        if (hashCheckCooldown > 0) {
            hashCheckCooldown--;
        }

        Date checkDate = new Date();
        for (Map.Entry<UUID, PlayerData> entry : loadedPlayerData.entrySet()) {
            entry.getValue().tick();

            if (hashCheckCooldown == 0) {
                if (checkDate.getTime() - entry.getValue().lastHashCheckTime.getTime() > 1000 * 10) {
                    checkPlayerDataHash(entry.getValue().playerId);
                    break;
                }
            }
        }
    }

    public static void checkPlayerDataHash(UUID id) {
        PlayerData dat = getDataForPlayer(id);

        if (dat instanceof LocalPlayerData) {
            if (((LocalPlayerData) dat).loadedName != null) {
                return;
            }
        }

        if (dat.lastHash.length() != 0) {
            dat.lastHashCheckTime = new Date();

            hashCheckCooldown = 4;

            CompletableFuture.runAsync(() -> {
                try {
                    String hash = FiguraNetworkManager.getAvatarHash(id).get();

                    if (hash.equals(dat.lastHash) == false && hash.length() > 0) {
                        toClear.add(id);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }

    }
}
