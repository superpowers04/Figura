package net.blancworks.figura.mixin;

import net.blancworks.figura.gui.FiguraGuiScreen;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameMenuScreen.class)
public class GameMenuScreenMixin extends Screen {

    private FiguraGuiScreen figura$screen;

    protected GameMenuScreenMixin(Text title) {
        super(title);
    }

    @Inject(at = @At("RETURN"), method = "init()V")
    void init(CallbackInfo ci) {
        if (this.figura$screen == null)
            this.figura$screen = new FiguraGuiScreen(this);

        int y = this.height - 20 - 5;
        if (FabricLoader.getInstance().isModLoaded("modmenu"))
            y -= 12;

        addButton(new ButtonWidget(this.width - 64 - 5, y, 64, 20, new LiteralText("Figura"),
                btn -> this.client.openScreen(figura$screen)));
    }
}
