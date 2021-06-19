package net.blancworks.figura.gui;

import net.blancworks.figura.Config;
import net.blancworks.figura.gui.widgets.ConfigListWidget;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.TranslatableText;

public class FiguraConfigScreen extends Screen {

    public Screen parentScreen;
    private ConfigListWidget configListWidget;

    public FiguraConfigScreen(Screen parentScreen) {
        super(new TranslatableText("gui.figura.configtitle"));
        this.parentScreen = parentScreen;
    }

    @Override
    protected void init() {
        super.init();

        this.addDrawableChild(new ButtonWidget(this.width / 2 - 154, this.height - 29, 150, 20, new TranslatableText("gui.cancel"), (buttonWidgetx) -> {
            Config.discardConfig();
            this.client.openScreen(parentScreen);
        }));

        this.addDrawableChild(new ButtonWidget(this.width / 2 + 4, this.height - 29, 150, 20, new TranslatableText("gui.done"), (buttonWidgetx) -> {
            Config.copyConfig();
            Config.saveConfig();
            this.client.openScreen(parentScreen);
        }));

        this.configListWidget = new ConfigListWidget(this, this.client);
        this.addSelectableChild(this.configListWidget);
    }

    @Override
    public void onClose() {
        this.client.openScreen(parentScreen);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        //background
        this.renderBackgroundTexture(0);

        //list
        this.configListWidget.render(matrices, mouseX, mouseY, delta);

        //buttons
        super.render(matrices, mouseX, mouseY, delta);

        //screen title
        drawCenteredText(matrices, this.textRenderer, this.title, this.width / 2, 12, 16777215);
    }
}