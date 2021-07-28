package net.blancworks.figura.mixin;

import net.blancworks.figura.FiguraMod;
import net.minecraft.client.resource.SplashTextResourceSupplier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SplashTextResourceSupplier.class)
public class SplashTextResourceSupplierMixin {

    @Inject(at = @At("HEAD"), method = "get", cancellable = true)
    public void init(CallbackInfoReturnable<String> cir) {
        if (FiguraMod.IS_CHEESE)
            cir.setReturnValue("LARGECHEESE!");
    }
}
