package net.blancworks.figura;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.blancworks.figura.network.FiguraNetworkManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Util;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class PlayerDataManager {

    public static boolean didInitLocalPlayer = false;
    public static HashMap<UUID, PlayerData> loadedPlayerData = new HashMap<UUID, PlayerData>();
    public static HashMap<UUID, PlayerTrustData> playerTrustData = new HashMap<UUID, PlayerTrustData>();

    //Players that we're currently queued up to grab data for.
    private static HashSet<UUID> serverRequestedPlayers = new HashSet<UUID>();

    public static LocalPlayerData localPlayer;

    public static PlayerData getDataForPlayer(UUID id) {
        
        PlayerData getData = null;
        
        if(!didInitLocalPlayer){
            if(id == MinecraftClient.getInstance().player.getUuid()){
                localPlayer = new LocalPlayerData();
                localPlayer.playerId = MinecraftClient.getInstance().player.getUuid();
                PlayerDataManager.loadedPlayerData.put(MinecraftClient.getInstance().player.getUuid(), localPlayer);
                didInitLocalPlayer = true;

                GetPlayerAvatarFromServer(localPlayer.playerId, localPlayer);
                return localPlayer;
            }
        }
        
        if (loadedPlayerData.containsKey(id) == false) {
            getData = new PlayerData();
            getData.playerId = id;

            GetPlayerAvatarFromServer(id, getData);

            loadedPlayerData.put(id, getData);
        } else {
            getData = loadedPlayerData.get(id);
        }
        
        return getData;
    }

    public static PlayerTrustData getTrustDataForPlayer(UUID id) {
        return playerTrustData.get(id);
    }


    //Attempts to get the data for a player from the server.
    public static void GetPlayerAvatarFromServer(UUID id, PlayerData targetData) {

        //Prevent this from running more than once at a time per player.
         if (serverRequestedPlayers.contains(id))
            return;
        serverRequestedPlayers.add(id);

        try {

            URL url = new URL(String.format("%s/api/avatar/%s", FiguraNetworkManager.GetServerURL(), id));

            System.out.println(url);

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
                        byte[] dataAsBytes = Base64.getDecoder().decode(dataString);
                        InputStream dataAsStream = new ByteArrayInputStream(dataAsBytes);
                        DataInputStream receivedDataToStream = new DataInputStream(dataAsStream);
                        receivedDataToStream.reset();

                        System.out.println(dataString);

                        targetData.loadFromNBT(receivedDataToStream);
                        System.out.println("LOADED NBT MODEL FOR PLAYER FROM SERVER");
                    }
                } catch (Exception e) {
                    System.out.println(e);
                    httpURLConnection.disconnect();
                }

                serverRequestedPlayers.remove(id);
                
            }, Util.getMainWorkerExecutor());
        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }

    
    public static void clearCache(){
        loadedPlayerData.clear();
        didInitLocalPlayer = false;
    }

    //Tick function for the client. Basically dispatches all the other functions in the mod.
    public static void tick() {
        if (MinecraftClient.getInstance().world == null)
            return;

        for (Map.Entry<UUID, PlayerData> entry : loadedPlayerData.entrySet()) {
            entry.getValue().tick();
        }
    }
}
