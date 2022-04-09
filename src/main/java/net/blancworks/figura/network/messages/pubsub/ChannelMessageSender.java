package net.blancworks.figura.network.messages.pubsub;

import com.google.common.io.LittleEndianDataOutputStream;
import net.blancworks.figura.network.messages.MessageSender;

import java.io.IOException;
import java.util.UUID;

public class ChannelMessageSender extends MessageSender {
    
    UUID id;
    
    public ChannelMessageSender(UUID id){
        this.id = id;
    }

    @Override
    protected void write(LittleEndianDataOutputStream stream) throws IOException {
        writeUUID(id, stream);
    }

    @Override
    public String getProtocolName() {
        return null;
    }
}
