package net.blancworks.figura.mixin;

import net.blancworks.figura.lua.api.keybind.FiguraKeybind;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Keyboard.class)
public class KeyboardMixin {

    @Shadow @Final private MinecraftClient client;

    @Inject(method = "onKey", at = @At("HEAD"))
    public void onKey(long window, int key, int scancode, int action, int modifiers, CallbackInfo ci) {
        if (window == this.client.getWindow().getHandle()) {
            InputUtil.Key key2 = InputUtil.fromKeyCode(key, scancode);

            if (action == 0) {
                FiguraKeybind.setKeyPressed(key2, false);
            } else {
                FiguraKeybind.setKeyPressed(key2, true);
                FiguraKeybind.onKeyPressed(key2);
            }
        }
    }
}
