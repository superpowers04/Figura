package net.blancworks.figura.trust;

import net.blancworks.figura.trust.settings.PermissionFloatSetting;
import net.blancworks.figura.trust.settings.PermissionSetting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class PlayerTrustManager {

    public static final Identifier untrustedID = new Identifier("groups", "untrusted");
    public static final Identifier maxInitID = new Identifier("setting", "maxinitinstructions");
    public static final Identifier maxTickID = new Identifier("setting", "maxtickinstructions");
    public static final Identifier maxRenderID = new Identifier("setting", "maxrenderinstructions");
    public static final Identifier maxComplexityID = new Identifier("setting", "maxcomplexity");
    public static HashMap<Identifier, TrustContainer> allContainers = new HashMap<Identifier, TrustContainer>();
    public static ArrayList<Identifier> allGroups = new ArrayList<Identifier>();
    public static ArrayList<Identifier> defaultGroups = new ArrayList<Identifier>();
    public static HashMap<Identifier, PermissionSetting> permissionSettings = new HashMap<>();
    public static ArrayList<Identifier> permissionDisplayOrder = new ArrayList<Identifier>();

    //Loads all the default groups from the json config file.
    public static void init() {
        registerPermissions();
        loadDefaultGroups();
    }

    public static void registerPermissions() {
        registerPermissionSetting(new PermissionFloatSetting(maxInitID) {{
            min = 0;
            max = 1024 * 11;
            value = 1024 * 10;
            integer = true;
            stepSize = 256;
            isSlider = true;
        }});

        registerPermissionSetting(new PermissionFloatSetting(maxTickID) {{
            min = 0;
            max = 1024 * 11;
            value = 1024 * 5;
            integer = true;
            stepSize = 256;
            isSlider = true;
        }});

        registerPermissionSetting(new PermissionFloatSetting(maxRenderID) {{
            min = 0;
            max = 1024 * 11;
            value = 1024 * 2;
            integer = true;
            stepSize = 256;
            isSlider = true;
        }});

        registerPermissionSetting(new PermissionFloatSetting(maxComplexityID) {{
            min = 0;
            max = 64 * 257;
            value = 64 * 256;
            integer = true;
            stepSize = 32;
            isSlider = true;
        }});
    }

    public static void loadDefaultGroups() {
        TrustContainer container = new TrustContainer(untrustedID, Text.of("untrusted"));

        fillOutGroup(container);
        allGroups.add(untrustedID);
        defaultGroups.add(untrustedID);
        allContainers.put(untrustedID, container);
    }

    public static void fillOutGroup(TrustContainer tc){

        for (Map.Entry<Identifier, PermissionSetting> entry : permissionSettings.entrySet()) {
            tc.permissionSet.put(entry.getKey(), entry.getValue().getCopy());
        }
    }

    public static void registerPermissionSetting(PermissionSetting baseSetting) {
        permissionSettings.put(baseSetting.id, baseSetting);
        permissionDisplayOrder.add(baseSetting.id);
    }

    public static TrustContainer getContainer(Identifier id) {

        if (!allContainers.containsKey(id)) {
            TrustContainer newContainer = new TrustContainer(id, Text.of(id.getPath()));

            newContainer.setParent(new Identifier("groups", "untrusted"));
            allContainers.put(id, newContainer);
            return newContainer;
        }

        if (allContainers.containsKey(id))
            return allContainers.get(id);
        return null;
    }

    public static void fromNBT(CompoundTag tag) {

    }

    public static void toNBT(CompoundTag tag) {

    }

}
