package net.blancworks.figura.config;

import net.blancworks.figura.config.ConfigManager.Config;
import net.blancworks.figura.config.ConfigManager.InputType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ConfigListWidget extends ElementListWidget<ConfigListWidget.Entry> {

    //focused binding
    public KeyBinding focusedBinding;

    public ConfigListWidget(ConfigScreen parent, MinecraftClient client) {
        super(client, parent.width + 45, parent.height, 43, parent.height - 32, 20);
    }

    public void addEntries(Config[] configs) {
        for (Config config : configs) {
            switch (config.type) {
                case CATEGORY -> addEntry(new CategoryEntry(config.name));
                case BOOLEAN -> addEntry(new BooleanEntry(config.name, config.tooltip, config));
                case ENUM -> addEntry(new EnumEntry(config.name, config.tooltip, config, config.enumList));
                case INPUT -> addEntry(new InputEntry(config.name, config.tooltip, config, config.inputType));
                case KEYBIND -> addEntry(new KeyBindEntry(config.name, config.tooltip, config, config.keyBind));
            }
        }
    }

    @Override
    protected int getScrollbarPositionX() {
        return super.getScrollbarPositionX() + 15;
    }

    @Override
    public int getRowWidth() {
        return super.getRowWidth() + 150;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        this.children().forEach(entry -> {
            if (entry instanceof InputEntry inputEntry) {
                inputEntry.field.setTextFieldFocused(inputEntry.field.isMouseOver(mouseX, mouseY));
                if (inputEntry.field.isFocused())
                    inputEntry.field.setSelectionEnd(0);
            }
        });
        return super.mouseClicked(mouseX, mouseY, button);
    }

    public class CategoryEntry extends Entry {

        public CategoryEntry(Text title) {
            super(null, title, null);
        }

        @Override
        public void render(MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            //render text
            TextRenderer textRenderer = ConfigListWidget.this.client.textRenderer;
            int textWidth = ConfigListWidget.this.client.textRenderer.getWidth(this.title);
            float xPos = ConfigListWidget.this.client.currentScreen.width / 2f - textWidth / 2f;
            int yPos = y + entryHeight;
            textRenderer.draw(matrices, this.title, xPos, (float)(yPos - 9 - 1), 0xFFFFFF);
        }

        @Override
        public boolean changeFocus(boolean lookForwards) {
            return false;
        }

        @Override
        public List<? extends Element> children() {
            return Collections.emptyList();
        }

        @Override
        public List<? extends Selectable> selectableChildren() {
            return Collections.emptyList();
        }
    }

    public class BooleanEntry extends Entry {
        //values
        private final boolean initValue;

        //buttons
        private final ButtonWidget toggle;
        private final ButtonWidget reset;

        public BooleanEntry(Text title, Text tooltip, Config config) {
            super(config, title, tooltip);
            this.initValue = (boolean) config.value;

            //toggle button
            this.toggle = new ButtonWidget(0, 0, 80, 20, this.title, (button) -> config.configValue = !(boolean) config.configValue);

            //reset button
            this.reset = new ButtonWidget(0, 0, 50, 20, new TranslatableText("controls.reset"), (button) -> config.configValue = config.defaultValue);
        }

        @Override
        public void render(MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            //text
            TextRenderer textRenderer = ConfigListWidget.this.client.textRenderer;
            int posY = y + entryHeight / 2;
            textRenderer.draw(matrices, this.title, (float) x, (float) (posY - 9 / 2), 0xFFFFFF);

            //reset button
            this.reset.x = x + 250;
            this.reset.y = y;
            this.reset.active = this.config.configValue != this.config.defaultValue;
            this.reset.render(matrices, mouseX, mouseY, tickDelta);

            //toggle button
            this.toggle.x = x + 165;
            this.toggle.y = y;
            this.toggle.setMessage(new TranslatableText("gui." + ((boolean) this.config.configValue ? "yes" : "no")));

            //if setting is changed
            if ((boolean) this.config.configValue != this.initValue)
                this.toggle.setMessage(new LiteralText("").styled(ConfigManager.ACCENT_COLOR).append(this.toggle.getMessage()));

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

    public class EnumEntry extends Entry {
        private final int initValue;
        private final List<Text> states;

        //buttons
        private final ButtonWidget toggle;
        private final ButtonWidget reset;

        public EnumEntry(Text title, Text tooltip, Config config, List<Text> states) {
            super(config, title, tooltip);
            this.initValue = (int) config.value;
            this.states = states;

            //toggle button
            this.toggle = new ButtonWidget(0, 0, 80, 20, this.title, (button) -> config.configValue = (int) config.configValue + 1);

            //reset button
            this.reset = new ButtonWidget(0, 0, 50, 20, new TranslatableText("controls.reset"), (button) -> config.configValue = config.defaultValue);
        }

        @Override
        public void render(MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            //text
            TextRenderer textRenderer = ConfigListWidget.this.client.textRenderer;
            int posY = y + entryHeight / 2;
            textRenderer.draw(matrices, this.title, (float) x, (float)(posY - 9 / 2), 0xFFFFFF);

            //reset button
            this.reset.x = x + 250;
            this.reset.y = y;
            this.reset.active = !this.config.configValue.equals(this.config.defaultValue);
            this.reset.render(matrices, mouseX, mouseY, tickDelta);

            //toggle button
            this.toggle.x = x + 165;
            this.toggle.y = y;
            this.toggle.setMessage(states.get((int) this.config.configValue % states.size()));

            //if setting is changed
            if ((int) this.config.configValue != this.initValue)
                this.toggle.setMessage(new LiteralText("").styled(ConfigManager.ACCENT_COLOR).append(this.toggle.getMessage()));

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

    public class InputEntry extends Entry {
        private final Object initValue;
        private final InputType inputType;

        //buttons
        private final TextFieldWidget field;
        private final ButtonWidget reset;

        public InputEntry(Text title, Text tooltip, Config config, InputType inputType) {
            super(config, title, tooltip);
            this.initValue = config.value;
            this.inputType = inputType;

            //field
            Text fieldText;
            if (inputType == InputType.HEX_COLOR)
                fieldText = new LiteralText(String.format("#%06X", config.configValue));
            else
                fieldText = new LiteralText(config.configValue + "");

            this.field = new TextFieldWidget(ConfigListWidget.this.client.textRenderer, 0, 0, 76, 16, fieldText);
            this.field.setChangedListener((text) -> {
                // Only write config value if it's valid
                if (inputType.validator.test(text)) {
                    if (inputType == InputType.HEX_COLOR)
                        config.configValue = hexToInt(text);
                    else
                        config.configValue = text;
                }
            });
            this.field.setMaxLength(1000);
            this.field.setText(fieldText.asString());
            this.field.setCursorToStart();

            //reset button
            this.reset = new ButtonWidget(0, 0, 50, 20, new TranslatableText("controls.reset"), (button) -> {
                config.configValue = config.defaultValue;
                if (inputType == InputType.HEX_COLOR)
                    this.field.setText(String.format("#%06X", config.configValue));
                else
                    this.field.setText(config.configValue + "");
            });
        }

        @Override
        public void render(MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            //text
            TextRenderer textRenderer = ConfigListWidget.this.client.textRenderer;
            int posY = y + entryHeight / 2;

            if (this.field.isFocused() && this.inputType == InputType.HEX_COLOR) {
                Text text = new LiteralText("").append(this.title).append(" (").append(this.field.getText()).append(")");
                textRenderer.draw(matrices, text, (float) x, (float) (posY - 9 / 2), 0xFFFFFF);
            }
            else {
                textRenderer.draw(matrices, this.title, (float) x, (float) (posY - 9 / 2), 0xFFFFFF);
            }

            //reset button
            this.reset.x = x + 250;
            this.reset.y = y;
            this.reset.active = !this.config.configValue.equals(this.config.defaultValue + "");
            this.reset.render(matrices, mouseX, mouseY, tickDelta);

            //text field
            this.field.y = y + 2;

            //focused size
            int extraWidth = 0;
            if (this.field.isFocused() && !field.getText().isBlank())
                extraWidth = MathHelper.clamp(textRenderer.getWidth(field.getText()) - 50, 0, 167);

            //set size
            this.field.setWidth(76 + extraWidth);
            this.field.x = x + 167 - extraWidth;

            //set text color
            int color = 0xFFFFFF;

            if (!this.config.configValue.equals(this.initValue + ""))
                if (this.inputType == InputType.HEX_COLOR)
                    color = hexToInt(this.field.getText());
                else
                    color = ConfigManager.ACCENT_COLOR.apply(Style.EMPTY).getColor().getRgb();

            if (!inputType.validator.test(field.getText())) {
                color = 0xFF5555;
            }
            this.field.setEditableColor(color);

            //render
            this.field.render(matrices, mouseX, mouseY, tickDelta);
        }

        @Override
        public List<? extends Element> children() {
            return Arrays.asList(this.field, this.reset);
        }

        @Override
        public List<? extends Selectable> selectableChildren() {
            return Arrays.asList(this.field, this.reset);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            return this.field.mouseClicked(mouseX, mouseY, button) || this.reset.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            return this.field.mouseReleased(mouseX, mouseY, button) || this.reset.mouseReleased(mouseX, mouseY, button);
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            return super.keyPressed(keyCode, scanCode, modifiers) || this.field.keyPressed(keyCode, scanCode, modifiers);
        }

        @Override
        public boolean charTyped(char chr, int modifiers) {
            return this.field.charTyped(chr, modifiers);
        }

        public static int hexToInt(String hexString) {
            //parse hex color
            StringBuilder hex = new StringBuilder(hexString);

            if (hex.toString().startsWith("#")) hex = new StringBuilder(hex.substring(1));
            if (hex.length() < 6) {
                char[] bgChar = hex.toString().toCharArray();

                //special catch for 3
                if (hex.length() == 3)
                    hex = new StringBuilder("" + bgChar[0] + bgChar[0] + bgChar[1] + bgChar[1] + bgChar[2] + bgChar[2]);
                else
                    hex.append("0".repeat(Math.max(0, 6 - hex.toString().length())));
            }

            try {
                return Integer.parseInt(hex.toString(), 16);
            } catch (Exception ignored) {
                return ConfigManager.ACCENT_COLOR.apply(Style.EMPTY).getColor().getRgb();
            }
        }
    }

    public class KeyBindEntry extends Entry {
        private final KeyBinding binding;

        //buttons
        private final ButtonWidget toggle;
        private final ButtonWidget reset;

        public KeyBindEntry(Text title, Text tooltip, Config config, KeyBinding binding) {
            super(config, title, tooltip);
            this.binding = binding;

            //toggle button
            this.toggle = new ButtonWidget(0, 0, 80, 20, this.title, (button) -> focusedBinding = binding);

            //reset button
            this.reset = new ButtonWidget(0, 0, 50, 20, new TranslatableText("controls.reset"), (button) -> {
                binding.setBoundKey(binding.getDefaultKey());
                KeyBinding.updateKeysByCode();
            });
        }

        @Override
        public void render(MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            //text
            TextRenderer textRenderer = ConfigListWidget.this.client.textRenderer;
            int posY = y + entryHeight / 2;
            textRenderer.draw(matrices, this.title, (float) x, (float) (posY - 9 / 2), 0xFFFFFF);

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
                this.toggle.setMessage(new LiteralText("> ").styled(ConfigManager.ACCENT_COLOR).append(this.toggle.getMessage()).append(" <"));
            }
            else if (!this.binding.isUnbound()) {
                for (KeyBinding key : MinecraftClient.getInstance().options.keysAll) {
                    if (key != this.binding && this.binding.equals(key)) {
                        this.toggle.setMessage(this.toggle.getMessage().shallowCopy().formatted(Formatting.RED));
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

    public abstract static class Entry extends ElementListWidget.Entry<Entry> {
        public final Config config;
        public final Text title;
        public final Text tooltip;
        public Entry(Config config, Text title, Text tooltip) {
            this.config = config;
            this.title = title;
            this.tooltip = tooltip;
        }
    }
}