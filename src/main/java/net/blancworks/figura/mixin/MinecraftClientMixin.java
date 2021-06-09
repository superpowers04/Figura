package net.blancworks.figura.mixin;

import net.blancworks.figura.FiguraMod;
import net.blancworks.figura.PlayerDataManager;
import net.blancworks.figura.gui.EmoteWheel;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {

    @Shadow @Final public Mouse mouse;
    public boolean emoteWheelActive = false;

    @Inject(at = @At("INVOKE"), method = "disconnect(Lnet/minecraft/client/gui/screen/Screen;)V")
    public void disconnect(Screen screen, CallbackInfo ci) {
        try {
            PlayerDataManager.clearCache();
        } catch (Exception ignored) {}
    }

    @Inject(at = @At("RETURN"), method = "handleInputEvents")
    public void handleInputEvents(CallbackInfo ci) {
        if (FiguraMod.emoteWheel.isPressed()) {
            if (EmoteWheel.enabled) {
                this.mouse.unlockCursor();
                emoteWheelActive = true;
            }
        }
        else if (emoteWheelActive) {
            EmoteWheel.play();
            this.mouse.lockCursor();
            emoteWheelActive = false;
        }
    }

    @Inject(at = @At("HEAD"), method = "openScreen")
    public void openScreen(Screen screen, CallbackInfo ci) {
        EmoteWheel.play();
        emoteWheelActive = false;
    }
}
