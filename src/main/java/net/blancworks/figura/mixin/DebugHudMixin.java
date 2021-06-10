package net.blancworks.figura.mixin;

import net.blancworks.figura.PlayerDataManager;
import net.minecraft.client.gui.hud.DebugHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(DebugHud.class)
public class DebugHudMixin {

    @Inject(at = @At("RETURN"), method = "getRightText()Ljava/util/List;")
    protected void getRightText(CallbackInfoReturnable<List<String>> cir) {
        if (PlayerDataManager.localPlayer != null && PlayerDataManager.localPlayer.script != null)
            cir.getReturnValue().add(4, String.format("[FIGURA] tick instructions : %d render instructions : %d", PlayerDataManager.localPlayer.script.tickInstructionCount, PlayerDataManager.localPlayer.script.renderInstructionCount));
    }
}