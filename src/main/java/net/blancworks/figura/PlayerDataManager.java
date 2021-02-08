package net.blancworks.figura;

import net.minecraft.client.MinecraftClient;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerDataManager {

    public static boolean didInitLocalPlayer = false;
    public static HashMap<UUID, PlayerData> loadedPlayerData = new HashMap<UUID, PlayerData>();
    public static HashMap<UUID, PlayerTrustData> playerTrustData = new HashMap<UUID, PlayerTrustData>();
    
    public static LocalPlayerData localPlayer;

    public static PlayerData getDataForPlayer(UUID id) {
        if (loadedPlayerData.containsKey(id) == false) {
            PlayerData newData = new PlayerData();
            newData.playerId = id;

            loadedPlayerData.put(id, newData);
            return newData;
        }

        return loadedPlayerData.get(id);
    }

    public static PlayerTrustData getTrustDataForPlayer(UUID id) {
        return playerTrustData.get(id);
    }


    
    //Tick function for the client. Basically dispatches all the other functions in the mod.
    public static void tick() {
        if(MinecraftClient.getInstance().world == null)
            return;
        
        if(!didInitLocalPlayer && MinecraftClient.getInstance().player != null){
            localPlayer = new LocalPlayerData();
            localPlayer.playerId = MinecraftClient.getInstance().player.getUuid();
            PlayerDataManager.loadedPlayerData.put(MinecraftClient.getInstance().player.getUuid(), localPlayer);
            didInitLocalPlayer = true;
        }
        
        for (Map.Entry<UUID, PlayerData> entry : loadedPlayerData.entrySet()) {
            entry.getValue().tick();
        }
    }
}
