package net.blancworks.figura.network.messages;

import com.google.common.io.LittleEndianDataInputStream;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class MessageHandler {
    public int bodyLength;
    
    public void handleHeader(LittleEndianDataInputStream stream) throws Exception{
        
    }

    public void handleBody(LittleEndianDataInputStream stream) throws Exception{

    }
    
    public boolean expectBody(){
        return false;
    }
    
    public String readString(LittleEndianDataInputStream stream) throws IOException {
        int length = stream.readInt();
        byte[] strData = new byte[length];
        stream.read(strData);
        
        return new String(strData, StandardCharsets.UTF_8);
    }
    
    public UUID readUUID(LittleEndianDataInputStream stream) throws IOException {
        return UUID.fromString(readString(stream));
    }
}
