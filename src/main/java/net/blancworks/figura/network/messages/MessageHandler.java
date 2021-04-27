package net.blancworks.figura.network.messages;

import com.google.common.io.LittleEndianDataInputStream;

import java.io.DataInputStream;

public class MessageHandler {
    public void handleHeader(LittleEndianDataInputStream stream){
        
    }

    public void handleBody(LittleEndianDataInputStream stream){

    }
    
    public boolean expectBody(){
        return false;
    }
}
