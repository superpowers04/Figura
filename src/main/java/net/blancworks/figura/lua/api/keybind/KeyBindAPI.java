package net.blancworks.figura.lua.api.keybind;

import com.google.common.collect.HashBiMap;
import net.blancworks.figura.PlayerDataManager;
import net.blancworks.figura.lua.CustomScript;
import net.blancworks.figura.lua.api.ReadOnlyLuaTable;
import net.blancworks.figura.lua.api.math.LuaVector;
import net.blancworks.figura.mixin.KeyBindingAccessorMixin;
import net.blancworks.figura.mixin.MouseMixin;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;
import java.util.HashSet;

public class KeyBindAPI {

    public static Identifier getID() {
        return new Identifier("default", "keybind");
    }

    public static ReadOnlyLuaTable getForScript(CustomScript script) {
        return new ReadOnlyLuaTable(new LuaTable() {{

            set("newKey", new TwoArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1, LuaValue arg2) {
                    String keyName = arg2.checkjstring();

                    //remove old key with the same name
                    script.keyBindings.removeIf(binding -> binding.name.equals(keyName));

                    //search for the binding
                    if (!keys.containsKey(keyName)) {
                        throw new LuaError("Could not find key " + keyName);
                    }

                    FiguraKeybind key = new FiguraKeybind(keys.get(keyName), arg1.checkjstring());
                    script.keyBindings.add(key);
                    return getTable(key);
                }
            });

            set("getRegisteredKeybind", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    if (script.playerData != PlayerDataManager.localPlayer)
                        return getTable(new KeyBinding("null", InputUtil.UNKNOWN_KEY.getCode(), "script_others"));

                    KeyBinding key = KeyBindingAccessorMixin.getKeysById().get(arg.checkjstring());
                    return key == null ? NIL : getTable(key);
                }
            });

        }});
    }

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

            set("setKey", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    String keyString = arg.checkjstring();
                    Integer key = keys.get(keyString);
                    if (key == null) {
                        throw new LuaError("Could not find key " + keyString);
                    }

                    if (keybind.getCategory().equals("figura$script_others"))
                        return NIL;

                    keybind.setBoundKey(keyString.startsWith("MOUSE") ? InputUtil.Type.MOUSE.createFromCode(key) : InputUtil.Type.KEYSYM.createFromCode(key));
                    KeyBinding.updateKeysByCode();
                    return NIL;
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
    public static ReadOnlyLuaTable getTable(final FiguraKeybind keybind) {

        return new ReadOnlyLuaTable(new LuaTable() {{

            set("isPressed", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    keybind.timesPressed++;
                    return LuaValue.valueOf(isKeyPressed(keybind.keycode));
                }
            });

            set("wasPressed", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    boolean b = keybind.timesPressed > 0;
                    if (b) keybind.timesPressed--;
                    return LuaValue.valueOf(b);
                }
            });

            set("getKey", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(keys.inverse().get(keybind.keycode));
                }
            });

            set("setKey", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    String keyString = arg.checkjstring();
                    if (!keys.containsKey(keyString)) {
                        throw new LuaError("Could not find key " + keyString);
                    }

                    keybind.keycode = keys.get(keyString);
                    KeyBinding.updateKeysByCode();
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

    public static final HashBiMap<String, Integer> keys = HashBiMap.create();
    static {
        for(Field f : GLFW.class.getFields()) {
            String name = f.getName();
            if (name.startsWith("GLFW_KEY_") || name.startsWith("GLFW_MOUSE_")) {
                try {
                    String keyName = f.getName().replace("GLFW_KEY_", "").replace("GLFW_MOUSE_", "MOUSE_");
                    if (!keys.containsValue(f.getInt(null))) {
                        keys.put(keyName, f.getInt(null));

                    }
                } catch (IllegalAccessException ignored) {}
            }
        }
    }

    public static final HashSet<Integer> heldKeycodes = new HashSet<>();
    public static final HashSet<Integer> heldMouseButtons = new HashSet<>();

    public static InputUtil.Key getKey(String name) {
        if (!keys.containsKey(name)) return InputUtil.UNKNOWN_KEY;
        return (name.startsWith("MOUSE_") ? InputUtil.Type.MOUSE : InputUtil.Type.KEYSYM).createFromCode(keys.get(name));
    }

    public static boolean isKeyPressed(int keycode) {
        if (keycode == -1) return false;

        boolean pressed;
        String keyId = keys.inverse().get(keycode);
        if (keyId.startsWith("MOUSE_")) {
            pressed = heldMouseButtons.contains(keycode);
        } else {
            pressed = InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(), keycode);
        }

        if (pressed && !heldKeycodes.contains(keycode)) {
            heldKeycodes.add(keycode);
        } else heldKeycodes.remove(keycode);

        return pressed;
    }
}
