package net.blancworks.figura.lua.api.keybind;

import com.google.common.collect.HashBiMap;
import net.blancworks.figura.avatar.AvatarData;
import net.blancworks.figura.avatar.AvatarDataManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;

public final class FiguraKeybind {

    public static final HashBiMap<String, Integer> KEYS = HashBiMap.create();
    static {
        for(Field f : GLFW.class.getFields()) {
            String name = f.getName();
            if (name.startsWith("GLFW_KEY_") || name.startsWith("GLFW_MOUSE_")) {
                try {
                    String keyName = f.getName().replace("GLFW_KEY_", "").replace("GLFW_MOUSE_", "MOUSE_");
                    if (!KEYS.containsValue(f.getInt(null))) {
                        KEYS.put(keyName, f.getInt(null));

                    }
                } catch (IllegalAccessException ignored) {}
            }
        }
    }

    public final String name;
    public final int defaultKeycode;
    public final boolean ignoreScreen;

    public int keycode;
    public int timesPressed = 0;
    public boolean pressed = false;

    public FiguraKeybind(int keycode, String name, boolean ignoreScreen) {
        this.name = name;
        this.defaultKeycode = keycode;
        this.keycode = keycode;
        this.ignoreScreen = ignoreScreen;
    }

    public static void setKeyPressed(InputUtil.Key key, boolean pressed) {
        FiguraKeybind keyBinding = getKeybind(key);
        if (keyBinding != null)
            keyBinding.setPressed(pressed);
    }

    public static void onKeyPressed(InputUtil.Key key) {
        FiguraKeybind keyBinding = getKeybind(key);
        if (keyBinding != null)
            ++keyBinding.timesPressed;
    }

    public static void unpressAll() {
        AvatarData data = AvatarDataManager.localPlayer;

        if (data == null || data.script == null)
            return;

        data.script.keyBindings.forEach(keyBind -> {
            if (!keyBind.ignoreScreen) keyBind.reset();
        });
    }

    public static InputUtil.Key getKey(String name) {
        Integer key = KEYS.get(name);
        if (key == null) return InputUtil.UNKNOWN_KEY;
        return (name.startsWith("MOUSE_") ? InputUtil.Type.MOUSE : InputUtil.Type.KEYSYM).createFromCode(key);
    }

    public static FiguraKeybind getKeybind(InputUtil.Key key) {
        //only gets from local player, since we dont want to process other ppl keybinds
        AvatarData data = AvatarDataManager.localPlayer;

        if (data == null || data.script == null)
            return null;

        for (FiguraKeybind keybind : data.script.keyBindings) {
            if (keybind.keycode == key.getCode()) return keybind;
        }

        return null;
    }

    public Text getLocalizedText() {
        InputUtil.Key key = getKey(KEYS.inverse().get(keycode));
        return key.getLocalizedText();
    }

    public void resetToDefault() {
        keycode = defaultKeycode;
    }

    public boolean isDefault() {
        return keycode == defaultKeycode;
    }

    public boolean isKeyPressed() {
        if (ignoreScreen || MinecraftClient.getInstance().currentScreen == null)
            return pressed;
        else
            return false;
    }

    public boolean wasPressed() {
        if (this.timesPressed == 0) {
            return false;
        } else if (ignoreScreen || MinecraftClient.getInstance().currentScreen == null) {
            --this.timesPressed;
            return true;
        } else {
            return false;
        }
    }

    public void setPressed(boolean pressed) {
        this.pressed = pressed;
    }

    public void reset() {
        this.timesPressed = 0;
        this.setPressed(false);
    }
}
