package net.blancworks.figura.mixin;

import net.blancworks.figura.avatar.AvatarData;
import net.blancworks.figura.avatar.AvatarDataManager;
import net.blancworks.figura.gui.ActionWheel;
import net.blancworks.figura.gui.NewActionWheel;
import net.blancworks.figura.gui.PlayerPopup;
import net.blancworks.figura.lua.CustomScript;
import net.blancworks.figura.lua.api.keybind.FiguraKeybind;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.util.InputUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class MouseMixin {

    @Inject(method = "onMouseButton", at = @At("HEAD"), cancellable = true)
    private void onMouseButton(long window, int button, int action, int mods, CallbackInfo ci) {
        if (window == MinecraftClient.getInstance().getWindow().getHandle()) {
            boolean pressed = action == 1;
            FiguraKeybind.setKeyPressed(InputUtil.Type.MOUSE.createFromCode(button), pressed);

            if (pressed) {
                FiguraKeybind.onKeyPressed(InputUtil.Type.MOUSE.createFromCode(button));

                AvatarData data = AvatarDataManager.localPlayer;
                if (ActionWheel.enabled || NewActionWheel.enabled || (data != null && data.script != null && data.script.unlockCursor)) {
                    if (button == 0) {
                        ActionWheel.play();
                        NewActionWheel.play();
                    }

                    ci.cancel();
                }
            }
        }
    }

    @Inject(method = "onMouseScroll", at = @At("HEAD"), cancellable = true)
    private void onMouseScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (window == MinecraftClient.getInstance().getWindow().getHandle()) {
            CustomScript.mouseScroll = vertical;
            double amount = Math.signum(vertical);
            if (NewActionWheel.mouseScrolled(amount) || PlayerPopup.mouseScrolled(amount))
                ci.cancel();
        }
    }

    @Inject(method = "lockCursor", at = @At("HEAD"), cancellable = true)
    private void lockCursor(CallbackInfo ci) {
        AvatarData data = AvatarDataManager.localPlayer;
        if (ActionWheel.enabled || NewActionWheel.enabled || (data != null && data.script != null && data.script.unlockCursor))
            ci.cancel();
    }
}
