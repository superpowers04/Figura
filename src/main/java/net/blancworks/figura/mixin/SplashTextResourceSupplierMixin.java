package net.blancworks.figura.mixin;

import net.blancworks.figura.FiguraMod;
import net.blancworks.figura.config.ConfigManager.Config;
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
        if (!(boolean) Config.EASTER_EGGS.value)
            return;

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());

        if (FiguraMod.IS_CHEESE) {
            cir.setReturnValue("LARGECHEESE!");
        } else { //b-days!!
            int month = calendar.get(Calendar.MONTH) + 1;
            int day = calendar.get(Calendar.DAY_OF_MONTH);
            String bday = "Happy birthday ";

            switch (month) {
                case 1 -> {
                    if (day == 1) cir.setReturnValue(bday + "Lily!");
                }
                case 3 -> {
                    if (day > 20 && day < 26) {
                        int diff = 26 - day;
                        cir.setReturnValue(diff + " day" + (diff > 1 ? "s!" : "!"));
                    } else {
                        switch (day) {
                            case 5 -> cir.setReturnValue(bday + "devnull!");
                            case 7 -> cir.setReturnValue(bday + "omoflop!");
                            case 11 -> cir.setReturnValue(bday + "Zandra!");
                            case 26 -> cir.setReturnValue(bday + "Figura!");
                        }
                    }
                }
                case 9 -> {
                    if (day == 21) cir.setReturnValue(bday + "Fran!");
                }
            }
        }
    }
}
