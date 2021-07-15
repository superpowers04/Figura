package net.blancworks.figura.mixin;

import net.blancworks.figura.access.SoundSystemAccess;
import net.minecraft.client.sound.Channel;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundSystem;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;

@Mixin(SoundSystem.class)
public class SoundSystemMixin implements SoundSystemAccess {
    @Shadow @Final private Map<SoundInstance, Channel.SourceManager> sources;

    @Override
    public Map<SoundInstance, Channel.SourceManager> getSources() {
        return this.sources;
    }
}
