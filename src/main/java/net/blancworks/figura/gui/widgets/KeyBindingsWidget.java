package net.blancworks.figura.gui.widgets;

import net.blancworks.figura.FiguraMod;
import net.blancworks.figura.PlayerData;
import net.blancworks.figura.PlayerDataManager;
import net.blancworks.figura.gui.FiguraKeyBindsScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.client.options.KeyBinding;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;

import java.util.Arrays;
import java.util.List;

public class KeyBindingsWidget extends ElementListWidget<KeyBindingsWidget.Entry> {

    //screen
    private final FiguraKeyBindsScreen parent;

    //focused binding
    public KeyBinding focusedBinding;

    public KeyBindingsWidget(FiguraKeyBindsScreen parent, MinecraftClient client) {
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
            data.script.keyBindings.forEach(binding -> this.addEntry(new KeyBindEntry(new LiteralText(binding.getTranslationKey().split("\\$", 2)[1]), binding)));
        }
    }

    public class KeyBindEntry extends KeyBindingsWidget.Entry {
        //values
        private final Text display;
        private final KeyBinding binding;

        //buttons
        private final ButtonWidget toggle;
        private final ButtonWidget reset;

        public KeyBindEntry(Text display, KeyBinding binding) {
            this.display = display;
            this.binding = binding;

            //toggle button
            this.toggle = new ButtonWidget(0, 0, 75, 20, this.display, (button) -> focusedBinding = binding);

            //reset button
            this.reset = new ButtonWidget(0, 0, 50, 20, new TranslatableText("controls.reset"), (button) -> {
                binding.setBoundKey(binding.getDefaultKey());
                KeyBinding.updateKeysByCode();
            });
        }

        public void render(MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            //text
            TextRenderer textRenderer = KeyBindingsWidget.this.client.textRenderer;
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
            this.toggle.setMessage(this.binding.getBoundKeyLocalizedText());

            if (focusedBinding == this.binding) {
                this.toggle.setMessage(new LiteralText("> ").styled(FiguraMod.ACCENT_COLOR).append(this.toggle.getMessage()).append(" <"));
            }
            else if (!this.binding.isUnbound()) {
                for (KeyBinding key : MinecraftClient.getInstance().options.keysAll) {
                    if (key != this.binding && this.binding.equals(key)) {
                        this.toggle.setMessage(this.toggle.getMessage().shallowCopy().formatted(Formatting.RED));

                        //render overlay
                        if (isMouseOver(mouseX, mouseY)) {
                            matrices.push();
                            matrices.translate(0, 0, 599);
                            parent.renderTooltip(matrices, new TranslatableText("figura.keybinds.warning").formatted(Formatting.RED), mouseX, mouseY);
                            matrices.pop();
                        }

                        break;
                    }
                }
            }

            this.toggle.render(matrices, mouseX, mouseY, tickDelta);
        }

        @Override
        public List<? extends Element> children() {
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
