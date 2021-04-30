package net.blancworks.figura.network.messages.avatar;

import com.google.common.io.LittleEndianDataInputStream;
import net.blancworks.figura.FiguraMod;
import net.blancworks.figura.network.NewFiguraNetworkManager;
import net.blancworks.figura.network.messages.MessageHandler;
import net.blancworks.figura.network.messages.user.UserSetAvatarMessageSender;

import java.util.UUID;

//Handles the response sent by a server about what happened when an avatar was uploaded
public class AvatarUploadResponseHandler extends MessageHandler {

    public static final byte SUCCESS_CODE = 0;
    public static final byte FAIL_TOO_MANY_AVATARS = 1;
    public static final byte FAIL_EMPTY_AVATAR = 2;
    public static final byte FAIL_NOT_ENOUGH_SPACE = 3;


    @Override
    public void handleHeader(LittleEndianDataInputStream stream) throws Exception {
        super.handleHeader(stream);

        byte returnCode = stream.readByte();

        switch (returnCode) {
            case SUCCESS_CODE:
                handleSuccess(stream);
                break;
            case FAIL_TOO_MANY_AVATARS:
                handleTooManyAvatars();
            case FAIL_EMPTY_AVATAR:
                handleEmptyAvatar();
            case FAIL_NOT_ENOUGH_SPACE:
                handleNotEnoughSpace();
        }
    }

    //If the avatar was correctly uploaded, read the UUID of the avatar from the stream, then grabs it.
    public void handleSuccess(LittleEndianDataInputStream stream) throws Exception {
        UUID id = readUUID(stream);

        FiguraMod.LOGGER.error("Uploaded avatar sucessfully, UUID is " + id);
        
        new UserSetAvatarMessageSender(id).sendMessage(NewFiguraNetworkManager.currWebSocket);
    }

    public void handleTooManyAvatars() {
        FiguraMod.LOGGER.error("Failed to upload avatar : User Has Too Many Avatars");
    }

    public void handleEmptyAvatar() {
        FiguraMod.LOGGER.error("Failed to upload avatar : Attempted Empty Avatar Upload");
    }

    public void handleNotEnoughSpace() {
        FiguraMod.LOGGER.error("Failed to upload avatar : Not Enough Space On Server");
    }

    @Override
    public boolean expectBody() {
        return false;
    }
}
