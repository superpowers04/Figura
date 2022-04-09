package net.blancworks.figura.gui.widgets.permissions;

import net.blancworks.figura.FiguraMod;
import net.blancworks.figura.gui.widgets.CustomListEntry;
import net.blancworks.figura.gui.widgets.CustomListWidget;
import net.blancworks.figura.gui.widgets.PermissionListWidget;
import net.blancworks.figura.trust.TrustContainer;
import net.minecraft.client.gui.Element;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;

import java.util.ArrayList;

public class PermissionListEntry extends CustomListEntry {

    public TrustContainer container;
    public Element matchingElement;
    public ArrayList<Text> tooltipText = new ArrayList<>();

    public PermissionListEntry(TrustContainer.Trust trust, CustomListWidget<?, ?> list, TrustContainer container) {
        super(trust, list);
        this.container = container;
    }

    public TrustContainer.Trust getEntrySetting() {
        return (TrustContainer.Trust) entryValue;
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if(matchingElement != null && matchingElement.isMouseOver(mouseX, mouseY))
            return matchingElement.mouseClicked(mouseX, mouseY, button);
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if(matchingElement != null && matchingElement.isMouseOver(mouseX, mouseY))
            return matchingElement.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if(matchingElement != null && matchingElement.isMouseOver(mouseX, mouseY))
            return matchingElement.mouseReleased(mouseX, mouseY, button);
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if(matchingElement != null && matchingElement.isMouseOver(mouseX, mouseY))
            return matchingElement.mouseScrolled(mouseX, mouseY, amount);
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if(matchingElement != null && matchingElement.isMouseOver(mouseX, mouseY))
            matchingElement.mouseMoved(mouseX, mouseY);
        else
            super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if(matchingElement != null)
            return matchingElement.keyPressed(keyCode, scanCode, modifiers);
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if(matchingElement != null)
            return matchingElement.keyReleased(keyCode, scanCode, modifiers);
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if(matchingElement != null)
            return matchingElement.charTyped(chr, modifiers);
        return super.charTyped(chr, modifiers);
    }

    @Override
    public Text getDisplayText() {
        PermissionListWidget realList = (PermissionListWidget) list;

        if(realList.isDifferent(getEntrySetting()))
            return new TranslatableText("figura.trust." + getEntrySetting().id).append("*").styled(FiguraMod.ACCENT_COLOR);

        return new TranslatableText("figura.trust." + getEntrySetting().id);
    }

    @Override
    public String getIdentifier() {
        return getEntrySetting().id;
    }
}