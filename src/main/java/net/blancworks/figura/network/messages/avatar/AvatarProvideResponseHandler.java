package net.blancworks.figura.network.messages.avatar;

import com.google.common.io.LittleEndianDataInputStream;
import net.blancworks.figura.network.messages.MessageHandler;

public class AvatarProvideResponseHandler extends MessageHandler {

    @Override
    public void handleMessage(LittleEndianDataInputStream stream) throws Exception {
        super.handleMessage(stream);
    }

    @Override
    public String getProtocolName() {
        return "figura_v1:avatar_provide";
    }
}
