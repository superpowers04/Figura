package net.blancworks.figura.network.messages;

import com.google.common.io.LittleEndianDataOutputStream;
import com.neovisionaries.ws.client.WebSocket;
import net.blancworks.figura.network.NewFiguraNetworkManager;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public abstract class MessageSender {

    public byte[] message;

    public void sendMessage(WebSocket socket) {
        try {
            //Build message.
            {
                ByteArrayOutputStream outStream = new ByteArrayOutputStream();
                LittleEndianDataOutputStream outWriter = new LittleEndianDataOutputStream(outStream);

                String pcName = getProtocolName(); 
                
                try {
                    //Write message ID
                    outWriter.write(NewFiguraNetworkManager.msgRegistry.getMessageId(pcName));
                } catch (Exception e){
                    e.printStackTrace();
                    return;
                }

                //Append extra header data provided up higher in the inheritence tree.
                write(outWriter);

                message = outStream.toByteArray();
                outStream.close();
            }

            socket.sendBinary(message, true);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        message = null;
    }

    public abstract String getProtocolName();

    protected void write(LittleEndianDataOutputStream stream) throws IOException {
    }

    public void writeString(String str, LittleEndianDataOutputStream stream) throws IOException {
        byte[] data = str.getBytes(StandardCharsets.UTF_8);

        stream.writeInt(data.length);
        stream.write(data);
    }
    
    public void writeUUID(UUID id, LittleEndianDataOutputStream stream) throws IOException {
        writeString(id.toString(), stream);
    }
}
