package net.blancworks.figura.trust.settings;

import com.google.gson.JsonElement;
import net.blancworks.figura.gui.widgets.CustomListWidget;
import net.blancworks.figura.gui.widgets.permissions.PermissionListEntry;
import net.blancworks.figura.gui.widgets.permissions.PermissionListToggleEntry;
import net.minecraft.nbt.NbtByte;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;

public class PermissionBooleanSetting extends PermissionSetting {
    public boolean value;

    public PermissionBooleanSetting(Identifier id) {
        super(id);
    }

    @Override
    public void fromNBT(NbtCompound tag) {
        value = tag.getBoolean(id.getPath());
    }

    @Override
    public void toNBT(NbtCompound tag) {
        tag.put(id.getPath(), NbtByte.of(value));
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
            return false;
        return true;
    }
}
