package net.blancworks.figura.network.messages.user;

import com.google.common.io.LittleEndianDataOutputStream;
import net.blancworks.figura.network.messages.MessageIDs;
import net.blancworks.figura.network.messages.MessageSender;

import java.io.IOException;
import java.util.UUID;

//Tells the Figura Server to set our current avatar to the avatar at this ID.
public class UserSetAvatarMessageSender extends MessageSender {

    public UUID id;
    public boolean deletePreviousAvatar;
    
    public UserSetAvatarMessageSender(UUID id) {
        super(MessageIDs.USER_SET_CURRENT_AVATAR_MESSAGE_ID);
        this.id = id;
        deletePreviousAvatar = true;
    }

    public UserSetAvatarMessageSender(UUID id, boolean deletePreviousAvatar) {
        super(MessageIDs.USER_SET_CURRENT_AVATAR_MESSAGE_ID);
        this.id = id;
        this.deletePreviousAvatar = deletePreviousAvatar;
    }

    @Override
    protected void writeHeader(LittleEndianDataOutputStream stream) throws IOException {
        super.writeHeader(stream);
        
        writeUUID(id, stream);
        
        stream.writeByte(deletePreviousAvatar ? 1 : 0);
    }
}
