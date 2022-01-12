package net.blancworks.figura.mixin;

import net.blancworks.figura.avatar.AvatarData;
import net.blancworks.figura.avatar.AvatarDataManager;
import net.blancworks.figura.trust.TrustContainer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameOverlayRenderer.class)
public class InGameOverlayRendererMixin {

    @Inject(at = @At("HEAD"), method = "renderFireOverlay", cancellable = true)
    private static void renderFireOverlay(MinecraftClient client, MatrixStack matrices, CallbackInfo ci) {
        AvatarData data = AvatarDataManager.localPlayer;
        if (AvatarDataManager.panic || data == null || data.script == null || data.script.shouldRenderFire == null || data.getTrustContainer().getTrust(TrustContainer.Trust.VANILLA_MODEL_EDIT) == 0)
            return;

        if (!data.script.shouldRenderFire)
            ci.cancel();
    }
}
