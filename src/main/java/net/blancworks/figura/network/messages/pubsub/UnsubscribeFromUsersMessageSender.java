package net.blancworks.figura.network.messages.pubsub;

import com.google.common.io.LittleEndianDataOutputStream;
import net.blancworks.figura.network.messages.MessageSender;

import java.io.IOException;
import java.util.UUID;

public class UnsubscribeFromUsersMessageSender extends MessageSender {

    public UUID[] ids;

    public UnsubscribeFromUsersMessageSender(UUID... ids) {
        this.ids = ids;
    }

    @Override
    protected void write(LittleEndianDataOutputStream stream) throws IOException {
        stream.writeInt(ids.length);

        for(int i = 0; i < ids.length; i++){
            writeUUID(ids[i], stream);
        }
    }

    @Override
    public String getProtocolName() {
        return "figura_v1:user_events_unsub";
    }
}