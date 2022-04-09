package net.blancworks.figura.gui.widgets;

import net.blancworks.figura.avatar.AvatarData;
import net.blancworks.figura.avatar.AvatarDataManager;
import net.blancworks.figura.gui.FiguraSoundScreen;
import net.blancworks.figura.lua.api.sound.FiguraSound;
import net.blancworks.figura.lua.api.sound.FiguraSoundManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class FiguraSoundWidget extends ElementListWidget<FiguraSoundWidget.Entry> {

    private static final Identifier PLAY_TEXTURE = new Identifier("figura", "textures/gui/play.png");
    private static final Identifier STOP_TEXTURE = new Identifier("figura", "textures/gui/stop.png");

    public FiguraSoundWidget(FiguraSoundScreen parent, MinecraftClient client) {
        super(client, parent.width + 45, parent.height, 43, parent.height - 32, 20);
    }

    @Override
    protected int getScrollbarPositionX() {
        return super.getScrollbarPositionX() + 15;
    }

    @Override
    public int getRowWidth() {
        return super.getRowWidth() + 150;
    }

    public void tick() {
        this.children().clear();

        AvatarData data = AvatarDataManager.localPlayer;
        if (data != null && data.script != null) {
            data.script.customSounds.forEach((name, sound) -> {
                DecimalFormat df = new DecimalFormat("#0.00", new DecimalFormatSymbols(Locale.US));
                df.setRoundingMode(RoundingMode.HALF_UP);
                float size = Float.parseFloat(df.format(sound.sample().length / 1000.0f));

                this.addEntry(new SoundEntry(new LiteralText(name).append(new LiteralText(" (" + size + "kb)").formatted(Formatting.DARK_GRAY)), sound));
            });
        }
    }

    public class SoundEntry extends Entry {
        //values
        private final Text title;

        //buttons
        private final ButtonWidget play;
        private final ButtonWidget stop;

        public SoundEntry(Text name, FiguraSound sound) {
            this.title = name;

            //play button
            this.play = new TexturedButtonWidget(
                    0, 0,
                    20, 20,
                    0, 0, 20,
                    PLAY_TEXTURE, 20, 40,
                    (bx) -> FiguraSoundManager.getChannel().playCustomSound(AvatarDataManager.localPlayer.script, sound.name(), AvatarDataManager.localPlayer.lastEntity.getPos(), 1f, 1f)
            );

            //stop button
            this.stop = new TexturedButtonWidget(
                    0, 0,
                    20, 20,
                    0, 0, 20,
                    STOP_TEXTURE, 20, 40,
                    (bx) -> FiguraSoundManager.getChannel().stopSound(sound.name())
            );
        }

        @Override
        public void render(MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            //text
            TextRenderer textRenderer = client.textRenderer;
            int posY = y + entryHeight / 2;
            textRenderer.draw(matrices, this.title, (float) x, (float) (posY - 9 / 2), 0xFFFFFF);

            //play button
            this.play.x = x + 260;
            this.play.y = y;
            this.play.render(matrices, mouseX, mouseY, tickDelta);

            //stop button
            this.stop.x = x + 285;
            this.stop.y = y;
            this.stop.render(matrices, mouseX, mouseY, tickDelta);
        }

        @Override
        public List<? extends Element> children() {
            return Arrays.asList(this.play, this.stop);
        }

        @Override
        public List<? extends Selectable> selectableChildren() {
            return Arrays.asList(this.play, this.stop);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            return this.play.mouseClicked(mouseX, mouseY, button) || this.stop.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            return this.play.mouseReleased(mouseX, mouseY, button) || this.stop.mouseReleased(mouseX, mouseY, button);
        }
    }

    public abstract static class Entry extends ElementListWidget.Entry<FiguraSoundWidget.Entry> {
        public Entry() {}
    }
}
