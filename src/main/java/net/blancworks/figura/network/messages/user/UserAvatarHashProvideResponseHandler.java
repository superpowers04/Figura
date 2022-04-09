package net.blancworks.figura.network.messages.user;

import com.google.common.io.LittleEndianDataInputStream;
import net.blancworks.figura.avatar.AvatarData;
import net.blancworks.figura.avatar.AvatarDataManager;
import net.blancworks.figura.network.messages.MessageHandler;

import java.util.UUID;

public class UserAvatarHashProvideResponseHandler extends MessageHandler {

    @Override
    public void handleMessage(LittleEndianDataInputStream stream) throws Exception {
        super.handleMessage(stream);
        
        UUID id = readUUID(stream);
        String hash = readString(stream);
        
        //Handle?
        AvatarData pDat = AvatarDataManager.getDataForPlayer(id);
        
        if(!pDat.lastHash.equals(hash)){
            pDat.isInvalidated = true;   
        }
    }

    @Override
    public String getProtocolName() {
        return "figura_v1:user_avatar_hash_provide";
    }
}
