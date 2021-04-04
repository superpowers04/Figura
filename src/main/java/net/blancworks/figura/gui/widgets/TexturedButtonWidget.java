package net.blancworks.figura.gui.widgets;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class TexturedButtonWidget extends ButtonWidget {
    private Identifier texture;
    private int u;
    private int v;
    private int hoveredVOffset;
    private int textureWidth;
    private int textureHeight;

    public TexturedButtonWidget(int x, int y, int width, int height, int u, int v, int hoveredVOffset, Identifier texture, PressAction pressAction) {
        this(x, y, width, height, u, v, hoveredVOffset, texture, 256, 256, pressAction);
    }

    public TexturedButtonWidget(int x, int y, int width, int height, int u, int v, int hoveredVOffset, Identifier texture, int textureWidth, int textureHeight, PressAction pressAction) {
        this(x, y, width, height, u, v, hoveredVOffset, texture, textureWidth, textureHeight, pressAction, LiteralText.EMPTY);
    }

    public TexturedButtonWidget(int i, int j, int k, int l, int m, int n, int o, Identifier identifier, int p, int q, PressAction pressAction, Text text) {
        this(i, j, k, l, m, n, o, identifier, p, q, pressAction, EMPTY, text);
    }

    public TexturedButtonWidget(int x, int y, int width, int height, int u, int v, int hoveredVOffset, Identifier texture, int textureWidth, int textureHeight, PressAction pressAction, TooltipSupplier tooltipSupplier, Text text) {
        super(x, y, width, height, text, pressAction, tooltipSupplier);
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
        this.u = u;
        this.v = v;
        this.hoveredVOffset = hoveredVOffset;
        this.texture = texture;
    }

    public void setPos(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void renderButton(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        minecraftClient.getTextureManager().bindTexture(this.texture);
        int i = this.u;
        int j = this.v;
        if (this.isHovered()) {
            j += this.hoveredVOffset;
        }
        if (this.active) {
            i += this.hoveredVOffset;
        }

        RenderSystem.enableDepthTest();
        drawTexture(matrices, this.x, this.y, (float)i, (float)j, this.width, this.height, this.textureWidth, this.textureHeight);
        if (this.isHovered()) {
            this.renderToolTip(matrices, mouseX, mouseY);
        }

    }
}
