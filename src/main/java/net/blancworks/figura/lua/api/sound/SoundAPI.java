package net.blancworks.figura.lua.api.sound;

import net.blancworks.figura.access.SourceManagerAccessor;
import net.blancworks.figura.lua.CustomScript;
import net.blancworks.figura.lua.api.ReadOnlyLuaTable;
import net.blancworks.figura.lua.api.math.LuaVector;
import net.blancworks.figura.mixin.SoundManagerAccessorMixin;
import net.blancworks.figura.mixin.SoundSystemAccessorMixin;
import net.blancworks.figura.trust.TrustContainer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.*;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class SoundAPI {
    public static FiguraChannel figuraChannel;

    public static HashMap<String, SoundEvent> soundEvents = new HashMap<String, SoundEvent>() {{
        for (Identifier id : Registry.SOUND_EVENT.getIds()) {
            SoundEvent type = Registry.SOUND_EVENT.get(id);

            put(id.getPath(), type);
            put(id.toString(), type);
        }
    }};


    public static Identifier getID() {
        return new Identifier("default", "sound");
    }

    public static ReadOnlyLuaTable getForScript(CustomScript script) {
        return new ReadOnlyLuaTable(new LuaTable() {{
            set("playSound", new VarArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1, LuaValue arg2) {
                    playSound(script, arg1, arg2, new LuaVector(1.0f, 1.0f));
                    return NIL;
                }

                @Override
                public LuaValue call(LuaValue arg1, LuaValue arg2, LuaValue arg3) {
                    playSound(script, arg1, arg2, arg3);
                    return NIL;
                }
            });

            set("getSounds", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    Map<SoundInstance, Channel.SourceManager> sources = ((SoundSystemAccessorMixin) ((SoundManagerAccessorMixin) MinecraftClient.getInstance().getSoundManager()).getSoundSystem()).getSources();
                    return makeSoundMapTable(sources);
                }
            });

            set("registerCustomSound", new TwoArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1, LuaValue arg2) {
                    byte[] bytes;

                    if (arg2.istable()) {
                        LuaTable bufTable = arg2.checktable();
                        bytes = new byte[bufTable.length()];
                        for(int i = 0; i < bytes.length; i++) {
                            bytes[i] = (byte)bufTable.get(i+1).checkint();
                        }
                    } else {
                        bytes = Base64.getDecoder().decode(arg2.checkjstring());
                    }

                    registerCustomSound(script, arg1.checkjstring(), bytes, true);

                    return NIL;
                }
            });

            set("playCustomSound", new ThreeArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1, LuaValue arg2, LuaValue arg3) {
                    if (script.soundSpawnCount > script.playerData.getTrustContainer().getTrust(TrustContainer.Trust.SOUNDS))
                        return NIL;
                    script.soundSpawnCount++;

                    LuaVector pitchVol = LuaVector.checkOrNew(arg3);
                    return figuraChannel.playCustomSound(script, arg1.checkjstring(), LuaVector.checkOrNew(arg2).asV3d(), pitchVol.x(), pitchVol.y());
                }
            });

            set("getCustomSounds", new VarArgFunction() {
                @Override
                public LuaTable invoke(Varargs args) {
                    boolean showUUIDs = !args.isnil(1) && args.checkboolean(1);

                    ReadOnlyLuaTable tbl = new ReadOnlyLuaTable();
                    int i = 1;
                    for (Channel.SourceManager sourceManager : figuraChannel.getSourceManagers()) {
                        String soundName = ((SourceManagerAccessor)sourceManager).getName();
                        tbl.javaRawSet(i, LuaString.valueOf(soundName));
                        i++;

                        if (showUUIDs) {
                            UUID owner = figuraChannel.getSourceOwner(sourceManager);
                            tbl.javaRawSet(i, LuaString.valueOf(owner.toString()));
                            i++;
                        }
                    }
                    return tbl;
                }
            });

            set("getRegisteredCustomSounds", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    ReadOnlyLuaTable tbl = new ReadOnlyLuaTable();
                    int i = 1;
                    for(String name : script.customSounds.keySet()) {
                        tbl.javaRawSet(i, LuaString.valueOf(name));
                        i++;
                    }
                    return tbl;
                }
            });

        }});
    }

    public static void tick() {
        if (figuraChannel == null)
            figuraChannel = new FiguraChannel();

        figuraChannel.tick();
    }

    public static SoundEngine getSoundEngine() {
        SoundManager soundManager = MinecraftClient.getInstance().getSoundManager();
        SoundManagerAccessorMixin soundManagerAccess = (SoundManagerAccessorMixin) soundManager;
        SoundSystem soundSystem = soundManagerAccess.getSoundSystem();
        SoundSystemAccessorMixin soundSystemAccess = (SoundSystemAccessorMixin) soundSystem;
        return soundSystemAccess.getEngine();
    }

    public static void playSound(@NotNull CustomScript script, LuaValue arg1, LuaValue arg2, LuaValue arg3) {
        if (script.soundSpawnCount < 1)
            return;
        script.soundSpawnCount--;

        SoundEvent targetEvent = soundEvents.get(arg1.checkjstring());
        if (targetEvent == null)
            return;

        LuaVector pos = LuaVector.checkOrNew(arg2);
        LuaVector pitchVol = LuaVector.checkOrNew(arg3);

        World w = MinecraftClient.getInstance().world;
        if (MinecraftClient.getInstance().isPaused() || w == null)
            return;

        w.playSound(
                pos.x(), pos.y(), pos.z(),
                targetEvent, SoundCategory.PLAYERS,
                pitchVol.x(), pitchVol.y(), true
        );
    }

    public static @NotNull ReadOnlyLuaTable makeSoundMapTable(@NotNull Map<SoundInstance, Channel.SourceManager> soundMap) {
        ReadOnlyLuaTable tbl = new ReadOnlyLuaTable();
        int i = 1;
        for (SoundInstance sound : soundMap.keySet()) {
            tbl.javaRawSet(i, LuaString.valueOf(sound.getId().toString()));
            i++;
        }
        return tbl;
    }

    public static void registerCustomSound(@NotNull CustomScript script, String name, byte[] source, boolean local) {
        try {
            System.out.printf("Registered custom sound: \"%s\"\n", name);
            OggAudioStream oggAudioStream = new OggAudioStream(new ByteArrayInputStream(source));
            StaticSound sound = new StaticSound(oggAudioStream.getBuffer(), oggAudioStream.getFormat());
            script.customSounds.put(name, new FiguraSound(sound, name, source, local));
        } catch (IOException e) {
            throw new LuaError(e);
        }
    }

}
