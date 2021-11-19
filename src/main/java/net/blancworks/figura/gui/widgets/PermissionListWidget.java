package net.blancworks.figura.gui.widgets;

import net.blancworks.figura.gui.FiguraTrustScreen;
import net.blancworks.figura.gui.widgets.permissions.PermissionListEntry;
import net.blancworks.figura.gui.widgets.permissions.PermissionListSliderEntry;
import net.blancworks.figura.gui.widgets.permissions.PermissionListToggleEntry;
import net.blancworks.figura.trust.PlayerTrustManager;
import net.blancworks.figura.trust.TrustContainer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class PermissionListWidget extends CustomListWidget<TrustContainer.Trust, PermissionListEntry> {

    public PermissionListWidget(MinecraftClient client, int width, int height, int y1, int y2, int entryHeight, TextFieldWidget searchBox, CustomListWidget<?, ?> list, Screen parent, CustomListWidgetState<?> state) {
        super(client, width, height, y1, y2, entryHeight, searchBox, list, parent, state);
    }

    @Override
    protected void doFiltering(String searchTerm) {
        super.doFiltering(searchTerm);
        rebuild();
    }

    @Override
    public boolean mouseClicked(double double_1, double double_2, int int_1) {
        boolean r = super.mouseClicked(double_1, double_2, int_1);

        if (r && getFocused() instanceof PermissionListEntry)
            getParent().focusOn(((PermissionListEntry) getFocused()).matchingElement);

        return r;
    }

    public void rebuild() {
        clear();

        TrustContainer tc = getCurrentContainer();
        if (tc != null)
            buildForTrustContainer(tc);
    }

    public TrustContainer getCurrentContainer() {
        FiguraTrustScreen trustScreen = (FiguraTrustScreen) getParent();

        if (trustScreen.playerListState.selected instanceof Identifier) {
            return PlayerTrustManager.getContainer((Identifier) trustScreen.playerListState.selected);
        } else if (trustScreen.playerListState.selected instanceof PlayerListEntry) {
            Identifier id = new Identifier("player", ((PlayerListEntry) trustScreen.playerListState.selected).getProfile().getId().toString());
            return PlayerTrustManager.getContainer(id);
        }

        return null;
    }

    private void buildForTrustContainer(TrustContainer tc) {
        for (TrustContainer.Trust trust : TrustContainer.Trust.values()) {
            if (trust.isBool) addEntry(new PermissionListToggleEntry(trust, this, tc));
            else addEntry(new PermissionListSliderEntry(trust, this, tc));
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
        if(lastFocus != null)
            lastFocus.changeFocus(true);
    }
    
    public boolean isDifferent(TrustContainer.Trust trust) {
        TrustContainer tc = getCurrentContainer();

        if (tc == null)
            return false;

        return tc.contains(trust);
    }
}
