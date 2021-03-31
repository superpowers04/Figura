package net.blancworks.figura.trust.settings;

import com.google.gson.JsonElement;
import net.blancworks.figura.gui.widgets.CustomListWidget;
import net.blancworks.figura.gui.widgets.permissions.PermissionListEntry;
import net.blancworks.figura.gui.widgets.permissions.PermissionListStringEntry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtString;
import net.minecraft.util.Identifier;

public class PermissionStringSetting extends PermissionSetting {

    public String value;

    public PermissionStringSetting(Identifier id) {
        super(id);
    }

    @Override
    public void fromNBT(NbtCompound tag) {
        value = tag.getString(id.getPath());
    }

    @Override
    public void toNBT(NbtCompound tag) {
        tag.put(id.getPath(), NbtString.of(value));
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
