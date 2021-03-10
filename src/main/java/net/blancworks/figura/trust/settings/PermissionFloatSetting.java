package net.blancworks.figura.trust.settings;

import com.google.gson.JsonElement;
import net.blancworks.figura.gui.widgets.CustomListWidget;
import net.blancworks.figura.gui.widgets.PermissionListWidget;
import net.blancworks.figura.trust.PlayerTrustData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

public class PermissionFloatSetting extends PermissionSetting {
    public String name;
    public Text displayText;
    public float value;
    
    public boolean isSlider = false;
    public double min, max;
    public boolean integer = false;
    public boolean allowInfinity = false;
    public int multiplier = 1;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void fromNBT(CompoundTag tag) {
        value = tag.getFloat(name);
    }

    @Override
    public void toNBT(CompoundTag tag) {
        tag.put(name, FloatTag.of(value));
    }

    @Override
    public void fromJson(JsonElement element) {
        value = element.getAsFloat();
    }

    @Override
    public PermissionSetting getCopy(PlayerTrustData pData) {
        PermissionFloatSetting pfs = this;
        return new PermissionFloatSetting(){{ 
            name = pfs.name;
            value = pfs.value;
            isSlider = pfs.isSlider;
            min = pfs.min;
            max = pfs.max;
            allowInfinity = pfs.allowInfinity;
            integer = pfs.integer;
            multiplier = pfs.multiplier;
            displayText = pfs.displayText.copy();
            parentData = pData;
        }};
    }

    @Override
    public PermissionListWidget.PermissionListEntry getEntry(PermissionSetting obj, CustomListWidget list) {
        PermissionFloatSetting pfs = this;
        return new PermissionListWidget.PermissionSliderEntry((PermissionFloatSetting) obj, list){{
            double setVal = MathHelper.getLerpProgress(value / multiplier, min, max);
            widget.setValue( setVal );
            widget.refreshMessage();
        }};
    }
}
