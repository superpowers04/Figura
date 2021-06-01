package net.blancworks.figura.network.messages.user;

import com.google.common.io.LittleEndianDataOutputStream;
import net.blancworks.figura.network.messages.MessageSender;

import java.io.IOException;
import java.util.UUID;

//Tells the Figura Server to set our current avatar to the avatar at this ID.
public class UserSetAvatarMessageSender extends MessageSender {

    public UUID id;
    public boolean deletePreviousAvatar;
    
    public UserSetAvatarMessageSender(UUID id) {
        this.id = id;
        deletePreviousAvatar = true;
    }

    public UserSetAvatarMessageSender(UUID id, boolean deletePreviousAvatar) {
        this.id = id;
        this.deletePreviousAvatar = deletePreviousAvatar;
    }

    @Override
    public String getProtocolName() {
        return "figura_v1:user_set_avatar";
    }

    @Override
    protected void write(LittleEndianDataOutputStream stream) throws IOException {
        super.write(stream);
        
        writeUUID(id, stream);
        
        stream.writeByte(deletePreviousAvatar ? 1 : 0);
    }
}
