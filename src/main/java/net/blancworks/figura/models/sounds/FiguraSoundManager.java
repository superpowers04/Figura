package net.blancworks.figura.models.sounds;

import net.blancworks.figura.lua.CustomScript;
import net.blancworks.figura.mixin.SoundManagerAccessorMixin;
import net.blancworks.figura.mixin.SoundSystemAccessorMixin;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public class FiguraSoundManager {
    private static FiguraChannel figuraChannel;

    public static FiguraChannel getChannel() {
        if (figuraChannel == null)
            figuraChannel = new FiguraChannel();
        return figuraChannel;
    }

    public static void tick() {
        if (figuraChannel != null) figuraChannel.tick();
    }

    public static SoundEngine getSoundEngine() {
        SoundManager soundManager = MinecraftClient.getInstance().getSoundManager();
        SoundManagerAccessorMixin soundManagerAccess = (SoundManagerAccessorMixin) soundManager;
        SoundSystem soundSystem = soundManagerAccess.getSoundSystem();
        SoundSystemAccessorMixin soundSystemAccess = (SoundSystemAccessorMixin) soundSystem;
        return soundSystemAccess.getEngine();
    }

    public static void registerCustomSound(CustomScript script, String name, byte[] source, boolean local) {
        try {
            OggAudioStream oggAudioStream = new OggAudioStream(new ByteArrayInputStream(source));
            StaticSound sound = new StaticSound(oggAudioStream.getBuffer(), oggAudioStream.getFormat());
            script.customSounds.put(name, new FiguraSound(sound, name, source, local));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
