package net.blancworks.figura.network.messages.user;

import com.google.common.io.LittleEndianDataInputStream;
import net.blancworks.figura.FiguraMod;
import net.blancworks.figura.avatar.AvatarData;
import net.blancworks.figura.avatar.AvatarDataManager;
import net.blancworks.figura.config.ConfigManager.Config;
import net.blancworks.figura.network.messages.MessageHandler;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

@SuppressWarnings("UnstableApiUsage")
public class UserAvatarProvideResponseHandler extends MessageHandler {

    public UUID targetUser;

    @Override
    public void handleMessage(LittleEndianDataInputStream stream) throws Exception {
        super.handleMessage(stream);

        targetUser = readUUID(stream);

        try {
            int avatarLength = stream.readInt();
            byte[] allAvatarData = new byte[avatarLength];
            stream.read(allAvatarData, 0, avatarLength);

            ByteArrayInputStream bis = new ByteArrayInputStream(allAvatarData);
            DataInputStream dis = new DataInputStream(bis);

            NbtCompound tag = NbtIo.readCompressed(dis);

            dis.close();

            AvatarData pData = AvatarDataManager.getDataForPlayer(targetUser);
            if (pData == null) return;

            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = md.digest(allAvatarData);

            String hashString = new String(hashBytes, StandardCharsets.UTF_8);

            pData.loadFromNbt((boolean) Config.EASTER_EGGS.value && FiguraMod.IS_CHEESE ? FiguraMod.cheese : tag);
            pData.isLocalAvatar = false;
            pData.lastHash = hashString;
            //pData.saveToCache();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getProtocolName() {
        return "figura_v1:user_avatar_provide";
    }
}
