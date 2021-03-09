package net.blancworks.figura.gui.widgets;

import net.blancworks.figura.PlayerDataManager;
import net.blancworks.figura.gui.FiguraTrustScreen;
import net.blancworks.figura.trust.PlayerTrustData;
import net.blancworks.figura.trust.TrustPreset;
import net.blancworks.figura.trust.settings.PermissionBooleanSetting;
import net.blancworks.figura.trust.settings.PermissionFloatSetting;
import net.blancworks.figura.trust.settings.PermissionSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.*;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

import java.util.Map;
import java.util.UUID;

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
        FiguraTrustScreen trustScreen = (FiguraTrustScreen) getParent();

        if (trustScreen.playerListState.selected != null) {
            PlayerListEntry entry = (PlayerListEntry) trustScreen.playerListState.selected;
            UUID id = entry.getProfile().getId();

            PlayerTrustData trustData = PlayerDataManager.getTrustDataForPlayer(id);
            TrustPreset preset = trustData.preset;

            for (String permissionKey : PlayerTrustData.permissionRegistry.keySet()) {
                PermissionSetting pSetting = trustData.getPermission(permissionKey);

                addEntry(pSetting.getEntry(pSetting, this));
            }
        }
    }

    Element lastFocus;
    
    @Override
    public void select(PermissionListEntry entry) {
        super.select(entry);
        
        if(lastFocus != null)
            lastFocus.changeFocus(true);
        
        lastFocus = entry.matchingElement;
        lastFocus.changeFocus(true);
    }

    public static class PermissionListEntry extends CustomListEntry {

        public Element matchingElement;

        public PermissionListEntry(PermissionSetting obj, CustomListWidget list) {
            super(obj, list);
        }
    }

    public static class PermissionFloatEntry extends PermissionListEntry{

        TextFieldWidget tfw;

        public PermissionFloatEntry(PermissionFloatSetting obj, CustomListWidget list) {
            super(obj, list);
            matchingElement = tfw = new TextFieldWidget(MinecraftClient.getInstance().textRenderer, 50, 0, list.getWidth() - 60, 14, new LiteralText("Test"));
        }

        @Override
        public void render(MatrixStack matrices, int index, int y, int x, int rowWidth, int rowHeight, int mouseX, int mouseY, boolean isSelected, float delta) {
            super.render(matrices, index, y, x, rowWidth, rowHeight, mouseX, mouseY, isSelected, delta);
            tfw.x = x + (rowWidth/2);
            tfw.y = y + 3;
            tfw.setWidth((rowWidth/2) - 2);
            tfw.render(matrices, mouseX, mouseY, delta);
        }
        
        @Override
        public String getIdentifier() {
            return ((PermissionFloatSetting) getEntryObject()).name;
        }

        @Override
        public Text getDisplayText() {
            PermissionFloatSetting val = ((PermissionFloatSetting) getEntryObject());
            return new LiteralText(val.name);
        }

        @Override
        public boolean mouseClicked(double v, double v1, int i) {
            super.mouseClicked(v, v1, i);
            return true;
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            return tfw.keyPressed(keyCode, scanCode, modifiers);
        }

        @Override
        public boolean charTyped(char chr, int modifiers) {
            return tfw.charTyped(chr, modifiers);
        }

        @Override
        public boolean changeFocus(boolean lookForwards) {
            return tfw.changeFocus(lookForwards);
        }
    }

    public static class PermissionSliderEntry extends PermissionListEntry{

        SliderWidget tfw;

        public PermissionSliderEntry(PermissionFloatSetting obj, CustomListWidget list) {
            super(obj, list);
            matchingElement = tfw = new SliderWidget(0, 0, 0, 20, new LiteralText("0.00"), 0) {
                @Override
                protected void updateMessage() {
                    
                }

                @Override
                protected void applyValue() {
                    setMessage(Text.of(String.format("%.2f", value)));
                }
            };
        }

        @Override
        public void render(MatrixStack matrices, int index, int y, int x, int rowWidth, int rowHeight, int mouseX, int mouseY, boolean isSelected, float delta) {
            super.render(matrices, index, y, x, rowWidth, rowHeight, mouseX, mouseY, isSelected, delta);
            tfw.x = x + (rowWidth / 2);
            tfw.y = y;
            tfw.setWidth(rowWidth / 2);
            tfw.render(matrices, mouseX, mouseY, delta);
        }

        @Override
        public String getIdentifier() {
            return ((PermissionFloatSetting) getEntryObject()).name;
        }

        @Override
        public Text getDisplayText() {
            PermissionFloatSetting val = ((PermissionFloatSetting) getEntryObject());
            return new LiteralText(val.name);
        }

        @Override
        public boolean mouseClicked(double v, double v1, int i) {
            super.mouseClicked(v, v1, i);
            tfw.mouseClicked(v, v1, i);
            return true;
        }

        @Override
        public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
            return tfw.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            return tfw.mouseReleased(mouseX, mouseY, button);
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            return tfw.keyPressed(keyCode, scanCode, modifiers);
        }

        @Override
        public boolean charTyped(char chr, int modifiers) {
            return tfw.charTyped(chr, modifiers);
        }

        @Override
        public boolean changeFocus(boolean lookForwards) {
            return tfw.changeFocus(lookForwards);
        }
    }

    public static class PermissionToggleEntry extends PermissionListEntry{

        ButtonWidget tfw;

        public PermissionToggleEntry(PermissionBooleanSetting obj, CustomListWidget list) {
            super(obj, list);
            matchingElement = tfw = new ButtonWidget(0,0,0, 20, getDisplayText(), (ctx)->{
                PermissionBooleanSetting val = ((PermissionBooleanSetting) getEntryObject());
                val.value = !val.value;
            } );
        }

        @Override
        public void render(MatrixStack matrices, int index, int y, int x, int rowWidth, int rowHeight, int mouseX, int mouseY, boolean isSelected, float delta) {
            super.render(matrices, index, y, x, rowWidth, rowHeight, mouseX, mouseY, isSelected, delta);

            PermissionBooleanSetting val = ((PermissionBooleanSetting) getEntryObject());

            tfw.x = x + (rowWidth / 2);
            tfw.y = y;
            tfw.setWidth(rowWidth / 2);
            
            tfw.setMessage(new LiteralText(val.value ? "ON" : "OFF"));
            
            tfw.render(matrices, mouseX, mouseY, delta);
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
            tfw.mouseClicked(v, v1, i);
            return true;
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            return tfw.keyPressed(keyCode, scanCode, modifiers);
        }

        @Override
        public boolean charTyped(char chr, int modifiers) {
            return tfw.charTyped(chr, modifiers);
        }

        @Override
        public boolean changeFocus(boolean lookForwards) {
            return tfw.changeFocus(lookForwards);
        }
    }
}
