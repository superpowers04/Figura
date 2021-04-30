package net.blancworks.figura.network.messages.avatar;

import com.google.common.io.LittleEndianDataInputStream;
import net.blancworks.figura.network.messages.MessageHandler;

public class AvatarProvideResponseHandler extends MessageHandler {

    @Override
    public void handleHeader(LittleEndianDataInputStream stream) throws Exception {
        super.handleHeader(stream);
    }

    @Override
    public void handleBody(LittleEndianDataInputStream stream) throws Exception {
        super.handleBody(stream);
    }

    @Override
    public boolean expectBody() {
        return true;
    }
}
