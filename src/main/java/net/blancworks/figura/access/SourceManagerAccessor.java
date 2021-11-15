package net.blancworks.figura.access;

import net.minecraft.client.sound.Source;

import java.util.UUID;

public interface SourceManagerAccessor {

    Source getSource();
    UUID getOwner();
    void setOwner(UUID data);
    String getName();
    void setName(String name);

}
