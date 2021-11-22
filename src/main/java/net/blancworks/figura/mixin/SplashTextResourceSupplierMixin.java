package net.blancworks.figura.mixin;

import net.blancworks.figura.FiguraMod;
import net.minecraft.client.resource.SplashTextResourceSupplier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Calendar;
import java.util.Date;

@Mixin(SplashTextResourceSupplier.class)
public class SplashTextResourceSupplierMixin {

    @Inject(at = @At("HEAD"), method = "get", cancellable = true)
    public void init(CallbackInfoReturnable<String> cir) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());

        if (FiguraMod.IS_CHEESE)
            cir.setReturnValue("LARGECHEESE!");
        else if (calendar.get(Calendar.MONTH) + 1 == 1 && calendar.get(Calendar.DAY_OF_MONTH) == 1) {
            cir.setReturnValue("Happy birthday Lily!");
        }
        else if (calendar.get(Calendar.MONTH) + 1 == 3 && calendar.get(Calendar.DAY_OF_MONTH) == 5) {
            cir.setReturnValue("Happy birthday devnull!");
        }
        else if (calendar.get(Calendar.MONTH) + 1 == 3 && calendar.get(Calendar.DAY_OF_MONTH) == 7) {
            cir.setReturnValue("Happy birthday Omoflop!");
        }
        else if (calendar.get(Calendar.MONTH) + 1 == 3 && calendar.get(Calendar.DAY_OF_MONTH) == 11) {
            cir.setReturnValue("Happy birthday Zandra!");
        }
        else if (calendar.get(Calendar.MONTH) + 1 == 9 && calendar.get(Calendar.DAY_OF_MONTH) == 21) {
            cir.setReturnValue("Happy birthday Fran!");
        }
    }
}
