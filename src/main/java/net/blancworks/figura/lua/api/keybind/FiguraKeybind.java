package net.blancworks.figura.lua.api.keybind;

import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;

public final class FiguraKeybind {
    public final String name;
    public final int defaultKeycode;
    public int keycode;
    public int timesPressed;

    public FiguraKeybind(int keycode, String name) {
        this.name = name;
        this.defaultKeycode = keycode;
        this.keycode = keycode;
    }

    public Text getLocalizedText() {
        InputUtil.Key key = KeyBindAPI.getKey(KeyBindAPI.keys.inverse().get(keycode));
        return key.getLocalizedText();
    }

    public void resetToDefault() {
        keycode = defaultKeycode;
    }

    public boolean isDefault() {
        return keycode == defaultKeycode;
    }
}
