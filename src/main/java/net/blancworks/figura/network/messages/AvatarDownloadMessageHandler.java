package net.blancworks.figura.network.messages;

import com.google.common.io.LittleEndianDataInputStream;

import java.io.DataInputStream;

public class AvatarDownloadMessageHandler extends MessageHandler{

    @Override
    public void handleHeader(LittleEndianDataInputStream stream) {
        super.handleHeader(stream);
    }

    @Override
    public void handleBody(LittleEndianDataInputStream stream) {
        super.handleBody(stream);
    }

    @Override
    public boolean expectBody() {
        return true;
    }
}
