package net.blancworks.figura.network.messages;

import com.google.common.io.LittleEndianDataOutputStream;
import com.neovisionaries.ws.client.WebSocket;

import java.io.DataOutputStream;
import java.io.IOException;

public class DebugMessageSender extends MessageSender {
    
    public DebugMessageSender() {
        super((byte) 0);
    }
    
    @Override
    protected void writeHeader(LittleEndianDataOutputStream stream) throws IOException {
        super.writeHeader(stream);
        
        stream.writeInt(123456789);
    }

    @Override
    protected void writeBody(LittleEndianDataOutputStream stream) throws IOException {
        stream.writeInt(987654321);
    }
}
