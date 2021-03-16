package net.blancworks.figura.trust.settings;

import com.google.gson.JsonElement;
import net.blancworks.figura.gui.widgets.CustomListWidget;
import net.blancworks.figura.gui.widgets.permissions.PermissionListEntry;
import net.blancworks.figura.gui.widgets.permissions.PermissionListToggleEntry;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Identifier;

public class PermissionBooleanSetting extends PermissionSetting {
    public boolean value;

    public PermissionBooleanSetting(Identifier id) {
        super(id);
    }

    @Override
    public void fromNBT(CompoundTag tag) {
        value = tag.getBoolean(id.getPath());
    }

    @Override
    public void toNBT(CompoundTag tag) {
        tag.put(id.getPath(), ByteTag.of(value));
    }

    @Override
    public void fromJson(JsonElement element) {
        value = element.getAsBoolean();
    }

    @Override
    public PermissionSetting getCopy() {
        PermissionBooleanSetting pfs = this;
        return new PermissionBooleanSetting(id){{
            value = pfs.value;
        }};
    }

    @Override
    public PermissionListEntry getEntry(CustomListWidget list) {
        return new PermissionListToggleEntry(this, list);
    }

    @Override
    public boolean isDifferent(PermissionSetting other) {
        if (other instanceof PermissionBooleanSetting && ((PermissionBooleanSetting) other).value == value)
            return true;
        return false;
    }
}
