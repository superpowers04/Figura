package net.blancworks.figura.mixin;

import net.blancworks.figura.FiguraMod;
import net.blancworks.figura.PlayerData;
import net.blancworks.figura.PlayerDataManager;
import net.blancworks.figura.gui.ActionWheel;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class InGameHudMixin {

    private ActionWheel actionWheel;

    @Inject(at = @At ("RETURN"), method = "<init>")
    public void init(MinecraftClient client, CallbackInfo ci) {
        actionWheel = new ActionWheel(client);
    }

    @Inject(at = @At ("RETURN"), method = "render")
    public void render(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
        if (FiguraMod.actionWheel.isPressed()) {
            if (ActionWheel.enabled)
                actionWheel.render(matrices);
        }
        else {
            ActionWheel.enabled = true;
        }
    }

    @Inject(at = @At ("HEAD"), method = "renderCrosshair", cancellable = true)
    private void renderCrosshair(MatrixStack matrices, CallbackInfo ci) {
        if (FiguraMod.actionWheel.isPressed() && ActionWheel.enabled)
            ci.cancel();
    }

    @Redirect(
            method = "renderCrosshair",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/hud/InGameHud;drawTexture(Lnet/minecraft/client/util/math/MatrixStack;IIIIII)V"
            ),
            slice = @Slice(
                    from = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;blendFuncSeparate(Lcom/mojang/blaze3d/platform/GlStateManager$SrcFactor;Lcom/mojang/blaze3d/platform/GlStateManager$DstFactor;Lcom/mojang/blaze3d/platform/GlStateManager$SrcFactor;Lcom/mojang/blaze3d/platform/GlStateManager$DstFactor;)V"),
                    to = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getAttackCooldownProgress(F)F")
            )
    )
    private void renderCrosshair(InGameHud inGameHud, MatrixStack matrices, int x, int y, int u, int v, int width, int height) {
        PlayerData currentData = PlayerDataManager.localPlayer;
        if (currentData != null && currentData.script != null) {

            //doesnt render crosshair
            if (!currentData.script.crossHairEnabled) return;

            //crosshair offset
            if (currentData.script.crossHairPos != null) {
                x += currentData.script.crossHairPos.x;
                y += currentData.script.crossHairPos.y;
            }
        }

        //render crosshair
        inGameHud.drawTexture(matrices, x, y, u, v, width, height);
    }
}
