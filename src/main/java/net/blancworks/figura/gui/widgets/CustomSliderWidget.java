package net.blancworks.figura.gui.widgets;

import com.mojang.blaze3d.systems.RenderSystem;
import net.blancworks.figura.config.ConfigManager;
import net.blancworks.figura.utils.TextUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

public abstract class CustomSliderWidget extends SliderWidget {
    public static final Identifier SLIDER_TEXTURE = new Identifier("figura", "textures/gui/slider.png");

    public CustomSliderWidget(int x, int y, int width, int height, Text text, double value) {
        super(x, y, width, height, text, value);
    }

    @Override
    public void renderButton(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        super.renderButton(matrices, mouseX, mouseY, delta);
        matrices.push();
        matrices.translate(0f, 0f, 0.1f);

        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        TextUtils.renderOutlineText(textRenderer, this.getMessage(), this.x + this.width / 2f - textRenderer.getWidth(this.getMessage()) / 2f, this.y + (this.height - 8) / 2f, this.active ? 0xFFFFFF : 0xA0A0A0 | MathHelper.ceil(this.alpha * 255.0F) << 24, 0x202020, matrices);
        matrices.pop();
    }

    @Override
    protected void renderBackground(MatrixStack matrices, MinecraftClient client, int mouseX, int mouseY) {
        int len = (int) (this.value * (double) (this.width - 8));
        int x = MathHelper.clamp(Math.abs(this.x + len), this.x, this.x + this.width - 8);

        //progress
        len = MathHelper.clamp(Math.abs(len + 4), 0, this.width);
        int color = ConfigManager.ACCENT_COLOR.apply(Style.EMPTY).getColor().getRgb();

        RenderSystem.setShaderTexture(0, SLIDER_TEXTURE);
        RenderSystem.setShaderColor(((color >> 16) & 0xFF) / 255f, ((color >>  8) & 0xFF) / 255f, (color & 0xFF) / 255f, this.alpha);
        drawTexture(matrices, this.x, this.y, 0, 0, len, 20, 200, 20);

        //button
        RenderSystem.setShaderTexture(0, WIDGETS_TEXTURE);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        int i = (!this.active ? 0 : this.isHovered() ? 2 : 1) * 20;
        this.drawTexture(matrices, x, this.y, 0, 46 + i, 4, 20);
        this.drawTexture(matrices, x + 4, this.y, 196, 46 + i, 4, 20);
    }
}
