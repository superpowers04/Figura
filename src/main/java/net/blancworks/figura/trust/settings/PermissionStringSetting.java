package net.blancworks.figura.trust.settings;

import com.google.gson.JsonElement;
import net.blancworks.figura.gui.widgets.CustomListWidget;
import net.blancworks.figura.gui.widgets.PermissionListWidget;
import net.blancworks.figura.trust.PlayerTrustData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class PermissionStringSetting extends PermissionSetting {
    public String name;
    public Text displayText;
    public String value;
    public Consumer<String> listener;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void fromNBT(CompoundTag tag) {
        value = tag.getString(name);
    }

    @Override
    public void toNBT(CompoundTag tag) {
        tag.put(name, StringTag.of(value));
    }

    @Override
    public void fromJson(JsonElement element) {
        value = element.getAsString();
    }

    @Override
    public PermissionSetting getCopy(PlayerTrustData pDat) {
        super.getCopy(pDat);
        PermissionStringSetting pfs = this;
        return new PermissionStringSetting() {{
            name = pfs.name;
            value = pfs.value;
            displayText = pfs.displayText;
            listener = pfs.listener;
            parentData = pDat;
        }};
    }

    @Override
    public PermissionListWidget.PermissionListEntry getEntry(PermissionSetting obj, CustomListWidget list) {
        PermissionStringSetting pfs = this;
        return new PermissionListWidget.PermissionStringEntry((PermissionStringSetting) obj, list) {{
            widget.setText(pfs.value);
            value = pfs.value;
            displayText = pfs.displayText;
            changedListeners.add(listener);
            parentData = pfs.parentData;
        }};
    }
}
