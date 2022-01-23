package net.blancworks.figura.gui;

import net.blancworks.figura.gui.widgets.FiguraSoundWidget;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.TranslatableText;

public class FiguraSoundScreen extends Screen {

    public Screen parentScreen;
    private FiguraSoundWidget soundWidget;

    public FiguraSoundScreen(Screen parentScreen) {
        super(new TranslatableText("figura.gui.sounds.title"));
        this.parentScreen = parentScreen;
    }

    @Override
    protected void init() {
        super.init();

        this.addDrawableChild(new ButtonWidget(this.width / 2 - 75, this.height - 29, 150, 20, new TranslatableText("gui.back"), (buttonWidgetx) -> this.client.setScreen(parentScreen)));

        this.soundWidget = new FiguraSoundWidget(this, this.client);
        this.addSelectableChild(this.soundWidget);
    }

    @Override
    public void onClose() {
        this.client.setScreen(parentScreen);
    }

    @Override
    public void tick() {
        super.tick();
        this.soundWidget.tick();
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        //background
        this.renderBackgroundTexture(0);

        //list
        this.soundWidget.render(matrices, mouseX, mouseY, delta);

        //buttons
        super.render(matrices, mouseX, mouseY, delta);

        //screen title
        drawCenteredText(matrices, this.textRenderer, this.title, this.width / 2, 12, 0xFFFFFF);
    }
}
