package net.blancworks.figura.network.messages.pubsub;

import com.google.common.io.LittleEndianDataInputStream;
import net.blancworks.figura.network.messages.MessageHandler;

import java.util.UUID;

//Called when a message is received from the server.
public class ChannelMessageHandler extends MessageHandler {

    public UUID senderID;
    
    @Override
    public void handleMessage(LittleEndianDataInputStream stream) throws Exception {
        super.handleMessage(stream);
        
        senderID = readUUID(stream);
    }

    @Override
    public String getProtocolName() {
        return "figura_v1:channel_message";
    }
}
