package net.blancworks.figura.mixin;

import net.blancworks.figura.FiguraMod;
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

    @Inject(at = @At("RETURN"), method = "getRightText")
    protected void getRightText(CallbackInfoReturnable<List<String>> cir) {
        List<String> lines = cir.getReturnValue();

        lines.add(3, "");
        lines.add(4, "§b[FIGURA]§r");
        lines.add(5, "Version: " + FiguraMod.MOD_VERSION);

        if (PlayerDataManager.localPlayer != null) {
            CustomScript script = PlayerDataManager.localPlayer.script;
            if (script != null) {
                lines.add(6, String.format("Init instructions: %d", script.initInstructionCount));
                lines.add(7, String.format("Tick instructions: %d", script.tickInstructionCount + script.damageInstructionCount));
                lines.add(8, String.format("Render instructions: %d",script.renderInstructionCount + script.worldRenderInstructionCount + script.renderLayerInstructionCount));
                lines.add(9, String.format("Pings Sent: %d, Received: %d", script.pingSent, script.pingReceived));
            }
        }
    }
}