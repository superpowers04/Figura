package net.blancworks.figura.gui.widgets.permissions;

import net.blancworks.figura.gui.widgets.CustomListWidget;
import net.blancworks.figura.gui.widgets.PermissionListWidget;
import net.blancworks.figura.trust.settings.PermissionFloatSetting;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;

public class PermissionListSliderEntry extends PermissionListEntry {

    public SliderWidget widget;

    public PermissionListSliderEntry(PermissionFloatSetting obj, CustomListWidget list) {
        super(obj, list);
        
        matchingElement = widget = new SliderWidget(0, 0, 0, 20, obj.getValueText(), obj.getSliderValue()) {
            @Override
            public void updateMessage() {
                setMessage(obj.getValueText());
            }

            @Override
            public void applyValue() {
                PermissionListWidget realList = (PermissionListWidget) list;
                obj.setFromSlider(value);
                updateMessage();

                realList.setPermissionValue(obj);
            }
        };
        if(((PermissionListWidget) list).getCurrentContainer().isLocked)
            widget.active = false;
    }

    @Override
    public void render(MatrixStack matrices, int index, int y, int x, int rowWidth, int rowHeight, int mouseX, int mouseY, boolean isSelected, float delta) {
        super.render(matrices, index, y, x, rowWidth, rowHeight, mouseX, mouseY, isSelected, delta);
        
        matrices.push();
        widget.setWidth((rowWidth / 2) - 2);
        widget.x = x + 2 + (rowWidth / 2);
        widget.y = y;
        widget.render(matrices, mouseX, mouseY, delta);
        matrices.pop();
    }

    @Override
    public Text getDisplayText() {
        PermissionListWidget realList = (PermissionListWidget) list;

        if(realList.isDifferent(getEntrySetting()))
            return new TranslatableText("gui.figura." + getEntrySetting().id.getPath()).append("*").setStyle(Style.EMPTY.withBold(true).withUnderline(true));
        
        return new TranslatableText("gui.figura." + getEntrySetting().id.getPath());
    }

    @Override
    public String getIdentifier() {
        return getEntrySetting().id.toString();
    }
}
