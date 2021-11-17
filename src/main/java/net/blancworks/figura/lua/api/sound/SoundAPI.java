package net.blancworks.figura.lua.api.sound;

import net.blancworks.figura.access.SourceManagerAccessor;
import net.blancworks.figura.lua.CustomScript;
import net.blancworks.figura.lua.api.ReadOnlyLuaTable;
import net.blancworks.figura.lua.api.math.LuaVector;
import net.blancworks.figura.mixin.SoundManagerAccessorMixin;
import net.blancworks.figura.mixin.SoundSystemAccessorMixin;
import net.blancworks.figura.models.sounds.FiguraSoundManager;
import net.blancworks.figura.trust.TrustContainer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.Channel;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.ThreeArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SoundAPI {

    public static HashMap<String, SoundEvent> soundEvents = new HashMap<>() {{
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
            set("playSound", new ThreeArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1, LuaValue arg2, LuaValue arg3) {
                    LuaValue vol = arg3.isnil() ? new LuaVector(1f, 1f) : arg3;
                    playSound(script, arg1, arg2, vol);
                    return NIL;
                }
            });

            set("getSounds", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    Map<SoundInstance, Channel.SourceManager> sources = ((SoundSystemAccessorMixin) ((SoundManagerAccessorMixin) MinecraftClient.getInstance().getSoundManager()).getSoundSystem()).getSources();

                    ReadOnlyLuaTable tbl = new ReadOnlyLuaTable();
                    int i = 1;
                    for (SoundInstance sound : sources.keySet()) {
                        tbl.javaRawSet(i, LuaString.valueOf(sound.getId().toString()));
                        i++;
                    }
                    return tbl;
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
                            bytes[i] = (byte) bufTable.get(i+1).checkint();
                        }
                    } else {
                        bytes = Base64.getDecoder().decode(arg2.checkjstring());
                    }

                    FiguraSoundManager.registerCustomSound(script, arg1.checkjstring(), bytes, true);

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
                    return FiguraSoundManager.figuraChannel.playCustomSound(script, arg1.checkjstring(), LuaVector.checkOrNew(arg2).asV3d(), pitchVol.x(), pitchVol.y());
                }
            });

            set("getCustomSounds", new OneArgFunction() {
                @Override
                public LuaTable call(LuaValue arg) {
                    boolean showUUIDs = !arg.isnil(1) && arg.checkboolean(1);

                    ReadOnlyLuaTable tbl = new ReadOnlyLuaTable();
                    int i = 1;
                    for (Channel.SourceManager sourceManager : FiguraSoundManager.figuraChannel.getSourceManagers()) {
                        String soundName = ((SourceManagerAccessor)sourceManager).getName();
                        tbl.javaRawSet(i, LuaString.valueOf(soundName));
                        i++;

                        if (showUUIDs) {
                            UUID owner = FiguraSoundManager.figuraChannel.getSourceOwner(sourceManager);
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

    public static void playSound(CustomScript script, LuaValue arg1, LuaValue arg2, LuaValue arg3) {
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
}
