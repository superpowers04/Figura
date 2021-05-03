package net.blancworks.figura.network.messages.avatar;

import com.google.common.io.LittleEndianDataOutputStream;
import net.blancworks.figura.network.messages.MessageIDs;
import net.blancworks.figura.network.messages.MessageSender;

import java.io.IOException;
import java.util.UUID;

public class AvatarRequestMessageSender extends MessageSender {
    
    public UUID avatarID;
    
    public AvatarRequestMessageSender(UUID avatarID) {
        super(MessageIDs.AVATAR_REQUEST_MESSAGE_ID);
        
        this.avatarID = avatarID;
    }

    @Override
    protected void write(LittleEndianDataOutputStream stream) throws IOException {
        super.write(stream);
        
        writeUUID(avatarID, stream);
    }
}
