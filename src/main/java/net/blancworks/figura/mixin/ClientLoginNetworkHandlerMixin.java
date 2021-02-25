package net.blancworks.figura.mixin;

import net.blancworks.figura.network.FiguraNetworkManager;
import net.minecraft.client.network.ClientLoginNetworkHandler;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLoginNetworkHandler.class)
public class ClientLoginNetworkHandlerMixin {


    @Inject(at = @At("HEAD"), method = "onDisconnected(Lnet/minecraft/text/Text;)V")
    public void onDisconnected(Text reason, CallbackInfo ci) {
        
        try{
            if(reason.asString().equals("This is the Figura Auth Server!\n")){

                Text keyText = reason.getSiblings().get(1);
                FiguraNetworkManager.figuraSessionKey = Integer.parseInt(keyText.asString());

                LiteralText garbleText = new LiteralText("-------------------------\n\n\n");
                garbleText.setStyle(Style.EMPTY.withFormatting(Formatting.OBFUSCATED));
                
                reason.getSiblings().set(1, garbleText);
                
                System.out.println(String.format("FIGURA AUTH CODE:%d", FiguraNetworkManager.figuraSessionKey));
            }
        }
        catch (Exception e){
            System.out.println(e.toString());
        }
    }
}
