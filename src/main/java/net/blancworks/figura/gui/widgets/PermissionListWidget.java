package net.blancworks.figura.gui.widgets;

import net.blancworks.figura.PlayerDataManager;
import net.blancworks.figura.gui.FiguraTrustScreen;
import net.blancworks.figura.trust.PlayerTrustData;
import net.blancworks.figura.trust.TrustPreset;
import net.blancworks.figura.trust.settings.PermissionBooleanSetting;
import net.blancworks.figura.trust.settings.PermissionFloatSetting;
import net.blancworks.figura.trust.settings.PermissionSetting;
import net.blancworks.figura.trust.settings.PermissionStringSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.*;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.UUID;
import java.util.function.Consumer;

public class PermissionListWidget extends CustomListWidget<PermissionSetting, PermissionListWidget.PermissionListEntry> {

    public PermissionListWidget(MinecraftClient client, int width, int height, int y1, int y2, int entryHeight, TextFieldWidget searchBox, CustomListWidget list, Screen parent, CustomListWidgetState state) {
        super(client, width, height, y1, y2, entryHeight, searchBox, list, parent, state);
        //allowSelection = false;
    }

    @Override
    protected void doFiltering(String searchTerm) {
        super.doFiltering(searchTerm);
        rebuild();
    }

    @Override
    public boolean mouseClicked(double double_1, double double_2, int int_1) {
        boolean r = super.mouseClicked(double_1, double_2, int_1);

        if (r) {
            if (getFocused() instanceof PermissionListEntry) {
                PermissionListEntry focused = (PermissionListEntry) getFocused();

                getParent().focusOn(focused.matchingElement);
            }
        }

        return r;
    }

    public void rebuild() {
        clear();
        FiguraTrustScreen trustScreen = (FiguraTrustScreen) getParent();

        if (trustScreen.playerListState.selected != null) {
            if (trustScreen.playerListState.selected instanceof String) {
                String entry = (String) trustScreen.playerListState.selected;

                TrustPreset preset = PlayerTrustData.allPresets.get(entry);

                for (String permissionKey : PlayerTrustData.permissionRegistry.keySet()) {
                    PermissionSetting pSetting = preset.permissions.get(permissionKey);

                    if (permissionKey.equals("preset"))
                        continue;

                    addEntry(pSetting.getEntry(pSetting, this));
                }
            } else {
                PlayerListEntry entry = (PlayerListEntry) trustScreen.playerListState.selected;
                UUID id = entry.getProfile().getId();

                PlayerTrustData trustData = PlayerDataManager.getTrustDataForPlayer(id);
                TrustPreset preset = trustData.preset;

                for (String permissionKey : PlayerTrustData.permissionRegistry.keySet()) {
                    PermissionSetting pSetting = trustData.getPermission(permissionKey);

                    if (pSetting instanceof PermissionStringSetting) {
                        PermissionStringEntry newEntry = (PermissionStringEntry) pSetting.getEntry(pSetting, this);
                        if (permissionKey.equals("preset")) {
                            newEntry.changedListeners.add((str) -> {
                                PlayerTrustData.moveToPreset(newEntry.parentData, str);
                            });
                        }
                        newEntry.changedListeners.add((str) -> {
                            newEntry.parentData.reset();
                            trustScreen.playerList.reloadFilters();
                            rebuild();
                        });
                        
                        newEntry.tooltipText.add(new LiteralText("Press enter to confirm group change."));
                        newEntry.tooltipText.add(new LiteralText("You can also shift-click a group with this option selected to change the group."));
                        newEntry.tooltipText.add(new LiteralText("NOTE: Changing groups will reset values").setStyle(Style.EMPTY.withColor(TextColor.parse("yellow"))));

                        addEntry(newEntry);
                    } else {
                        addEntry(pSetting.getEntry(pSetting, this));
                    }
                }
            }
        }
    }

