package net.blancworks.figura.gui.widgets;

import net.blancworks.figura.FiguraMod;
import net.blancworks.figura.avatar.AvatarData;
import net.blancworks.figura.avatar.AvatarDataManager;
import net.blancworks.figura.gui.FiguraKeyBindsScreen;
import net.blancworks.figura.lua.api.keybind.FiguraKeybind;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;

import java.util.Arrays;
import java.util.List;

public class FiguraKeybindWidget extends ElementListWidget<FiguraKeybindWidget.Entry> {

    //focused binding
    public FiguraKeybind focusedBinding;

    public FiguraKeybindWidget(FiguraKeyBindsScreen parent, MinecraftClient client) {
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
            data.script.keyBindings.forEach(binding -> this.addEntry(new FiguraKeybindEntry(Text.of(binding.name), binding)));
        }
    }

    public class FiguraKeybindEntry extends FiguraKeybindWidget.Entry {
        //values
        private final Text display;
        private final FiguraKeybind binding;

        //buttons
        private final ButtonWidget toggle;
        private final ButtonWidget reset;

        public FiguraKeybindEntry(Text display, FiguraKeybind binding) {
            this.display = display;
            this.binding = binding;

            //toggle button
            this.toggle = new ButtonWidget(0, 0, 80, 20, this.display, (button) -> focusedBinding = binding);

            //reset button
            this.reset = new ButtonWidget(0, 0, 40, 20, new TranslatableText("controls.reset"), (button) -> binding.resetToDefault());
        }

        public void render(MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            //text
            TextRenderer textRenderer = FiguraKeybindWidget.this.client.textRenderer;
            int posY = y + entryHeight / 2;
            textRenderer.draw(matrices, this.display, (float) x, (float) (posY - 9 / 2), 16777215);

            //reset button
            this.reset.x = x + 260;
            this.reset.y = y;
            this.reset.active = !this.binding.isDefault();
            this.reset.render(matrices, mouseX, mouseY, tickDelta);

            //toggle button
            this.toggle.x = x + 175;
            this.toggle.y = y;
            this.toggle.setMessage(this.binding.getLocalizedText());

            if (focusedBinding == this.binding) {
                this.toggle.setMessage(new LiteralText("> ").styled(FiguraMod.ACCENT_COLOR).append(this.toggle.getMessage()).append(" <"));
            }

            this.toggle.render(matrices, mouseX, mouseY, tickDelta);
        }

        @Override
        public List<? extends Element> children() {
            return Arrays.asList(this.toggle, this.reset);
        }

        @Override
        public List<? extends Selectable> selectableChildren() {
            return Arrays.asList(this.toggle, this.reset);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            return this.toggle.mouseClicked(mouseX, mouseY, button) || this.reset.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            return this.toggle.mouseReleased(mouseX, mouseY, button) || this.reset.mouseReleased(mouseX, mouseY, button);
        }
    }

    public abstract static class Entry extends ElementListWidget.Entry<FiguraKeybindWidget.Entry> {
        public Entry() {}
    }
}
