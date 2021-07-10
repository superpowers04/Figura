package net.blancworks.figura.utils;

import net.blancworks.figura.PlayerData;
import net.blancworks.figura.PlayerDataManager;
import net.blancworks.figura.lua.api.sound.PlayerSoundCustomization;
import net.minecraft.entity.Entity;
import net.minecraft.sound.SoundEvent;

public class SoundUtils {

    public static SoundEvent getSoundOverride(Entity self, SoundEvent sound) {
        if (sound == null)
            return null;

        PlayerData data = PlayerDataManager.getDataForPlayer(self.getUuid());
        if (data == null || data.script == null)
            return null;

        PlayerSoundCustomization customization = data.script.getOrMakePlayerSoundCustomization(sound.getId());
        if (customization == null)
            return null;

        return customization.soundEvent;
    }

}