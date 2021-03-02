package net.blancworks.figura.gui;

import net.blancworks.figura.network.FiguraNetworkManager;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;

public class FiguraGuiScreen extends Screen {

    public Screen parentScreen;

    public Identifier connectedTexture = new Identifier("figura", "gui/menu/connected.png");
    public Identifier disconnectedTexture = new Identifier("figura", "gui/menu/disconnected.png");
    public TexturedButtonWidget connectionStatusButton;
    public TexturedButtonWidget disconnectedStatusButton;


    public FiguraGuiScreen(Screen parentScreen) {
        super(new LiteralText("Figura Menu"));
        this.parentScreen = parentScreen;
    }

    @Override
    protected void init() {
        super.init();

        this.addButton(new ButtonWidget(5, this.height / 4 + 10, 64, 20, new LiteralText("Back"), (buttonWidgetx) -> {
            this.client.openScreen((Screen) parentScreen);
            //this.client.mouse.lockCursor();
        }));

        connectionStatusButton = new TexturedButtonWidget(
                this.width - 32 - 5, 5,
                32, 32,
                0, 0, 32,
                connectedTexture, 32, 64,
                (bx) -> {
                    
                }
        );
        this.addButton(connectionStatusButton);
        connectionStatusButton.active = false;

        disconnectedStatusButton = new TexturedButtonWidget(
                this.width - 32 - 5, 5,
                32, 32,
                0, 0, 32,
                disconnectedTexture, 32, 64,
                (bx) -> {
                    FiguraNetworkManager.authUser();
                }
        );
        this.addButton(disconnectedStatusButton);
        disconnectedStatusButton.active = false;

    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);

        if(FiguraNetworkManager.hasAuthKey()){
            connectionStatusButton.active = true;
            connectionStatusButton.visible = true;
            disconnectedStatusButton.active = false;
            disconnectedStatusButton.visible = false;
        } else {
            disconnectedStatusButton.active = true;
            disconnectedStatusButton.visible = true;
            connectionStatusButton.active = false;
            connectionStatusButton.visible = false;
        }
        
        super.render(matrices, mouseX, mouseY, delta);
    }


}
