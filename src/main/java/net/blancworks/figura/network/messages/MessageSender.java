package net.blancworks.figura.network.messages;

import com.google.common.io.LittleEndianDataOutputStream;
import com.neovisionaries.ws.client.WebSocket;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class MessageSender {

    public byte messageID;

    public byte[] header;
    public byte[] body;

    public MessageSender(byte messageID) {
        this.messageID = messageID;
    }

    public void sendMessage(WebSocket socket) {
        //Build body.
        try {
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            LittleEndianDataOutputStream outWriter = new LittleEndianDataOutputStream(outStream);

            writeBody(outWriter);

            //If there's any data for the body, save it.
            if (outStream.size() != 0)
                body = outStream.toByteArray();

            outWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }


        try {
            //Build header.
            {
                ByteArrayOutputStream outStream = new ByteArrayOutputStream();
                LittleEndianDataOutputStream outWriter = new LittleEndianDataOutputStream(outStream);

                //Write message ID
                outWriter.write(messageID);

                //If there IS a body, write the length
                if (body != null)
                    outWriter.writeInt(body.length);
                else
                    outWriter.writeInt((int) 0);

                //Append extra header data provided up higher in the inheritence tree.
                writeHeader(outWriter);

                header = outStream.toByteArray();
                outStream.close();
            }

            socket.sendBinary(header, true);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        if (body != null) {
            try {
                socket.sendBinary(body, true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        header = null;
        body = null;
    }

    protected void writeHeader(LittleEndianDataOutputStream stream) throws IOException {
    }

    protected void writeBody(LittleEndianDataOutputStream stream) throws IOException {
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
