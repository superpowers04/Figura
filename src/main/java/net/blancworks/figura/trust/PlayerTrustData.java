package net.blancworks.figura.trust;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import net.blancworks.figura.FiguraMod;
import net.blancworks.figura.gui.widgets.PermissionListWidget;
import net.blancworks.figura.trust.settings.PermissionBooleanSetting;
import net.blancworks.figura.trust.settings.PermissionFloatSetting;
import net.blancworks.figura.trust.settings.PermissionSetting;
import net.blancworks.figura.trust.settings.PermissionStringSetting;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

import java.io.FileInputStream;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class PlayerTrustData {

    public static HashMap<String, Supplier<PermissionSetting>> permissionRegistry = new HashMap<String, Supplier<PermissionSetting>>();
    public static HashMap<String, TrustPreset> allPresets = new HashMap<>();
    public static ArrayList<String> defaultPresets = new ArrayList<String>();

    public static void init() {

        //EXAMPLE CODE! Please don't remove.
        /*registerPermissionSetting("testString", () -> new PermissionFloatSetting() {{
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
        }});*/

        registerPermissionSetting("maxInitInstructions", () -> new PermissionFloatSetting() {{
            name = "maxInitInstructions";
            displayText = Text.of("Max Init Instructions");
            min = 0;
            max = 17;
            integer = true;
            allowInfinity = true;
            isSlider = true;
            multiplier = 1024;
        }});

        registerPermissionSetting("maxTickInstructions", () -> new PermissionFloatSetting() {{
            name = "maxTickInstructions";
            displayText = Text.of("Max Tick Instructions");
            min = 0;
            max = 17;
            integer = true;
            allowInfinity = true;
            isSlider = true;
            multiplier = 1024;
        }});

        registerPermissionSetting("maxRenderInstructions", () -> new PermissionFloatSetting() {{
            name = "maxRenderInstructions";
            displayText = Text.of("Max Render Instructions");
            min = 0;
            max = 33;
            integer = true;
            allowInfinity = true;
            isSlider = true;
            multiplier = 512;
        }});

        registerPermissionSetting("maxComplexity", () -> new PermissionFloatSetting() {{
            name = "maxComplexity";
            displayText = Text.of("Max Complexity");
            min = 0;
            max = (512 / 4) + 1;
            integer = true;
            allowInfinity = true;
            isSlider = true;
            multiplier = 8;
        }});

        registerPermissionSetting("preset", () -> new PermissionStringSetting() {{
            name = "preset";
            displayText = Text.of("Group");
        }});

        loadPresetsFromAssets();
    }

    public static void registerPermissionSetting(String key, Supplier<PermissionSetting> supplier) {
        permissionRegistry.put(key, supplier);
    }

    public static void loadPresetsFromAssets() {
        Path p = FabricLoader.getInstance().getModContainer("figura").get().getPath("assets").resolve("figura").resolve("presets.json");

        if (Files.exists(p)) {

            try {
                FileReader fileReader = new FileReader(p.toString());
                JsonParser parser = new JsonParser();
                JsonObject rootObject = (JsonObject) parser.parse(fileReader);

                TrustPreset base = new TrustPreset();
                fillOutPreset(base, rootObject.getAsJsonObject("base"));
                base.name = "base";

                for (Map.Entry<String, JsonElement> entry : rootObject.entrySet()) {
                    if (entry.getKey().equals("base"))
                        continue;

                    String key = entry.getKey();
                    JsonObject value = (JsonObject) entry.getValue();

                    TrustPreset preset = base.getCopy();
                    preset.name = key;

                    fillOutPreset(preset, value);
                    allPresets.put(key, preset);
                    defaultPresets.add(key);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        FiguraMod.LOGGER.debug("Loaded presets from assets");
    }

    public static void fillOutPreset(TrustPreset preset, JsonObject obj) {
        for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
            String permissionKey = entry.getKey();
            if (permissionRegistry.containsKey(permissionKey)) {
                JsonElement el = entry.getValue();
                PermissionSetting setting = permissionRegistry.get(permissionKey).get();
                setting.fromJson(el);

                preset.permissions.put(permissionKey, setting);
            }
        }
    }

    public static void moveToPreset(PlayerTrustData data, String newPreset) {
        //Do not allow anything to be set to preset "base"
        if(newPreset.equals("base"))
            return;

        if (!allPresets.containsKey(newPreset)) {
            TrustPreset genPreset = data.preset == null ? allPresets.get("base").getCopy() : data.preset.getCopy();
            allPresets.put(newPreset, genPreset);
            data.preset = genPreset;
            data.reset();
            return;
        }
        
        data.preset = allPresets.get(newPreset);
        data.reset();
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

    public float getPermissionFloat(String key) {
        PermissionSetting setting = getPermission(key);

        if ((setting instanceof PermissionFloatSetting)) {
            return ((PermissionFloatSetting) setting).value;
        }

        return 0;
    }

    public String getPermissionString(String key) {
        PermissionSetting setting = getPermission(key);

        if ((setting instanceof PermissionStringSetting)) {
            return ((PermissionStringSetting) setting).value;
        }

        return "";
    }

    public PermissionSetting getPermission(String key) {
        //Get from override permissions first
        if (permissions.containsKey(key)) {
            return permissions.get(key);
        }
        PermissionSetting copySetting = preset.permissions.get(key).getCopy(this);
        permissions.put(key, copySetting);
        return copySetting;
    }

    public void setPermission(String key, PermissionSetting setting) {
        permissions.put(key, setting);
    }
}
