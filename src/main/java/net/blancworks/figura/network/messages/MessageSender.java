package net.blancworks.figura.network.messages;

import com.google.common.io.LittleEndianDataOutputStream;
import com.neovisionaries.ws.client.WebSocket;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class MessageSender {

    public byte messageID;

    public byte[] message;

    public MessageSender(byte messageID) {
        this.messageID = messageID;
    }

    public void sendMessage(WebSocket socket) {
        try {
            //Build message.
            {
                ByteArrayOutputStream outStream = new ByteArrayOutputStream();
                LittleEndianDataOutputStream outWriter = new LittleEndianDataOutputStream(outStream);

                //Write message ID
                outWriter.write(messageID);

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
