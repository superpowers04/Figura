package net.blancworks.figura.trust;

import net.blancworks.figura.gui.widgets.PermissionListWidget;
import net.blancworks.figura.trust.settings.PermissionBooleanSetting;
import net.blancworks.figura.trust.settings.PermissionFloatSetting;
import net.blancworks.figura.trust.settings.PermissionSetting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTag;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class PlayerTrustData {

    public static HashMap<String, Supplier<PermissionSetting>> permissionRegistry = new HashMap<String, Supplier<PermissionSetting>>();
    public static HashMap<String, TrustPreset> allPresets = new HashMap<>();

    public static void init() {
        registerPermissionSetting("testString", () -> new PermissionFloatSetting() {{
            name = "testString";
        }});
        registerPermissionSetting("testSlider", () -> new PermissionFloatSetting() {{
            name = "testSlider";
            isSlider = true;
        }});
        registerPermissionSetting("testToggle", () -> new PermissionBooleanSetting() {{
            name = "testToggle";
        }});
        registerPermissionSetting("testToggle 2", () -> new PermissionBooleanSetting() {{
            name = "testToggle 2";
        }});


        allPresets.put("UNTRUSTED", getBlancPreset());
    }

    public static void registerPermissionSetting(String key, Supplier<PermissionSetting> supplier) {
        permissionRegistry.put(key, supplier);
    }

    public static TrustPreset getBlancPreset() {
        TrustPreset newSet = new TrustPreset();

        for (Map.Entry<String, Supplier<PermissionSetting>> stringSupplierEntry : permissionRegistry.entrySet()) {
            newSet.permissions.put(stringSupplierEntry.getKey(), stringSupplierEntry.getValue().get());
        }
        
        return newSet;
    }

    public TrustPreset preset;
    public HashMap<String, PermissionSetting> permissions = new HashMap<String, PermissionSetting>();


    public void fromNBT(CompoundTag tag) {

        if (preset != null) {
            tag.put("preset", StringTag.of(preset.name));
        }

    }

    public void toNBT(CompoundTag tag) {
        if (tag.contains("preset") && allPresets.containsKey("preset")) {
            preset = allPresets.get(preset);
        }
    }

    //Resets the trust data to the current preset.
    public void reset() {
        permissions.clear();
    }


    public PermissionSetting getPermission(String key) {
        //Get from override permissions first
        if (permissions.containsKey(key)) {
            return permissions.get(key);
        }
        //Get from preset next
        if (preset.permissions.containsKey(key)) {
            return preset.permissions.get(key);
        }
        return null;
    }

    public void setPermissions(String key, PermissionSetting setting) {
        permissions.put(key, setting);
    }
}
