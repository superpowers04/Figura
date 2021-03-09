package net.blancworks.figura.trust;

import net.minecraft.nbt.CompoundTag;

public interface PermissionSetting<T extends PermissionSetting> {
    //Gets the name of the setting.
    void getName();

    //Reads from NBT
    void fromNBT(CompoundTag tag);

    //Writes to NBT.
    void toNBT(CompoundTag tag);

    //Gets a copy of the permission, used for cloning settings from presets.
    public T getCopy();
}
