package net.blancworks.figura.trust.settings;

import net.blancworks.figura.gui.widgets.CustomListWidget;
import net.blancworks.figura.gui.widgets.PermissionListWidget;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.nbt.CompoundTag;

public interface PermissionSetting<T extends PermissionSetting> {
    //Gets the name of the setting.
    String getName();

    //Reads from NBT
    void fromNBT(CompoundTag tag);

    //Writes to NBT.
    void toNBT(CompoundTag tag);

    //Gets a copy of the permission, used for cloning settings from presets.
    T getCopy();
    
    PermissionListWidget.PermissionListEntry getEntry(PermissionSetting obj, CustomListWidget list);
}
