package net.blancworks.figura.network.messages.pings;

import com.google.common.io.LittleEndianDataInputStream;
import net.blancworks.figura.avatar.AvatarData;
import net.blancworks.figura.avatar.AvatarDataManager;
import net.blancworks.figura.lua.api.network.LuaNetworkReadWriter;
import net.blancworks.figura.network.messages.pubsub.ChannelMessageHandler;
import org.luaj.vm2.LuaValue;

public class PingMessageHandler extends ChannelMessageHandler {
    
    @Override
    public void handleMessage(LittleEndianDataInputStream stream) throws Exception {
        super.handleMessage(stream);
        
        short count = (short) Math.max(Math.min(stream.readShort(), 32), 0);
        
        AvatarData data = AvatarDataManager.getDataForPlayer(senderID);

        if (data != null && data.script != null) {
            for (int i = 0; i < count; i++) {
                short id = stream.readShort();
                LuaValue val = LuaNetworkReadWriter.readLuaValue(stream);
                data.script.handlePing(id, val);
            }
        }
    }

    @Override
    public String getProtocolName() {
        return "figura_v1:ping_handle";
    }
}
