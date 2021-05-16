package net.blancworks.figura.network.messages.user;

import net.blancworks.figura.network.messages.MessageSender;

public class UserDeleteCurrentAvatarMessageSender extends MessageSender {
    @Override
    public String getProtocolName() {
        return "figura_v1:user_delete_current_avatar";
    }
}
