package net.blancworks.figura.gui.widgets;

import net.blancworks.figura.FiguraMod;
import net.blancworks.figura.PlayerData;
import net.blancworks.figura.PlayerDataManager;
import net.blancworks.figura.gui.FiguraKeyBindsScreen;
import net.blancworks.figura.lua.api.keybind.FiguraKeybind;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;

import java.util.Arrays;
import java.util.List;

public class FiguraKeybindWidget extends ElementListWidget<KeyBindingsWidget.Entry> {

    //screen
    private final FiguraKeyBindsScreen parent;

    //focused binding
    public FiguraKeybind focusedBinding;

    public FiguraKeybindWidget(FiguraKeyBindsScreen parent, MinecraftClient client) {
        super(client, parent.width + 45, parent.height, 43, parent.height - 32, 20);
        this.parent = parent;
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

        PlayerData data = PlayerDataManager.localPlayer;
        if (data != null && data.script != null) {
            data.script.keyBindings.forEach(binding -> this.addEntry(new FiguraKeybindEntry(Text.of(binding.name), binding)));
        }
    }

    public class FiguraKeybindEntry extends KeyBindingsWidget.Entry {
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
            this.toggle = new ButtonWidget(0, 0, 75, 20, this.display, (button) -> focusedBinding = binding);

            //reset button
            this.reset = new ButtonWidget(0, 0, 50, 20, new TranslatableText("controls.reset"), (button) -> {
                binding.resetToDefault();
            });
        }

        public void render(MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            //text
            TextRenderer textRenderer = FiguraKeybindWidget.this.client.textRenderer;
            int posY = y + entryHeight / 2;
            textRenderer.draw(matrices, this.display, (float) x, (float) (posY - 9 / 2), 16777215);

            //reset button
            this.reset.x = x + 250;
            this.reset.y = y;
            this.reset.active = !this.binding.isDefault();
            this.reset.render(matrices, mouseX, mouseY, tickDelta);

            //toggle button
            this.toggle.x = x + 165;
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

    public abstract static class Entry extends ElementListWidget.Entry<KeyBindingsWidget.Entry> {
        public Entry() {}
    }
}
