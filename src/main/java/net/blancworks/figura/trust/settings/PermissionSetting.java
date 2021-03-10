package net.blancworks.figura.trust.settings;

import com.google.gson.JsonElement;
import net.blancworks.figura.gui.widgets.CustomListWidget;
import net.blancworks.figura.gui.widgets.PermissionListWidget;
import net.blancworks.figura.trust.PlayerTrustData;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.nbt.CompoundTag;

public abstract class PermissionSetting<T extends PermissionSetting> {

    public PlayerTrustData parentData;

    //Gets the name of the setting.
    public String getName() {
        return null;
    }

    //Reads from NBT
    public void fromNBT(CompoundTag tag) {
    }

    //Writes to NBT.
    public void toNBT(CompoundTag tag) {
    }

    public void fromJson(JsonElement element) {
    }

    //Gets a copy of the permission, used for cloning settings from presets.
    public T getCopy(PlayerTrustData parentData) {
        return null;
    }

    public PermissionListWidget.PermissionListEntry getEntry(PermissionSetting obj, CustomListWidget list) {
        return null;
    }
}
