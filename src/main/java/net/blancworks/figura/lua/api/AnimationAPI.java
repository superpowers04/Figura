package net.blancworks.figura.lua.api;

import net.blancworks.figura.lua.CustomScript;
import net.blancworks.figura.models.CustomModel;
import net.blancworks.figura.models.animations.Animation;
import net.blancworks.figura.models.animations.Animation.LoopMode;
import net.blancworks.figura.models.animations.Animation.PlayState;
import net.minecraft.util.Identifier;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;

public class AnimationAPI {
    public static Identifier getID() {
        return new Identifier("default", "animation");
    }

    public static ReadOnlyLuaTable getForScript(CustomScript script) {
        return new ReadOnlyLuaTable(new LuaTable() {{
            set("get", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    Animation anim = getAnimation(script, arg.checkjstring());
                    if (anim == null) return NIL;

                    return getTable(anim);
                }
            });

            set("listAnimations", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    CustomModel model = script.avatarData.model;
                    if (model == null) return NIL;

                    int i = 1;
                    LuaTable tbl = new LuaTable();
                    for (Animation animation : model.animations.values()) {
                        tbl.set(i, LuaValue.valueOf(animation.name));
                        i++;
                    }

                    return tbl;
                }
            });

            set("stopAll", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    CustomModel model = script.avatarData.model;
                    if (model == null) return NIL;

                    for (Animation animation : model.animations.values())
                        animation.stop();

                    return NIL;
                }
            });
        }});
    }

    public static ReadOnlyLuaTable getTable(Animation anim) {
        return new AnimationTable(anim).getTable();
    }

    private static Animation getAnimation(CustomScript script, String name) {
        CustomModel model = script.avatarData.model;
        if (model == null) return null;

        return model.animations.get(name);
    }

    private static class AnimationTable extends ReadOnlyLuaTable {
        private final Animation animation;

        private AnimationTable(Animation animation) {
            this.animation = animation;
        }

        public ReadOnlyLuaTable getTable() {
            return new ReadOnlyLuaTable(new LuaTable() {{
                set("play", new ZeroArgFunction() {
                    @Override
                    public LuaValue call() {
                        animation.play();
                        return NIL;
                    }
                });

                set("pause", new ZeroArgFunction() {
                    @Override
                    public LuaValue call() {
                        animation.playState = PlayState.PAUSED;
                        return NIL;
                    }
                });

                set("stop", new ZeroArgFunction() {
                    @Override
                    public LuaValue call() {
                        animation.stop();
                        return NIL;
                    }
                });

                set("isPlaying", new ZeroArgFunction() {
                    @Override
                    public LuaValue call() {
                        return LuaValue.valueOf(animation.playState == PlayState.PLAYING);
                    }
                });

                set("setLength", new OneArgFunction() {
                    @Override
                    public LuaValue call(LuaValue arg) {
                        animation.length = arg.checknumber().tofloat();
                        return NIL;
                    }
                });

                set("getLength", new ZeroArgFunction() {
                    @Override
                    public LuaValue call() {
                        return LuaValue.valueOf(animation.length);
                    }
                });

                set("setSpeed", new OneArgFunction() {
                    @Override
                    public LuaValue call(LuaValue arg) {
                        animation.speed = arg.checknumber().tofloat();
                        animation.inverted = animation.speed < 0f;
                        return NIL;
                    }
                });

                set("getSpeed", new ZeroArgFunction() {
                    @Override
                    public LuaValue call() {
                        return LuaValue.valueOf(animation.speed);
                    }
                });

                set("setLoopMode", new OneArgFunction() {
                    @Override
                    public LuaValue call(LuaValue arg) {
                        LoopMode mode;
                        try {
                            mode = LoopMode.valueOf(arg.checkjstring());
                        } catch (Exception ignored) {
                            mode = LoopMode.ONCE;
                        }

                        animation.loopMode = mode;
                        return NIL;
                    }
                });

                set("getLoopMode", new ZeroArgFunction() {
                    @Override
                    public LuaValue call() {
                        return LuaValue.valueOf(animation.loopMode.name());
                    }
                });

            }});
        }
    }
}
