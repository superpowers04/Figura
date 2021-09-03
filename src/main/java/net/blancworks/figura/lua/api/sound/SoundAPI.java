package net.blancworks.figura.lua.api.sound;

import net.blancworks.figura.access.SoundManagerAccess;
import net.blancworks.figura.access.SoundSystemAccess;
import net.blancworks.figura.lua.CustomScript;
import net.blancworks.figura.lua.api.ReadOnlyLuaTable;
import net.blancworks.figura.lua.api.math.LuaVector;
import net.blancworks.figura.trust.PlayerTrustManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.Channel;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.VarArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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
                    Map<SoundInstance, Channel.SourceManager> sources = ((SoundSystemAccess) ((SoundManagerAccess) MinecraftClient.getInstance().getSoundManager()).getSoundSystem()).getSources();

                    ArrayList<String> songs = new ArrayList<>();

                    for (SoundInstance sound : sources.keySet()) {
                        if (!songs.contains(sound.getId().toString()))
                            songs.add(sound.getId().toString());
                    }

                    int i = 1;
                    LuaTable tbl = new LuaTable();

                    for (String song : songs) {
                        tbl.set(i, song);
                        i++;
                    }

                    return tbl;
                }
            });

        }});
    }

    public static void playSound(CustomScript script, LuaValue arg1, LuaValue arg2, LuaValue arg3) {
        if (script.soundSpawnCount > script.playerData.getTrustContainer().getIntSetting(PlayerTrustManager.MAX_SOUND_EFFECTS_ID))
            return;
        script.soundSpawnCount++;

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
