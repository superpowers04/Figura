package net.blancworks.figura.network.messages.pubsub;

import com.google.common.io.LittleEndianDataOutputStream;
import net.blancworks.figura.network.messages.MessageSender;

import java.io.IOException;
import java.util.UUID;

public class ChannelAvatarUpdateMessageSender extends ChannelMessageSender {

    public ChannelAvatarUpdateMessageSender(UUID id) {
        super(id);
    }

    @Override
    public String getProtocolName() {
        return "figura_v1:channel_avatar_update";
    }
}
