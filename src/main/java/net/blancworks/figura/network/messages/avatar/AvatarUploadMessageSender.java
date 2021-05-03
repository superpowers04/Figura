package net.blancworks.figura.network.messages.avatar;

import com.google.common.io.LittleEndianDataOutputStream;
import net.blancworks.figura.network.messages.MessageIDs;
import net.blancworks.figura.network.messages.MessageSender;

import java.io.IOException;

public class AvatarUploadMessageSender extends MessageSender {
    
    //The binary data representing the avatar's NBT data.
    private byte[] avatarPayload;
    
    public AvatarUploadMessageSender(byte[] payload, boolean deletePrevious) {
        super(MessageIDs.AVATAR_UPLOAD_MESSAGE_ID);
        this.avatarPayload = payload;
    }

    public AvatarUploadMessageSender(byte[] payload) {
        super(MessageIDs.AVATAR_UPLOAD_MESSAGE_ID);
        this.avatarPayload = payload;
    }

    @Override
    protected void write(LittleEndianDataOutputStream stream) throws IOException {
        super.write(stream);

        //Write the main payload for the avatar.
        stream.writeInt(avatarPayload.length);
        stream.write(avatarPayload);
    }
}
