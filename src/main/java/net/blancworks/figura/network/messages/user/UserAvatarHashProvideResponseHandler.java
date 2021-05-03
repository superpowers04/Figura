package net.blancworks.figura.network.messages.user;

import com.google.common.io.LittleEndianDataInputStream;
import net.blancworks.figura.PlayerData;
import net.blancworks.figura.PlayerDataManager;
import net.blancworks.figura.network.messages.MessageHandler;

import java.util.UUID;

public class UserAvatarHashProvideResponseHandler extends MessageHandler {

    @Override
    public void handleMessage(LittleEndianDataInputStream stream) throws Exception {
        super.handleMessage(stream);
        
        UUID id = readUUID(stream);
        String hash = readString(stream);
        
        //Handle?
        PlayerData pDat = PlayerDataManager.getDataForPlayer(id);
        
        if(!pDat.lastHash.equals(hash)){
            pDat.isInvalidated = true;   
        }
    }
}
