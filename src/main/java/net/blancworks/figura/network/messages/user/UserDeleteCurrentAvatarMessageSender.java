package net.blancworks.figura.network.messages.user;

import net.blancworks.figura.network.messages.MessageIDs;
import net.blancworks.figura.network.messages.MessageSender;

public class UserDeleteCurrentAvatarMessageSender extends MessageSender {
    public UserDeleteCurrentAvatarMessageSender() {
        super(MessageIDs.USER_DELETE_CURRENT_AVATAR_MESSAGE_ID);
    }
}
