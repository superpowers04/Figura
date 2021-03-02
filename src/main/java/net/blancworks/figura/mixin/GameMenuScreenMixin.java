package net.blancworks.figura.mixin;

import net.blancworks.figura.gui.FiguraGuiScreen;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameMenuScreen.class)
public class GameMenuScreenMixin extends Screen {

    private FiguraGuiScreen figuraScreen;
    
    protected GameMenuScreenMixin(Text title) {
        super(title);
    }

    @Inject(at = @At("RETURN"), method = "init()V")
    void init(CallbackInfo ci){
        
        if(figuraScreen == null)
            figuraScreen = new FiguraGuiScreen(this);
        
        addButton(new ButtonWidget(this.width - 64 - 5, this.height - 20 - 5, 64, 20, new LiteralText("Figura"), (buttonWidgetx) -> {
            this.client.openScreen(figuraScreen);
        }));
        
        
    }
    
}
