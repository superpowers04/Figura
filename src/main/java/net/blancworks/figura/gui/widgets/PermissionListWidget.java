package net.blancworks.figura.gui.widgets;

import net.blancworks.figura.gui.FiguraTrustScreen;
import net.blancworks.figura.gui.widgets.permissions.PermissionListEntry;
import net.blancworks.figura.trust.PlayerTrustManager;
import net.blancworks.figura.trust.TrustContainer;
import net.blancworks.figura.trust.settings.PermissionBooleanSetting;
import net.blancworks.figura.trust.settings.PermissionFloatSetting;
import net.blancworks.figura.trust.settings.PermissionSetting;
import net.blancworks.figura.trust.settings.PermissionStringSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.function.Consumer;

public class PermissionListWidget extends CustomListWidget<PermissionSetting, PermissionListEntry> {

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

        TrustContainer tc = getCurrentContainer();
        if(tc != null)
            buildForTrustContainer(tc);
    }

    private TrustContainer getCurrentContainer(){
        FiguraTrustScreen trustScreen = (FiguraTrustScreen) getParent();

        if(trustScreen.playerListState.selected instanceof Identifier){
            Identifier groupId = (Identifier) trustScreen.playerListState.selected;
            TrustContainer tc = PlayerTrustManager.getContainer(groupId);

            return tc;
        } else if(trustScreen.playerListState.selected instanceof PlayerListEntry){
            PlayerListEntry listEntry = (PlayerListEntry) trustScreen.playerListState.selected;
            Identifier id = new Identifier("players", listEntry.getProfile().getId().toString());
            TrustContainer tc = PlayerTrustManager.getContainer(id);

            return tc;
        }

        return null;
    }

    private void buildForTrustContainer(TrustContainer tc){

        for (Identifier identifier : PlayerTrustManager.permissionDisplayOrder) {
            PermissionSetting ps = tc.getSetting(identifier);

            if(ps == null)
                continue;

            addEntry(ps.getEntry(this));
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


    public void setPermissionValue(PermissionSetting newSetting){
        TrustContainer tc = getCurrentContainer();

        if(tc == null)
            return;

        tc.setSetting(newSetting.getCopy());
    }
    
    public boolean isDifferent(PermissionSetting setting){
        TrustContainer tc = getCurrentContainer();

        if(tc == null)
            return false;
        
        if(tc.getParentIdentifier() == null)
            return false;
        
        return tc.permissionSet.containsKey(setting.id);
    }
}
