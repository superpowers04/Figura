package net.blancworks.figura.lua.api.sound;

import net.blancworks.figura.PlayerData;
import net.blancworks.figura.access.SourceManagerAccessor;
import net.blancworks.figura.lua.CustomScript;
import net.blancworks.figura.mixin.ChannelAccessorMixin;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.sound.Channel;
import net.minecraft.client.sound.SoundEngine;
import net.minecraft.client.sound.Source;
import net.minecraft.client.sound.StaticSound;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.Vec3d;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaValue;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.luaj.vm2.LuaValue.NIL;

public class FiguraChannel extends Channel {

    private final HashSet<UUID> playersToRemove = new HashSet<>();
    private boolean stopAllSounds = false;

    public FiguraChannel() {
        super(SoundAPI.getSoundEngine(), Runnable::run);
    }

    public Set<SourceManager> getSourceManagers() {
        return ((ChannelAccessorMixin)this).getSources();
    }

    public Source getSource(SourceManager sourceManager) {
        return ((SourceManagerAccessor)sourceManager).getSource();
    }
    public UUID getSourceOwner(SourceManager sourceManager) {
        return ((SourceManagerAccessor)sourceManager).getOwner();
    }

    @Override
    public void tick() {
        GameOptions options = MinecraftClient.getInstance().options;
        if (options.getSoundVolume(SoundCategory.PLAYERS) <= 0 || options.getSoundVolume(SoundCategory.MASTER) <= 0)
            stopAllSounds = true;

        Set<SourceManager> sources = getSourceManagers();
        Iterator<SourceManager> iterator = sources.iterator();
        ArrayList<SourceManager> managersToRemove = new ArrayList<>();

        while(iterator.hasNext()) {
            SourceManager sourceManager = iterator.next();
            Source src = getSource(sourceManager);

            UUID owner = getSourceOwner(sourceManager);
            boolean removePlayer = playersToRemove.contains(owner);
            if (src == null || removePlayer || stopAllSounds) {
                managersToRemove.add(sourceManager);
                if (src != null) {
                    src.stop();
                    iterator.remove();
                }

                continue;
            }

            src.tick();
            if (src.isStopped()) {
                sourceManager.close();
                iterator.remove();
            }
        }
        stopAllSounds = false;

        managersToRemove.forEach(sources::remove);
        managersToRemove.clear();
        playersToRemove.clear();
    }

    public CompletableFuture<SourceManager> createSource(PlayerData soundOwner, String name, SoundEngine.RunMode mode) {
        CompletableFuture<SourceManager> future = super.createSource(mode);
        future.thenApply((sm) -> {
            ((SourceManagerAccessor)sm).setOwner(soundOwner.lastEntity.getUuid());
            ((SourceManagerAccessor)sm).setName(name);
            return sm;
        });

        return future;
    }

    public LuaValue playCustomSound(CustomScript script, String soundName, Vec3d pos, float pitch, float volume) {
        FiguraSound sound = script.customSounds.get(soundName);
        if (sound == null) {
            throw new LuaError("Custom sound \"" + soundName + "\" is not defined, or cannot be empty!");
        }

        createSource(script.playerData, soundName, SoundEngine.RunMode.STATIC).thenAccept(sourceManager -> sourceManager.run(source -> {
            if (source != null) {
                source.setBuffer(sound.sound());
                source.setPosition(pos);
                source.setPitch(pitch);
                source.setVolume(volume);
                source.play();
            }
        }));

        return NIL;
    }

    public void stopForPlayer(UUID id) {
        playersToRemove.add(id);
    }

    public void stopAllSounds() {
        stopAllSounds = true;
    }

}
