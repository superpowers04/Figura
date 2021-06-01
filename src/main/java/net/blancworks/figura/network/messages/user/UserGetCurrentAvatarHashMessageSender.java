package net.blancworks.figura.network.messages.user;

import com.google.common.io.LittleEndianDataOutputStream;
import net.blancworks.figura.network.messages.MessageSender;

import java.io.IOException;
import java.util.UUID;

public class UserGetCurrentAvatarHashMessageSender extends MessageSender {
    public UUID id;
    
    public UserGetCurrentAvatarHashMessageSender(UUID id) {
        this.id = id;
    }

    @Override
    public String getProtocolName() {
        return "figura_v1:user_get_current_avatar_hash";
    }

    @Override
    protected void write(LittleEndianDataOutputStream stream) throws IOException {
        super.write(stream);
        
        writeUUID(id, stream);
    }
}
