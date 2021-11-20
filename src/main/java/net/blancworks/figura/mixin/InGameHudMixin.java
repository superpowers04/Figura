package net.blancworks.figura.mixin;

import net.blancworks.figura.FiguraMod;
import net.blancworks.figura.PlayerData;
import net.blancworks.figura.PlayerDataManager;
import net.blancworks.figura.gui.ActionWheel;
import net.blancworks.figura.gui.PlayerPopup;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {

    @Inject(at = @At ("HEAD"), method = "render")
    public void preRender(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
        if (FiguraMod.PLAYER_POPUP_BUTTON.isPressed())
            PlayerPopup.render(matrices);
    }

    @Inject(at = @At ("RETURN"), method = "render")
    public void postRender(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
        if (FiguraMod.ACTION_WHEEL_BUTTON.isPressed())
            ActionWheel.render(matrices);
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/KeyBinding;isPressed()Z"))
    private boolean onPlayerListRender(KeyBinding keyPlayerList) {
        return keyPlayerList.isPressed() || PlayerPopup.miniEnabled;
    }

    @Inject(at = @At ("HEAD"), method = "renderCrosshair", cancellable = true)
    private void renderCrosshair(MatrixStack matrices, CallbackInfo ci) {
        if (FiguraMod.ACTION_WHEEL_BUTTON.isPressed() && ActionWheel.enabled)
            ci.cancel();

        //do not render crosshair
        PlayerData currentData = PlayerDataManager.localPlayer;
        if (currentData != null && currentData.script != null && !currentData.script.crossHairEnabled)
            ci.cancel();
    }

    @ModifyArgs(
            method = "renderCrosshair",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/hud/InGameHud;drawTexture(Lnet/minecraft/client/util/math/MatrixStack;IIIIII)V",
                    ordinal = 0
            )
    )
    private void renderCrosshairDrawTexture(Args args) {
        //set crosshair offset
        PlayerData currentData = PlayerDataManager.localPlayer;
        if (currentData != null && currentData.script != null && currentData.script.crossHairPos != null) {
            args.set(1, (int) ((int) args.get(1) + currentData.script.crossHairPos.x));
            args.set(2, (int) ((int) args.get(2) + currentData.script.crossHairPos.y));
        }
    }
}
