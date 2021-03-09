package net.blancworks.figura.trust.settings;

import net.blancworks.figura.gui.widgets.CustomListWidget;
import net.blancworks.figura.gui.widgets.PermissionListWidget;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;

public class PermissionFloatSetting implements PermissionSetting {
    public String name;
    public float value;
    
    public boolean isSlider = false;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void fromNBT(CompoundTag tag) {
        value = tag.getFloat(name);
    }

    @Override
    public void toNBT(CompoundTag tag) {
        tag.put(name, FloatTag.of(value));
    }

    @Override
    public PermissionSetting getCopy() {
        PermissionFloatSetting pfs = this;
        return new PermissionFloatSetting(){{ 
            name = pfs.name;
            value = pfs.value;
            isSlider = pfs.isSlider;
        }};
    }

    @Override
    public PermissionListWidget.PermissionListEntry getEntry(PermissionSetting obj, CustomListWidget list) {
        if(isSlider)
            return new PermissionListWidget.PermissionSliderEntry((PermissionFloatSetting) obj, list);
        return new PermissionListWidget.PermissionFloatEntry((PermissionFloatSetting) obj, list);
    }
}
