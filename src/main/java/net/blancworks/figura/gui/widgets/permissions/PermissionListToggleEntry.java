package net.blancworks.figura.gui.widgets.permissions;

import net.blancworks.figura.gui.widgets.CustomListWidget;
import net.blancworks.figura.gui.widgets.PermissionListWidget;
import net.blancworks.figura.trust.settings.PermissionBooleanSetting;
import net.blancworks.figura.trust.settings.PermissionSetting;
import net.minecraft.client.gui.widget.ToggleButtonWidget;

public class PermissionListToggleEntry extends PermissionListEntry{
    public ToggleButtonWidget widget;

    public PermissionListToggleEntry(PermissionBooleanSetting obj, CustomListWidget list) {
        super(obj, list);

        widget = new ToggleButtonWidget(0, 0, 0 ,0, obj.value){
            @Override
            public void onClick(double mouseX, double mouseY) {
                obj.value = !obj.value;
                toggled = obj.value;
                ((PermissionListWidget)list).setPermissionValue(obj);
            }
        };
    }
}
