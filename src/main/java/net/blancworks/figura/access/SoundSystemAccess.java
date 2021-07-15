package net.blancworks.figura.access;

import net.minecraft.client.sound.Channel;
import net.minecraft.client.sound.SoundInstance;

import java.util.Map;

public interface SoundSystemAccess {
    Map<SoundInstance, Channel.SourceManager> getSources();
}
