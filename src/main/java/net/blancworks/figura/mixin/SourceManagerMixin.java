package net.blancworks.figura.mixin;

import net.blancworks.figura.access.SourceManagerAccessor;
import net.minecraft.client.sound.Channel;
import net.minecraft.client.sound.Source;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.UUID;


@Mixin(Channel.SourceManager.class)
public class SourceManagerMixin implements SourceManagerAccessor {

    @Shadow @Nullable Source source;
    @Unique private UUID owner;
    @Unique private String name;

    @Override
    public @Nullable Source getSource() {
        return this.source;
    }

    @Override
    public UUID getOwner() {
        return this.owner;
    }

    @Override
    public void setOwner(UUID data) {
        this.owner = data;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }
}
