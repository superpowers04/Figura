package net.blancworks.figura.trust.settings;

import com.google.gson.JsonElement;
import net.blancworks.figura.gui.widgets.CustomListWidget;
import net.blancworks.figura.gui.widgets.permissions.PermissionListEntry;
import net.blancworks.figura.gui.widgets.permissions.PermissionListSliderEntry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtFloat;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

public class PermissionFloatSetting extends PermissionSetting {
    public float value;

    public boolean isSlider = false;
    public double min, max;
    public boolean integer = false;
    public boolean allowInfinity = false;
    public int stepSize = 1;

    public PermissionFloatSetting(Identifier id) {
        super(id);
    }

    @Override
    public void fromNBT(NbtCompound tag) {
        value = tag.getFloat(id.getPath());
    }

    @Override
    public void toNBT(NbtCompound tag) {
        tag.put(id.getPath(), NbtFloat.of(value));
    }

    @Override
    public void fromJson(JsonElement element) {
        value = element.getAsFloat();
    }

    @Override
    public PermissionSetting getCopy() {
        PermissionFloatSetting pfs = this;
        return new PermissionFloatSetting(id) {{
            value = pfs.value;
            isSlider = pfs.isSlider;
            min = pfs.min;
            max = pfs.max;
            allowInfinity = pfs.allowInfinity;
            integer = pfs.integer;
            stepSize = pfs.stepSize;
        }};
    }

    @Override
    public PermissionListEntry getEntry(CustomListWidget list) {
        PermissionFloatSetting pfs = this;
        return new PermissionListSliderEntry(this, list) {{

        }};
    }

    @Override
    public boolean isDifferent(PermissionSetting other) {
        if (other instanceof PermissionFloatSetting && ((PermissionFloatSetting) other).value == value)
            return false;
        return true;
    }

    public void setFromSlider(double value) {

        this.value = (float) MathHelper.lerp((float) value, min, max);

        if (allowInfinity && this.value >= max)
            this.value = Float.MAX_VALUE;

        if (stepSize > 0)
            this.value = (float) (Math.floor(this.value / stepSize) * stepSize);

        if (integer)
            this.value = (float) Math.floor(this.value);

        //Finally, clamp value.
        this.value = (float) MathHelper.clamp(this.value, min, max);
    }
    
    public float getSliderValue(){
        return (float) MathHelper.getLerpProgress(value, min, max);
    }

    public Text getValueText() {

        if (allowInfinity && value >= max) {
            return Text.of("INFINITY");
        }

        if (integer)
            return Text.of(String.format("%d", (int) value));
        else
            return Text.of(String.format("%.2f", value));

    }
}
