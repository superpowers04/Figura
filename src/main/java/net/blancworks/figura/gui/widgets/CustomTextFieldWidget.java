package net.blancworks.figura.gui.widgets;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

public class CustomTextFieldWidget extends TextFieldWidget {

    private final TextRenderer textRenderer;

    public CustomTextFieldWidget(TextRenderer textRenderer, int x, int y, int width, int height, @Nullable TextFieldWidget copyFrom, Text text) {
        super(textRenderer, x, y, width, height, copyFrom, text);
        this.textRenderer = textRenderer;
    }

    @Override
    public void renderButton(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        super.renderButton(matrices, mouseX, mouseY, delta);
        if (this.getText().isEmpty() && !this.isFocused()) {
            this.textRenderer.drawWithShadow(matrices, this.getMessage(), this.x + 4, this.y + (this.height - 8) / 2, 7368816);
        }
    }
}
