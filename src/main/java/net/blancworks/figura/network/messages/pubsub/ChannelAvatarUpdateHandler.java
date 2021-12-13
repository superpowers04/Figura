package net.blancworks.figura.network.messages.pubsub;

import com.google.common.io.LittleEndianDataInputStream;
import net.blancworks.figura.avatar.AvatarDataManager;

public class ChannelAvatarUpdateHandler extends ChannelMessageHandler {

    @Override
    public void handleMessage(LittleEndianDataInputStream stream) throws Exception {
        super.handleMessage(stream);

        AvatarDataManager.getDataForPlayer(senderID).isInvalidated = true;
        
        System.out.println("AVATAR UPDATE RECEIVED");
    }

    @Override
    public String getProtocolName() {
        return "figura_v1:channel_avatar_update";
    }
}
