package net.blancworks.figura.trust.settings;

import com.google.gson.JsonElement;
import net.blancworks.figura.gui.widgets.CustomListWidget;
import net.blancworks.figura.gui.widgets.permissions.PermissionListEntry;
import net.blancworks.figura.gui.widgets.permissions.PermissionListStringEntry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.util.Identifier;

public class PermissionStringSetting extends PermissionSetting {

    public String value;

    public PermissionStringSetting(Identifier id) {
        super(id);
    }

    @Override
    public void fromNBT(CompoundTag tag) {
        value = tag.getString(id.getPath());
    }

    @Override
    public void toNBT(CompoundTag tag) {
        tag.put(id.getPath(), StringTag.of(value));
    }

    @Override
    public void fromJson(JsonElement element) {
        value = element.getAsString();
    }

    @Override
    public PermissionSetting getCopy() {
        super.getCopy();
        PermissionStringSetting pfs = this;
        return new PermissionStringSetting(id) {{
            value = pfs.value;
        }};
    }

    @Override
    public PermissionListEntry getEntry(CustomListWidget list) {
        PermissionStringSetting pfs = this;
        return new PermissionListStringEntry(this, list) {{
            widget.setText(pfs.value);
            value = pfs.value;
        }};
    }

    @Override
    public boolean isDifferent(PermissionSetting other) {
        if(other instanceof PermissionStringSetting && ((PermissionStringSetting) other).value.equals(value))
            return false;
        return true;
    }
}
