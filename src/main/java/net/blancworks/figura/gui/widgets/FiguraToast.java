package net.blancworks.figura.gui.widgets;

import com.mojang.blaze3d.systems.RenderSystem;
import net.blancworks.figura.FiguraMod;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.toast.Toast;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class FiguraToast implements Toast {

    private final Text title;
    private final Text message;
    private long startTime;
    private boolean justUpdated;

    private static final Identifier TEXTURE = new Identifier("figura", "textures/gui/toast.png");

    public FiguraToast(Text title, Text message) {
        this.title = title;
        this.message = message;
    }

    @Override
    public Visibility draw(MatrixStack matrices, ToastManager manager, long startTime) {
        if (this.justUpdated) {
            this.startTime = startTime;
            this.justUpdated = false;
        }

        RenderSystem.setShaderTexture(0, TEXTURE);
        boolean cheese = FiguraMod.IS_CHEESE || Math.random() < 0.0001;
        DrawableHelper.drawTexture(matrices, 0, 0, 0f, cheese ? 32f : 0f, 160, 32, 160, 64);

        if (this.message == null) {
            manager.getGame().textRenderer.draw(matrices, this.title, 29f, 12f, 0x55FFFF);
        } else {
            manager.getGame().textRenderer.draw(matrices, this.title, 29f, 7f, 0x55FFFF);
            manager.getGame().textRenderer.draw(matrices, this.message, 29f, 18f, 0xFFFFFF);
        }

        return startTime - this.startTime < 5000L ? Toast.Visibility.SHOW : Toast.Visibility.HIDE;
    }
}
