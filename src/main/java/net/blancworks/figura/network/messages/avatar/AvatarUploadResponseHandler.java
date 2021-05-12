package net.blancworks.figura.network.messages.avatar;

import com.google.common.io.LittleEndianDataInputStream;
import net.blancworks.figura.FiguraMod;
import net.blancworks.figura.network.NewFiguraNetworkManager;
import net.blancworks.figura.network.messages.MessageHandler;
import net.blancworks.figura.network.messages.user.UserSetAvatarMessageSender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;

import java.util.UUID;

//Handles the response sent by a server about what happened when an avatar was uploaded
public class AvatarUploadResponseHandler extends MessageHandler {

    public static final byte SUCCESS_CODE = 0;
    public static final byte FAIL_TOO_MANY_AVATARS = 1;
    public static final byte FAIL_EMPTY_AVATAR = 2;
    public static final byte FAIL_NOT_ENOUGH_SPACE = 3;


    @Override
    public void handleMessage(LittleEndianDataInputStream stream) throws Exception {
        super.handleMessage(stream);

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

        FiguraMod.LOGGER.info("Uploaded avatar sucessfully, UUID is " + id);
        MinecraftClient.getInstance().getToastManager().add(new SystemToast(SystemToast.Type.WORLD_BACKUP,
                new TranslatableText("gui.figura.toast.upload.success.title"),
                new LiteralText(""))
        );
        
        new UserSetAvatarMessageSender(id).sendMessage(NewFiguraNetworkManager.currWebSocket);
    }

    public void handleTooManyAvatars() {
        FiguraMod.LOGGER.error("Failed to upload avatar : User Has Too Many Avatars");
        MinecraftClient.getInstance().getToastManager().add(new SystemToast(SystemToast.Type.WORLD_BACKUP,
                new TranslatableText("gui.figura.toast.upload.error.title"),
                new TranslatableText("gui.figura.toast.upload.error.many"))
        );
    }

    public void handleEmptyAvatar() {
        FiguraMod.LOGGER.error("Failed to upload avatar : Attempted Empty Avatar Upload");
        MinecraftClient.getInstance().getToastManager().add(new SystemToast(SystemToast.Type.WORLD_BACKUP,
                new TranslatableText("gui.figura.toast.upload.error.title"),
                new TranslatableText("gui.figura.toast.upload.error.empty"))
        );
    }

    public void handleNotEnoughSpace() {
        FiguraMod.LOGGER.error("Failed to upload avatar : Not Enough Space On Server");
        MinecraftClient.getInstance().getToastManager().add(new SystemToast(SystemToast.Type.WORLD_BACKUP,
                new TranslatableText("gui.figura.toast.upload.error.title"),
                new TranslatableText("gui.figura.toast.upload.error.space"))
        );
    }
}
