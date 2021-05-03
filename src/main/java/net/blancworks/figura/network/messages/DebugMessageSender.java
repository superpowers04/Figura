package net.blancworks.figura.network.messages;

import com.google.common.io.LittleEndianDataOutputStream;

import java.io.IOException;

public class DebugMessageSender extends MessageSender {
    
    public DebugMessageSender() {
        super((byte) 0);
    }
    
    @Override
    protected void write(LittleEndianDataOutputStream stream) throws IOException {
        super.write(stream);
        
        stream.writeInt(123456789);
    }
}
