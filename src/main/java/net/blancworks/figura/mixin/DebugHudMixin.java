package net.blancworks.figura.mixin;

import net.blancworks.figura.PlayerDataManager;
import net.blancworks.figura.lua.CustomScript;
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
        if (PlayerDataManager.localPlayer != null) {
            CustomScript script = PlayerDataManager.localPlayer.script;
            if (script != null) {
                cir.getReturnValue().add(4, String.format("[§bFIGURA§r] tick instructions: %d, render instructions: %d", script.tickInstructionCount, script.renderInstructionCount + script.worldRenderInstructionCount));
                cir.getReturnValue().add(5, String.format("[§bFIGURA§r] pings sent: %d, pings received: %d", script.pingSent, script.pingReceived));
            }
        }
    }
}