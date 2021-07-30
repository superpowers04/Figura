package net.blancworks.figura.lua.api.keybind;

import net.blancworks.figura.PlayerDataManager;
import net.blancworks.figura.lua.CustomScript;
import net.blancworks.figura.lua.api.ReadOnlyLuaTable;
import net.blancworks.figura.mixin.KeyBindingAccessorMixin;
import net.minecraft.client.options.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;

public class KeyBindAPI {

    public static Identifier getID() {
        return new Identifier("default", "keybind");
    }

    public static ReadOnlyLuaTable getForScript(CustomScript script) {
        return new ReadOnlyLuaTable(new LuaTable() {{

            set("newKey", new TwoArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1, LuaValue arg2) {

                    //remove old key with the same name
                    script.keyBindings.removeIf(binding -> binding.getTranslationKey().equals(arg1.checkjstring()));

                    //search for the binding
                    String keyString = arg2.checkjstring();
                    Integer key = keys.get(keyString);
                    if (key == null) {
                        throw new LuaError("Could not find key " + keyString);
                    }

                    //create keybind
                    KeyBinding keybind;

                    //only local can create valid keybinds
                    if (script.playerData != PlayerDataManager.localPlayer) {
                        keybind = new KeyBinding(
                                arg1.checkjstring(),
                                InputUtil.Type.KEYSYM,
                                keys.get("NONE"),
                                "figura$script_others"
                        );
                        //no need to update the keybinds here since we cant overwrite any used keybind
                    }
                    else {
                        keybind = new KeyBinding(
                                arg1.checkjstring(),
                                keyString.startsWith("MOUSE") ? InputUtil.Type.MOUSE : InputUtil.Type.KEYSYM,
                                key,
                                "figura$script"
                        );

                        //update all keybinds then add it to the script
                        KeyBinding.updateKeysByCode();
                        script.keyBindings.add(keybind);
                    }

                    return getTable(keybind);
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

    public static final Map<String, Integer> keys = new HashMap<String, Integer>() {{
        put("NONE", InputUtil.UNKNOWN_KEY.getCode());

        put("SPACE", GLFW.GLFW_KEY_SPACE);
        put("APOSTROPHE", GLFW.GLFW_KEY_APOSTROPHE);
        put("COMMA", GLFW.GLFW_KEY_COMMA);
        put("MINUS", GLFW.GLFW_KEY_MINUS);
        put("PERIOD", GLFW.GLFW_KEY_PERIOD);
        put("SLASH", GLFW.GLFW_KEY_SLASH);
        put("0", GLFW.GLFW_KEY_0);
        put("1", GLFW.GLFW_KEY_1);
        put("2", GLFW.GLFW_KEY_2);
        put("3", GLFW.GLFW_KEY_3);
        put("4", GLFW.GLFW_KEY_4);
        put("5", GLFW.GLFW_KEY_5);
        put("6", GLFW.GLFW_KEY_6);
        put("7", GLFW.GLFW_KEY_7);
        put("8", GLFW.GLFW_KEY_8);
        put("9", GLFW.GLFW_KEY_9);
        put("SEMICOLON", GLFW.GLFW_KEY_SEMICOLON);
        put("EQUAL", GLFW.GLFW_KEY_EQUAL);
        put("A", GLFW.GLFW_KEY_A);
        put("B", GLFW.GLFW_KEY_B);
        put("C", GLFW.GLFW_KEY_C);
        put("D", GLFW.GLFW_KEY_D);
        put("E", GLFW.GLFW_KEY_E);
        put("F", GLFW.GLFW_KEY_F);
        put("G", GLFW.GLFW_KEY_G);
        put("H", GLFW.GLFW_KEY_H);
        put("I", GLFW.GLFW_KEY_I);
        put("J", GLFW.GLFW_KEY_J);
        put("K", GLFW.GLFW_KEY_K);
        put("L", GLFW.GLFW_KEY_L);
        put("M", GLFW.GLFW_KEY_M);
        put("N", GLFW.GLFW_KEY_N);
        put("O", GLFW.GLFW_KEY_O);
        put("P", GLFW.GLFW_KEY_P);
        put("Q", GLFW.GLFW_KEY_Q);
        put("R", GLFW.GLFW_KEY_R);
        put("S", GLFW.GLFW_KEY_S);
        put("T", GLFW.GLFW_KEY_T);
        put("U", GLFW.GLFW_KEY_U);
        put("V", GLFW.GLFW_KEY_V);
        put("W", GLFW.GLFW_KEY_W);
        put("X", GLFW.GLFW_KEY_X);
        put("Y", GLFW.GLFW_KEY_Y);
        put("Z", GLFW.GLFW_KEY_Z);
        put("LEFT_BRACKET", GLFW.GLFW_KEY_LEFT_BRACKET);
        put("BACKSLASH", GLFW.GLFW_KEY_BACKSLASH);
        put("RIGHT_BRACKET", GLFW.GLFW_KEY_RIGHT_BRACKET);

        put("ESCAPE", GLFW.GLFW_KEY_ESCAPE);
        put("ENTER", GLFW.GLFW_KEY_ENTER);
        put("TAB", GLFW.GLFW_KEY_TAB);
        put("BACKSPACE", GLFW.GLFW_KEY_BACKSPACE);
        put("INSERT", GLFW.GLFW_KEY_INSERT);
        put("DELETE", GLFW.GLFW_KEY_DELETE);
        put("RIGHT", GLFW.GLFW_KEY_RIGHT);
        put("LEFT", GLFW.GLFW_KEY_LEFT);
        put("DOWN", GLFW.GLFW_KEY_DOWN);
        put("UP", GLFW.GLFW_KEY_UP);
        put("PAGE_UP", GLFW.GLFW_KEY_PAGE_UP);
        put("PAGE_DOWN", GLFW.GLFW_KEY_PAGE_DOWN);
        put("HOME", GLFW.GLFW_KEY_HOME);
        put("END", GLFW.GLFW_KEY_END);
        put("CAPS_LOCK", GLFW.GLFW_KEY_CAPS_LOCK);
        put("SCROLL_LOCK", GLFW.GLFW_KEY_SCROLL_LOCK);
        put("NUM_LOCK", GLFW.GLFW_KEY_NUM_LOCK);
        put("PAUSE", GLFW.GLFW_KEY_PAUSE);
        put("F1", GLFW.GLFW_KEY_F1);
        put("F2", GLFW.GLFW_KEY_F2);
        put("F3", GLFW.GLFW_KEY_F3);
        put("F4", GLFW.GLFW_KEY_F4);
        put("F5", GLFW.GLFW_KEY_F5);
        put("F6", GLFW.GLFW_KEY_F6);
        put("F7", GLFW.GLFW_KEY_F7);
        put("F8", GLFW.GLFW_KEY_F8);
        put("F9", GLFW.GLFW_KEY_F9);
        put("F10", GLFW.GLFW_KEY_F10);
        put("F11", GLFW.GLFW_KEY_F11);
        put("F12", GLFW.GLFW_KEY_F12);
        put("F13", GLFW.GLFW_KEY_F13);
        put("F14", GLFW.GLFW_KEY_F14);
        put("F15", GLFW.GLFW_KEY_F15);
        put("F16", GLFW.GLFW_KEY_F16);
        put("F17", GLFW.GLFW_KEY_F17);
        put("F18", GLFW.GLFW_KEY_F18);
        put("F19", GLFW.GLFW_KEY_F19);
        put("F20", GLFW.GLFW_KEY_F20);
        put("F21", GLFW.GLFW_KEY_F21);
        put("F22", GLFW.GLFW_KEY_F22);
        put("F23", GLFW.GLFW_KEY_F23);
        put("F24", GLFW.GLFW_KEY_F24);
        put("F25", GLFW.GLFW_KEY_F25);
        put("NP_0", GLFW.GLFW_KEY_KP_0);
        put("NP_1", GLFW.GLFW_KEY_KP_1);
        put("NP_2", GLFW.GLFW_KEY_KP_2);
        put("NP_3", GLFW.GLFW_KEY_KP_3);
        put("NP_4", GLFW.GLFW_KEY_KP_4);
        put("NP_5", GLFW.GLFW_KEY_KP_5);
        put("NP_6", GLFW.GLFW_KEY_KP_6);
        put("NP_7", GLFW.GLFW_KEY_KP_7);
        put("NP_8", GLFW.GLFW_KEY_KP_8);
        put("NP_9", GLFW.GLFW_KEY_KP_9);
        put("NP_DECIMAL", GLFW.GLFW_KEY_KP_DECIMAL);
        put("NP_DIVIDE", GLFW.GLFW_KEY_KP_DIVIDE);
        put("NP_MULTIPLY", GLFW.GLFW_KEY_KP_MULTIPLY);
        put("NP_SUBTRACT", GLFW.GLFW_KEY_KP_SUBTRACT);
        put("NP_ADD", GLFW.GLFW_KEY_KP_ADD);
        put("NP_ENTER", GLFW.GLFW_KEY_KP_ENTER);
        put("NP_EQUAL", GLFW.GLFW_KEY_KP_EQUAL);
        put("LEFT_SHIFT", GLFW.GLFW_KEY_LEFT_SHIFT);
        put("LEFT_CONTROL", GLFW.GLFW_KEY_LEFT_CONTROL);
        put("LEFT_ALT", GLFW.GLFW_KEY_LEFT_ALT);
        put("RIGHT_SHIFT", GLFW.GLFW_KEY_RIGHT_SHIFT);
        put("RIGHT_CONTROL", GLFW.GLFW_KEY_RIGHT_CONTROL);
        put("RIGHT_ALT", GLFW.GLFW_KEY_RIGHT_ALT);
        put("MENU", GLFW.GLFW_KEY_MENU);

        put("MOUSE_1", GLFW.GLFW_MOUSE_BUTTON_1);
        put("MOUSE_2", GLFW.GLFW_MOUSE_BUTTON_2);
        put("MOUSE_3", GLFW.GLFW_MOUSE_BUTTON_3);
        put("MOUSE_4", GLFW.GLFW_MOUSE_BUTTON_4);
        put("MOUSE_5", GLFW.GLFW_MOUSE_BUTTON_5);
        put("MOUSE_6", GLFW.GLFW_MOUSE_BUTTON_6);
        put("MOUSE_7", GLFW.GLFW_MOUSE_BUTTON_7);
        put("MOUSE_8", GLFW.GLFW_MOUSE_BUTTON_8);
    }};
}
