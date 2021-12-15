package net.blancworks.figura.lua.api;

import net.blancworks.figura.lua.CustomScript;
import net.blancworks.figura.models.CustomModel;
import net.blancworks.figura.models.CustomModelPartGroup;
import net.blancworks.figura.models.animations.Animation;
import net.minecraft.util.Identifier;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;

public class animation {
    public static Identifier getID() {
        return new Identifier("default", "animation");
    }

    public static ReadOnlyLuaTable getForScript(CustomScript script) {
        return new ReadOnlyLuaTable(new LuaTable() {{
            set("play", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    checkForAnimation(script, arg).playState = Animation.PlayState.playing;
                    return NIL;
                }
            });

            set("pause", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    checkForAnimation(script, arg).playState = Animation.PlayState.paused;
                    return NIL;
                }
            });

            set("stop", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    Animation anim = checkForAnimation(script, arg);
                    anim.playState = Animation.PlayState.stopped;
                    anim.tick = 0;
                    return NIL;
                }
            });
        }});
    }

    private static Animation checkForAnimation(CustomScript script, LuaValue arg) {
        CustomModel model = script.avatarData.model;
        if (model == null)
            throw new LuaError("Avatar has no model!");

        if (!model.animations.containsKey(arg.checkjstring()))
            throw new LuaError("Animation not found!");

        return model.animations.get(arg.checkjstring());
    }
}
