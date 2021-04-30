package net.blancworks.figura.network.messages;

import com.google.common.io.LittleEndianDataInputStream;
import net.blancworks.figura.FiguraMod;

import java.io.DataInputStream;

public class DebugMessageHandler extends MessageHandler {

    @Override
    public void handleHeader(LittleEndianDataInputStream stream) throws Exception {
        super.handleHeader(stream);

        try {
            FiguraMod.LOGGER.warn(stream.readInt());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void handleBody(LittleEndianDataInputStream stream) throws Exception {
        super.handleBody(stream);

        try {
            FiguraMod.LOGGER.warn(stream.readInt());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean expectBody() {
        return true;
    }
}
