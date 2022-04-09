package net.blancworks.figura.lua.api.sound;

import net.blancworks.figura.access.SourceManagerAccessor;
import net.blancworks.figura.lua.CustomScript;
import net.blancworks.figura.lua.api.math.LuaVector;
import net.blancworks.figura.mixin.SoundManagerAccessorMixin;
import net.blancworks.figura.mixin.SoundSystemAccessorMixin;
import net.blancworks.figura.trust.TrustContainer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.Channel;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.luaj.vm2.LuaBoolean;
import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.ThreeArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;

import java.util.*;

public class SoundAPI {

    public static Identifier getID() {
        return new Identifier("default", "sound");
    }

    public static LuaTable getForScript(CustomScript script) {
        return new LuaTable() {{
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
                    Map<SoundInstance, Channel.SourceManager> sources = new HashMap<>(((SoundSystemAccessorMixin) ((SoundManagerAccessorMixin) MinecraftClient.getInstance().getSoundManager()).getSoundSystem()).getSources());

                    LuaTable tbl = new LuaTable();
                    int i = 1;
                    for (SoundInstance sound : sources.keySet()) {
                        tbl.set(i, LuaString.valueOf(sound.getId().toString()));
                        i++;
                    }
                    return tbl;
                }
            });

            set("registerCustomSound", new TwoArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1, LuaValue arg2) {
                    if (script.avatarData.getTrustContainer().getTrust(TrustContainer.Trust.CUSTOM_SOUNDS) == 0) return NIL;

                    byte[] bytes;
                    if (arg2.istable()) {
                        LuaTable bufTable = arg2.checktable();
                        bytes = new byte[bufTable.length()];
                        for(int i = 0; i < bytes.length; i++) {
                            bytes[i] = (byte) bufTable.get(i + 1).checkint();
                        }
                    } else {
                        bytes = Base64.getDecoder().decode(arg2.checkjstring());
                    }

                    FiguraSoundManager.registerCustomSound(script, arg1.checkjstring(), bytes, true);

                    return NIL;
                }
            });

            set("isCustomSoundRegistered", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    final String name = arg.checkjstring();
                    return LuaBoolean.valueOf(script.customSounds.keySet().stream().anyMatch(str -> str.equals(name)));
                }
            });

            set("playCustomSound", new ThreeArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1, LuaValue arg2, LuaValue arg3) {
                    LuaVector pitchVol = arg3.isnil() ? new LuaVector(1f, 1f) : LuaVector.checkOrNew(arg3);
                    FiguraSoundManager.getChannel().playCustomSound(script, arg1.checkjstring(), LuaVector.checkOrNew(arg2).asV3d(), pitchVol.x(), pitchVol.y());
                    return NIL;
                }
            });

            set("getCustomSounds", new OneArgFunction() {
                @Override
                public LuaTable call(LuaValue arg) {
                    boolean showUUIDs = !arg.isnil(1) && arg.checkboolean(1);

                    LuaTable tbl = new LuaTable();
                    int i = 1;
                    for (Channel.SourceManager sourceManager : new HashSet<>(FiguraSoundManager.getChannel().getSourceManagers())) {
                        String soundName = ((SourceManagerAccessor)sourceManager).getName();
                        tbl.set(i, LuaString.valueOf(soundName));
                        i++;

                        if (showUUIDs) {
                            UUID owner = FiguraChannel.getSourceOwner(sourceManager);
                            tbl.set(i, LuaString.valueOf(owner.toString()));
                            i++;
                        }
                    }
                    return tbl;
                }
            });

            set("getRegisteredCustomSounds", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    LuaTable tbl = new LuaTable();
                    int i = 1;
                    for(String name : script.customSounds.keySet()) {
                        tbl.set(i, LuaString.valueOf(name));
                        i++;
                    }
                    return tbl;
                }
            });

            set("stopCustomSound", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    FiguraSoundManager.getChannel().stopSound(arg.checkjstring(), script.avatarData.entityId);
                    return NIL;
                }
            });
        }};
    }

    public static void playSound(CustomScript script, LuaValue arg1, LuaValue arg2, LuaValue arg3) {
        World w = MinecraftClient.getInstance().world;
        if (MinecraftClient.getInstance().isPaused() || w == null)
            return;

        if (script.soundSpawnCount < 1)
            return;
        script.soundSpawnCount--;

        SoundEvent targetEvent = new SoundEvent(new Identifier(arg1.checkjstring()));
        LuaVector pos = LuaVector.checkOrNew(arg2);
        LuaVector pitchVol = LuaVector.checkOrNew(arg3);

        w.playSound(
                pos.x(), pos.y(), pos.z(),
                targetEvent, SoundCategory.PLAYERS,
                pitchVol.x(), pitchVol.y(), true
        );
    }
}
