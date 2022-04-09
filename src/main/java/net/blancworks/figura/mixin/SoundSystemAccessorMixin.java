package net.blancworks.figura.mixin;

import net.minecraft.client.sound.Channel;
import net.minecraft.client.sound.SoundEngine;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(SoundSystem.class)
public interface SoundSystemAccessorMixin {

    @Accessor("sources")
    Map<SoundInstance, Channel.SourceManager> getSources();

    @Accessor("soundEngine")
    SoundEngine getEngine();
}
