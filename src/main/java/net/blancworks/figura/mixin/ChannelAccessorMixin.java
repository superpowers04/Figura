package net.blancworks.figura.mixin;

import net.minecraft.client.sound.Channel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Set;

@Mixin(Channel.class)
public interface ChannelAccessorMixin {

    @Accessor("sources")
    Set<Channel.SourceManager> getSources();
}
