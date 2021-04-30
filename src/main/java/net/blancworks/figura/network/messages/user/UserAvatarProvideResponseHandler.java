package net.blancworks.figura.network.messages.user;

import com.google.common.io.LittleEndianDataInputStream;
import net.blancworks.figura.PlayerData;
import net.blancworks.figura.PlayerDataManager;
import net.blancworks.figura.network.messages.MessageHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Date;
import java.util.UUID;

public class UserAvatarProvideResponseHandler extends MessageHandler {

    public UUID targetUser;

    @Override
    public void handleHeader(LittleEndianDataInputStream stream) throws Exception {
        super.handleHeader(stream);

        targetUser = readUUID(stream);
    }

    @Override
    public void handleBody(LittleEndianDataInputStream stream) throws Exception {
        super.handleBody(stream);

        try {
            byte[] allAvatarData = new byte[bodyLength];
            stream.read(allAvatarData, 0, bodyLength);

            ByteArrayInputStream bis = new ByteArrayInputStream(allAvatarData);
            DataInputStream dis = new DataInputStream(bis);

            CompoundTag tag = NbtIo.readCompressed(dis);

            dis.close();

            PlayerData pData = PlayerDataManager.getDataForPlayer(targetUser);

            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = md.digest(allAvatarData);

            String hashString = new String(hashBytes, StandardCharsets.UTF_8);

            pData.loadFromNbt(tag);
            pData.lastHash = hashString;
            pData.lastHashCheckTime = new Date(new Date().getTime() - (1000 * 1000));
            pData.saveToCache(targetUser);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean expectBody() {
        return true;
    }
}
