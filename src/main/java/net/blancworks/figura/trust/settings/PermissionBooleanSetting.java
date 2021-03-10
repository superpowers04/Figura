package net.blancworks.figura.trust.settings;

import com.google.gson.JsonElement;
import net.blancworks.figura.gui.widgets.CustomListWidget;
import net.blancworks.figura.gui.widgets.PermissionListWidget;
import net.blancworks.figura.trust.PlayerTrustData;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;

public class PermissionBooleanSetting extends PermissionSetting {
    public String name;
    public boolean value;
    
    @Override
    public String getName() {
        return name;
    }

    @Override
    public void fromNBT(CompoundTag tag) {
        value = tag.getBoolean(name);
    }

    @Override
    public void toNBT(CompoundTag tag) {
        tag.put(name, ByteTag.of(value));
    }

    @Override
    public void fromJson(JsonElement element) {
        value = element.getAsBoolean();
    }

    @Override
    public PermissionSetting getCopy(PlayerTrustData trustData) {
        PermissionBooleanSetting pfs = this;
        return new PermissionBooleanSetting(){{
            name = pfs.name;
            value = pfs.value;
            parentData = trustData;
        }};
    }

    @Override
    public PermissionListWidget.PermissionListEntry getEntry(PermissionSetting obj, CustomListWidget list) {
        return new PermissionListWidget.PermissionToggleEntry((PermissionBooleanSetting) obj, list);
    }
}
