package net.blancworks.figura.network.messages;

import com.google.common.io.LittleEndianDataInputStream;
import net.blancworks.figura.FiguraMod;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class MessageRegistry {
    private Map<String, Byte> mapping = new HashMap<>();

    public void readRegistryMessage(LittleEndianDataInputStream stream) throws IOException {
        int count = stream.readInt();
        FiguraMod.LOGGER.debug("Received server registry message! {} handlers", count);
        byte lastId = Byte.MIN_VALUE + 1;
        for (int i = 0; i < count; i++) {
            int nameLength = stream.readInt();
            byte[] nameData = new byte[nameLength];
            stream.read(nameData);

            String protocolName = new String(nameData, StandardCharsets.UTF_8);
            mapping.put(protocolName, lastId);
            FiguraMod.LOGGER.debug("{} is mapped to sbyte 0x{}", protocolName, String.format("%02X", lastId));
            lastId++;
        }
    }

    public boolean isEmpty() {
        return mapping.isEmpty();
    }

    public byte getMessageId(String protocolName) {
        return mapping.get(protocolName);
    }
}