    public void clear() {
        addedObjects.clear();
        clearEntries();
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        super.render(matrices, mouseX, mouseY, delta);

        FiguraTrustScreen trustScreen = (FiguraTrustScreen) getParent();

        for (int i = 0; i < getEntryCount(); i++) {
            PermissionListEntry entry = (PermissionListEntry) getEntry(i);
            
            if(entry.tooltipText != null && entry.matchingElement != null && entry.matchingElement.isMouseOver(mouseX,mouseY)){
                trustScreen.renderTooltip(matrices, entry.tooltipText, mouseX, mouseY);
            }
        }
    }

    Element lastFocus;

    @Override
    public void select(PermissionListEntry entry) {
        super.select(entry);

        if (lastFocus != null)
            lastFocus.changeFocus(true);

        lastFocus = entry.matchingElement;
        lastFocus.changeFocus(true);
    }

    public static class PermissionListEntry extends CustomListEntry {

        public PlayerTrustData parentData;
        public Element matchingElement;
        
        public ArrayList<Text> tooltipText = new ArrayList<Text>();

        public PermissionListEntry(PermissionSetting obj, CustomListWidget list) {
            super(obj, list);
        }
    }

    public static class PermissionStringEntry extends PermissionListEntry {

        public TextFieldWidget widget;
        public ArrayList<Consumer<String>> changedListeners = new ArrayList<>();

        public PermissionStringEntry(PermissionStringSetting obj, CustomListWidget list) {
            super(obj, list);
            matchingElement = widget = new TextFieldWidget(MinecraftClient.getInstance().textRenderer, 50, 0, list.getWidth() - 60, 14, new LiteralText("Test"));
            widget.setChangedListener((str) -> {
                PermissionStringSetting val = ((PermissionStringSetting) getEntryObject());
                val.value = str;
            });
        }

        @Override
        public void render(MatrixStack matrices, int index, int y, int x, int rowWidth, int rowHeight, int mouseX, int mouseY, boolean isSelected, float delta) {
            super.render(matrices, index, y, x, rowWidth, rowHeight, mouseX, mouseY, isSelected, delta);
            widget.x = x + (rowWidth / 2);
            widget.y = y + 3;
            widget.setWidth((rowWidth / 2) - 2);
            widget.render(matrices, mouseX, mouseY, delta);
        }

        @Override
        public String getIdentifier() {
            return ((PermissionStringSetting) getEntryObject()).name;
        }

        @Override
        public Text getDisplayText() {
            PermissionStringSetting val = ((PermissionStringSetting) getEntryObject());
            return val.displayText;
        }

        @Override
        public boolean mouseClicked(double v, double v1, int i) {
            super.mouseClicked(v, v1, i);
            return true;
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (keyCode == GLFW.GLFW_KEY_ENTER) {
                PermissionStringSetting val = ((PermissionStringSetting) getEntryObject());

                for (Consumer<String> changedListener : changedListeners) {
                    if(changedListener != null)
                        changedListener.accept(val.value);
                }
            }
            return widget.keyPressed(keyCode, scanCode, modifiers);
        }

        @Override
        public boolean charTyped(char chr, int modifiers) {
            return widget.charTyped(chr, modifiers);
        }

        @Override
        public boolean changeFocus(boolean lookForwards) {
            return widget.changeFocus(lookForwards);
        }
    }

    public static class PermissionSliderEntry extends PermissionListEntry {

        public CustomSliderWidget widget;

