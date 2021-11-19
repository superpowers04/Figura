package net.blancworks.figura.models.sounds;

import net.blancworks.figura.PlayerData;
import net.blancworks.figura.access.SourceManagerAccessor;
import net.blancworks.figura.lua.CustomScript;
import net.blancworks.figura.mixin.ChannelAccessorMixin;
import net.blancworks.figura.trust.TrustContainer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.sound.*;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.Vec3d;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaValue;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.luaj.vm2.LuaValue.NIL;

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
        Iterator<SourceManager> iterator = sources.iterator();
        ArrayList<SourceManager> managersToRemove = new ArrayList<>();

        while(iterator.hasNext()) {
            SourceManager sourceManager = iterator.next();
            Source src = getSource(sourceManager);

            boolean removePlayer = stopSoundRequests.stream().anyMatch(req -> req.shouldStop(sourceManager));
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
        stopSoundRequests.clear();
    }

    public CompletableFuture<SourceManager> createSource(PlayerData soundOwner, String name, SoundEngine.RunMode mode) {
        CompletableFuture<SourceManager> future = super.createSource(mode);
        future.thenApply((sm) -> {
            ((SourceManagerAccessor) sm).setOwner(soundOwner.lastEntity.getUuid());
            ((SourceManagerAccessor) sm).setName(name);
            return sm;
        });

        return future;
    }

    public LuaValue playCustomSound(CustomScript script, String soundName, Vec3d pos, float pitch, float volume) {
        if (script.playerData.getTrustContainer().getTrust(TrustContainer.Trust.CUSTOM_SOUNDS) == 0 || script.soundSpawnCount < 1) return NIL;
        script.soundSpawnCount--;

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
            boolean a = ownerId   != null && getSourceOwner(sourceManager).equals(ownerId);
            boolean b = soundName != null && getSourceName(sourceManager).equals(soundName);
            return a && b;
        }
    }
}
