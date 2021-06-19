package net.blancworks.figura.trust.settings;

import com.google.gson.JsonElement;
import net.blancworks.figura.gui.widgets.CustomListWidget;
import net.blancworks.figura.gui.widgets.permissions.PermissionListEntry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;

public abstract class PermissionSetting<T extends PermissionSetting> {
    public Identifier id;

    public PermissionSetting(Identifier id) {
        this.id = id;
    }

    //Reads from NBT
    public void fromNBT(NbtCompound tag) {
    }

    //Writes to NBT.
    public void toNBT(NbtCompound tag) {
        
    }

    public void fromJson(JsonElement element) {
        
    }

    //Gets a copy of the permission, used for cloning settings from presets.
    public T getCopy() {
        return null;
    }

    public PermissionListEntry getEntry(CustomListWidget list) {
        return null;
    }

    public boolean isDifferent(PermissionSetting other){
        return true;
    }
}