        public PermissionSliderEntry(PermissionFloatSetting obj, CustomListWidget list) {
            super(obj, list);
            matchingElement = widget = new CustomSliderWidget(0, 0, 0, 20, new LiteralText("0.00"), 0) {
                @Override
                public void updateMessage() {
                    PermissionFloatSetting val = ((PermissionFloatSetting) getEntryObject());

                    if (val.allowInfinity && value > 0.999) {
                        setMessage(Text.of("INFINITY"));
                        return;
                    }

                    if (val.integer) {
                        setMessage(Text.of(String.format("%d", (int) getRealValue())));
                    } else {
                        setMessage(Text.of(String.format("%.2f", getRealValue())));
                    }
                }

                @Override
                public void applyValue() {
                    PermissionFloatSetting val = ((PermissionFloatSetting) getEntryObject());

                    if (val.allowInfinity && value > 0.999) {
                        val.value = Float.MAX_VALUE;
                    } else {
                        val.value = (float) getRealValue();
                    }
                }

                public double getRealValue() {
                    PermissionFloatSetting val = ((PermissionFloatSetting) getEntryObject());

                    if (val.integer)
                        return MathHelper.floor(MathHelper.lerp(value, val.min, val.max)) * val.multiplier;
                    return MathHelper.lerp(value, val.min, val.max) * val.multiplier;
                }
            };
        }

        @Override
        public void render(MatrixStack matrices, int index, int y, int x, int rowWidth, int rowHeight, int mouseX, int mouseY, boolean isSelected, float delta) {
            super.render(matrices, index, y, x, rowWidth, rowHeight, mouseX, mouseY, isSelected, delta);
            widget.x = x + (rowWidth / 2);
            widget.y = y;
            widget.setWidth(rowWidth / 2);
            widget.render(matrices, mouseX, mouseY, delta);
        }

        @Override
        public String getIdentifier() {
            return ((PermissionFloatSetting) getEntryObject()).name;
        }

        @Override
        public Text getDisplayText() {
            PermissionFloatSetting val = ((PermissionFloatSetting) getEntryObject());
            return val.displayText;
        }

        @Override
        public boolean mouseClicked(double v, double v1, int i) {
            super.mouseClicked(v, v1, i);
            widget.mouseClicked(v, v1, i);
            return true;
        }

        @Override
        public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
            return widget.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            return widget.mouseReleased(mouseX, mouseY, button);
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            return widget.keyPressed(keyCode, scanCode, modifiers);
        }

        @Override
        public boolean charTyped(char chr, int modifiers) {
            return widget.charTyped(chr, modifiers);
        }

        @Override
        public boolean changeFocus(boolean lookForwards) {
            return widget.changeFocus(lookForwards);
        }
    }

    public static class PermissionToggleEntry extends PermissionListEntry {

        public ButtonWidget widget;

        public PermissionToggleEntry(PermissionBooleanSetting obj, CustomListWidget list) {
            super(obj, list);
            matchingElement = widget = new ButtonWidget(0, 0, 0, 20, getDisplayText(), (ctx) -> {
                PermissionBooleanSetting val = ((PermissionBooleanSetting) getEntryObject());
                val.value = !val.value;
            });
        }

        @Override
        public void render(MatrixStack matrices, int index, int y, int x, int rowWidth, int rowHeight, int mouseX, int mouseY, boolean isSelected, float delta) {
            super.render(matrices, index, y, x, rowWidth, rowHeight, mouseX, mouseY, isSelected, delta);

            PermissionBooleanSetting val = ((PermissionBooleanSetting) getEntryObject());

            widget.x = x + (rowWidth / 2);
            widget.y = y;
            widget.setWidth(rowWidth / 2);

            widget.setMessage(new LiteralText(val.value ? "ON" : "OFF"));

            widget.render(matrices, mouseX, mouseY, delta);
        }

        @Override
        public String getIdentifier() {
            return ((PermissionBooleanSetting) getEntryObject()).name;
        }

        @Override
        public Text getDisplayText() {
            PermissionBooleanSetting val = ((PermissionBooleanSetting) getEntryObject());
            return new LiteralText(val.name);
        }

        @Override
        public boolean mouseClicked(double v, double v1, int i) {
            super.mouseClicked(v, v1, i);
            widget.mouseClicked(v, v1, i);
            return true;
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            return widget.keyPressed(keyCode, scanCode, modifiers);
        }

        @Override
        public boolean charTyped(char chr, int modifiers) {
            return widget.charTyped(chr, modifiers);
        }

        @Override
        public boolean changeFocus(boolean lookForwards) {
            return widget.changeFocus(lookForwards);
        }
    }
}
