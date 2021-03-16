package net.blancworks.figura;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.blancworks.figura.network.FiguraNetworkManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.util.Util;
import org.apache.logging.log4j.Level;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
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
                    localPlayer.vanillaModel = ((PlayerEntityRenderer)MinecraftClient.getInstance().getEntityRenderDispatcher().getRenderer(MinecraftClient.getInstance().player)).getModel();
                    localPlayer.loadModelFile(lastLoadedFileName);
                    localPlayer.getFileSize();
                }

                getPlayerAvatarFromServer(localPlayer.playerId, localPlayer);
                return localPlayer;
            }
        }

        if (id == MinecraftClient.getInstance().player.getUuid())
            return localPlayer;

        if (loadedPlayerData.containsKey(id) == false) {
            getData = new PlayerData();
            getData.playerId = id;

            getPlayerAvatarFromServer(id, getData);

            loadedPlayerData.put(id, getData);
        } else {
            getData = loadedPlayerData.get(id);
        }

        return getData;
    }

    //Attempts to get the data for a player from the server.
    public static void getPlayerAvatarFromServer(UUID id, PlayerData targetData) {

        //Prevent this from running more than once at a time per player.
        if (serverRequestedPlayers.contains(id))
            return;
        serverRequestedPlayers.add(id);

        try {

            URL url = new URL(String.format("%s/api/avatar/%s", FiguraNetworkManager.getServerURL(), id));

            CompletableFuture.runAsync(() -> {
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

    public static void clearPlayer(UUID id) {
        toClear.add(id);

        if(localPlayer != null) {
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

    //Tick function for the client. Basically dispatches all the other functions in the mod.
    public static void tick() {
        if (MinecraftClient.getInstance().world == null)
            return;

        for (UUID uuid : toClear) {
            loadedPlayerData.remove(uuid);
        }
        toClear.clear();

        for (Map.Entry<UUID, PlayerData> entry : loadedPlayerData.entrySet()) {
            entry.getValue().tick();
        }
    }
}
