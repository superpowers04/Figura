package net.blancworks.figura.gui.widgets.permissions;

import net.blancworks.figura.gui.widgets.CustomListWidget;
import net.blancworks.figura.gui.widgets.PermissionListWidget;
import net.blancworks.figura.trust.settings.PermissionFloatSetting;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

public class PermissionListSliderEntry extends PermissionListEntry {

    public SliderWidget widget;

    public PermissionListSliderEntry(PermissionFloatSetting obj, CustomListWidget list) {
        super(obj, list);

        widget = new SliderWidget(0, 0, 0, 0, Text.of("NULL"), 0) {
            @Override
            protected void updateMessage() {
                setMessage(obj.getValueText());
            }

            @Override
            protected void applyValue() {
                PermissionListWidget realList = (PermissionListWidget) list;
                obj.setFromSlider(value);
                updateMessage();

                realList.setPermissionValue(obj);
            }
        };
    }
}
