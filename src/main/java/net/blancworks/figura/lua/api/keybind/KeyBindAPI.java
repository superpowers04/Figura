package net.blancworks.figura.lua.api.keybind;

import net.blancworks.figura.lua.CustomScript;
import net.blancworks.figura.lua.api.ReadOnlyLuaTable;
import net.blancworks.figura.mixin.KeyBindingAccessorMixin;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.util.Identifier;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.ThreeArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;

public class KeyBindAPI {

    public static Identifier getID() {
        return new Identifier("default", "keybind");
    }

    public static ReadOnlyLuaTable getForScript(CustomScript script) {
        return new ReadOnlyLuaTable(new LuaTable() {{

            set("newKey", new ThreeArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1, LuaValue arg2, LuaValue arg3) {
                    String keyName = arg2.checkjstring();

                    //remove old key with the same name
                    script.keyBindings.removeIf(binding -> binding.name.equals(keyName));

                    //search for the binding
                    Integer keycode = FiguraKeybind.KEYS.get(keyName);
                    if (keycode == null)
                        throw new LuaError("Could not find key " + keyName);

                    //set key
                    FiguraKeybind key;
                    boolean ignoreScreen = !arg3.isnil() && arg3.checkboolean();

                    key = new FiguraKeybind(keycode, arg1.checkjstring(), ignoreScreen);

                    script.keyBindings.add(key);
                    return getTable(key);
                }
            });

            set("getRegisteredKeybind", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    KeyBinding key = KeyBindingAccessorMixin.getKeysById().get(arg.checkjstring());
                    return key == null ? NIL : getTable(key);
                }
            });

            set("getKeyList", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    LuaTable list = new LuaTable();

                    int i = 1;
                    for (String entry : FiguraKeybind.KEYS.keySet()) {
                        list.insert(i, LuaValue.valueOf(entry));
                        i++;
                    }

                    return list;
                }
            });

            set("getRegisteredKeyList", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    LuaTable list = new LuaTable();

                    int i = 1;
                    for (String entry : KeyBindingAccessorMixin.getKeysById().keySet()) {
                        list.insert(i, LuaValue.valueOf(entry));
                        i++;
                    }

                    return list;
                }
            });

        }});
    }

    //vanilla keybinds
    public static ReadOnlyLuaTable getTable(KeyBinding keybind) {
        return new ReadOnlyLuaTable(new LuaTable() {{

            set("isPressed", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(keybind.isPressed());
                }
            });

            set("wasPressed", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(keybind.wasPressed());
                }
            });

            set("getKey", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(keybind.getBoundKeyLocalizedText().getString());
                }
            });

            set("getName", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(keybind.getTranslationKey());
                }
            });

        }});
    }

    //custom keybinds
    public static ReadOnlyLuaTable getTable(FiguraKeybind keybind) {
        return new ReadOnlyLuaTable(new LuaTable() {{

            set("isPressed", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(keybind.isKeyPressed());
                }
            });

            set("wasPressed", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(keybind.wasPressed());
                }
            });

            set("reset", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    keybind.reset();
                    return NIL;
                }
            });

            set("getKey", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(FiguraKeybind.KEYS.inverse().get(keybind.keycode));
                }
            });

            set("setKey", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    String keyString = arg.checkjstring();
                    Integer keycode = FiguraKeybind.KEYS.get(keyString);

                    if (keycode == null)
                        throw new LuaError("Could not find key " + keyString);

                    keybind.keycode = keycode;
                    return NIL;
                }
            });

            set("getName", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(keybind.name);
                }
            });

        }});
    }
}
