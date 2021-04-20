package net.blancworks.figura.mixin;

import net.blancworks.figura.Config;
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

        int x = 5;
        int y = 5;

        switch (Config.buttonLocation.value) {
            case 0: //top left
                if (FabricLoader.getInstance().isModLoaded("modmenu"))
                    y += 12;
                break;
            case 1: //top right
                x = this.width - 64 - 5;
                if (FabricLoader.getInstance().isModLoaded("modmenu"))
                    y += 12;
                break;
            case 2: //bottom left
                y = this.height - 20 - 5;
                if (FabricLoader.getInstance().isModLoaded("modmenu"))
                    y -= 12;
                break;
            case 3: //bottom right
                x = this.width - 64 - 5;
                y = this.height - 20 - 5;
                if (FabricLoader.getInstance().isModLoaded("modmenu"))
                    y -= 12;
                break;
        }

        addButton(new ButtonWidget(x, y, 64, 20, new LiteralText("Figura"),
                btn -> this.client.openScreen(figura$screen)));
    }
}
