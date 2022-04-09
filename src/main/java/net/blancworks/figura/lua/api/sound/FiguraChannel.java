package net.blancworks.figura.lua.api.sound;

import net.blancworks.figura.avatar.AvatarData;
import net.blancworks.figura.access.SourceManagerAccessor;
import net.blancworks.figura.lua.CustomScript;
import net.blancworks.figura.mixin.ChannelAccessorMixin;
import net.blancworks.figura.trust.TrustContainer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.sound.Channel;
import net.minecraft.client.sound.SoundEngine;
import net.minecraft.client.sound.Source;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.luaj.vm2.LuaError;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class FiguraChannel extends Channel {

    private final HashSet<StopSoundRequest> stopSoundRequests = new HashSet<>();
    private boolean stopAllSounds = false;

    public FiguraChannel() {
        super(FiguraSoundManager.getSoundEngine(), Runnable::run);
    }

    public Set<SourceManager> getSourceManagers() {
        return ((ChannelAccessorMixin) this).getSources();
    }

    public static Source getSource(SourceManager sourceManager) {
        return ((SourceManagerAccessor) sourceManager).getSource();
    }

    public static UUID getSourceOwner(SourceManager sourceManager) {
        return ((SourceManagerAccessor) sourceManager).getOwner();
    }

    public static String getSourceName(SourceManager sourceManager) {
        return ((SourceManagerAccessor) sourceManager).getName();
    }

    @Override
    public void tick() {
        GameOptions options = MinecraftClient.getInstance().options;
        if (options.getSoundVolume(SoundCategory.PLAYERS) <= 0 || options.getSoundVolume(SoundCategory.MASTER) <= 0)
            stopAllSounds = true;

        Set<SourceManager> sources = getSourceManagers();
        for (SourceManager sourceManager : new HashSet<>(sources)) {
            Source src = getSource(sourceManager);

            boolean removePlayer = stopSoundRequests.stream().anyMatch(req -> req.shouldStop(sourceManager));
            if (src == null || removePlayer || stopAllSounds) {
                if (src != null)
                    src.stop();
                sourceManager.close();
                sources.remove(sourceManager);
                continue;
            }

            src.tick();
            if (src.isStopped()) {
                sourceManager.close();
                sources.remove(sourceManager);
            }
        }

        stopSoundRequests.clear();
        stopAllSounds = false;
    }

    public CompletableFuture<SourceManager> createSource(AvatarData soundOwner, String name, SoundEngine.RunMode mode) {
        CompletableFuture<SourceManager> future = super.createSource(mode);
        future.thenApply((sm) -> {
            ((SourceManagerAccessor) sm).setOwner(soundOwner.lastEntity.getUuid());
            ((SourceManagerAccessor) sm).setName(name);
            return sm;
        });

        return future;
    }

    public void playCustomSound(CustomScript script, String soundName, Vec3d pos, float volume, float pitch) {
        if (script.avatarData.getTrustContainer().getTrust(TrustContainer.Trust.CUSTOM_SOUNDS) == 0 || script.soundSpawnCount < 1 || pitch <= 0f || volume <= 0f) return;
        script.soundSpawnCount--;

        FiguraSound sound = script.customSounds.get(soundName);
        if (sound == null)
            throw new LuaError("Custom sound \"" + soundName + "\" is not defined, or cannot be empty!");

        createSource(script.avatarData, soundName, SoundEngine.RunMode.STATIC).thenAccept(sourceManager -> sourceManager.run(source -> {
            if (source != null) {
                source.setAttenuation(Math.max(volume * 16f, 16f));
                source.setBuffer(sound.sound());
                source.setPosition(pos);
                source.setVolume(MathHelper.clamp(volume * MinecraftClient.getInstance().options.getSoundVolume(SoundCategory.PLAYERS), 0f, 1f));
                source.setPitch(pitch);
                source.play();
            }
        }));
    }

    public void stopAllSounds() {
        stopAllSounds = true;
        stopSoundRequests.clear();
    }

    public void stopSound(String soundName, UUID ownerId) {
        stopSoundRequests.add(new StopSoundRequest(soundName, ownerId));
    }

    public void stopSound(String soundName) {
        stopSoundRequests.add(new StopSoundRequest(soundName));
    }

    public void stopSound(UUID ownerId) {
        stopSoundRequests.add(new StopSoundRequest(ownerId));
    }

    protected class StopSoundRequest {
        private final String soundName;
        private final UUID ownerId;

        public StopSoundRequest(String soundName) {
            this(soundName, null);
        }

        public StopSoundRequest(UUID ownerId) {
            this(null, ownerId);
        }

        public StopSoundRequest(String soundName, UUID ownerId) {
            this.soundName = soundName;
            this.ownerId = ownerId;
        }

        protected boolean shouldStop(SourceManager sourceManager) {
            boolean a = ownerId   == null || getSourceOwner(sourceManager).equals(ownerId);
            boolean b = soundName == null || getSourceName(sourceManager).equals(soundName);
            return a && b;
        }
    }
}
