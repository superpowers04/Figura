package net.blancworks.figura.config;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.blancworks.figura.FiguraMod;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;

public class ConfigManager {

    //accent color
    public static final Formatting ACCENT_COLOR = Formatting.AQUA;

    //mod name
    public static final String MOD_NAME = "figura";

    //mod config version
    //change this only if you edit old configs
    public static final int CONFIG_VERSION = 1;
    private static final Map<Config, String> V0_CONFIG = new HashMap<>() {{
        put(Config.PREVIEW_NAMEPLATE, "previewNameTag");
        put(Config.FIGURA_BUTTON_LOCATION, "buttonLocation");
        put(Config.USE_LOCAL_SERVER, "useLocalServer");
        put(Config.SCRIPT_LOG_LOCATION, "scriptLog");
        put(Config.PLAYERLIST_MODIFICATIONS, "listMods");
        put(Config.CHAT_MODIFICATIONS, "chatMods");
        put(Config.NAMEPLATE_MODIFICATIONS, "nameTagMods");
        put(Config.BADGE_AS_ICONS, "nameTagIcon");
        put(Config.BADGES, "showBadges");
        put(Config.RENDER_OWN_NAMEPLATE, "ownNameTag");
        put(Config.LOG_OTHERS_SCRIPT, "logOthers");
        put(Config.ACTION_WHEEL_BUTTON, "actionWheel");
        put(Config.FORMAT_SCRIPT_ON_UPLOAD, "formatScript");
        put(Config.ACTION_WHEEL_TITLE_POS, "actionWheelPos");
        put(Config.RENDER_DEBUG_PARTS_PIVOT, "partsHitBox");
    }};

    //configs!!
    public enum Config {
        PREVIEW_NAMEPLATE(true),
        FIGURA_BUTTON_LOCATION(4, 5),
        USE_LOCAL_SERVER(false),
        SCRIPT_LOG_LOCATION(0, 3),
        PLAYERLIST_MODIFICATIONS(true),
        CHAT_MODIFICATIONS(true),
        NAMEPLATE_MODIFICATIONS(true),
        BADGE_AS_ICONS(true),
        BADGES(true),
        RENDER_OWN_NAMEPLATE(false),
        LOG_OTHERS_SCRIPT(false),
        ACTION_WHEEL_BUTTON(GLFW.GLFW_KEY_B),
        FORMAT_SCRIPT_ON_UPLOAD(true),
        ACTION_WHEEL_TITLE_POS(0, 4),
        RENDER_DEBUG_PARTS_PIVOT(true),
        MODEL_FOLDER_PATH("");

        //config data
        public Object value;
        public Object defaultValue;
        public Object configValue;
        public Object modValue;

        Config(Object value, Object modValue) {
            this.value = value;
            this.defaultValue = value;
            this.configValue = value;
            this.modValue = modValue;
        }
        Config(Object value) {
            this(value, null);
        }

        public void setValue(String text) {
            try {
                if (value instanceof String)
                    value = text;
                else if (value instanceof Boolean)
                    value = Boolean.valueOf(text);
                else if (value instanceof Integer)
                    value = Integer.valueOf(text);
                else if (value instanceof Float)
                    value = Float.valueOf(text);
                else if (value instanceof Long)
                    value = Long.valueOf(text);
                else if (value instanceof Double)
                    value = Double.valueOf(text);
                else if (value instanceof Byte)
                    value = Byte.valueOf(text);
                else if (value instanceof Short)
                    value = Short.valueOf(text);

                if (modValue != null)
                    value = (Integer.parseInt(text) + (int) modValue) % (int) modValue;
            } catch (Exception e) {
                value = defaultValue;
            }

            configValue = value;
        }
    }

    private static final File FILE = new File(FabricLoader.getInstance().getConfigDir().resolve(MOD_NAME + ".json").toString());

    public static void initialize() {
        loadConfig();
        saveConfig();
    }

    public static void loadConfig() {
        try {
            if (FILE.exists()) {
                BufferedReader br = new BufferedReader(new FileReader(FILE));
                JsonObject json = new JsonParser().parse(br).getAsJsonObject();

                JsonElement version = json.get("CONFIG_VERSION");
                if (version == null || version.getAsInt() < CONFIG_VERSION) {
                    update(json, version == null ? 0 : version.getAsInt());
                }
                else {
                    for (Config config : Config.values()) {
                        JsonElement object = json.get(config.name().toLowerCase());
                        if (object == null)
                            continue;

                        String jsonValue = object.getAsString();
                        config.setValue(jsonValue);
                    }
                }

                br.close();
            }
        } catch (Exception e) {
            FiguraMod.LOGGER.warn("Failed to load config file! Generating a new one...");
            e.printStackTrace();
            setDefaults();
        }
    }

    public static void saveConfig() {
        try {
            JsonObject configJson = new JsonObject();

            for(Config config : Config.values()) {
                if (config.value instanceof Number)
                    configJson.addProperty(config.name().toLowerCase(), (Number) config.value);
                if (config.value instanceof Character)
                    configJson.addProperty(config.name().toLowerCase(), (Character) config.value);
                else if (config.value instanceof Boolean)
                    configJson.addProperty(config.name().toLowerCase(), (boolean) config.value);
                else
                    configJson.addProperty(config.name().toLowerCase(), String.valueOf(config.value));
            }
            configJson.addProperty("CONFIG_VERSION", CONFIG_VERSION);

            String jsonString = new GsonBuilder().setPrettyPrinting().create().toJson(configJson);

            FileWriter fileWriter = new FileWriter(FILE);
            fileWriter.write(jsonString);
            fileWriter.close();
        } catch (Exception e) {
            FiguraMod.LOGGER.error("Failed to save config file!");
            e.printStackTrace();
        }
    }

    public static void applyConfig() {
        for(Config config : Config.values()) {
            config.setValue(String.valueOf(config.configValue));
        }
    }

    public static void discardConfig() {
        for(Config config : Config.values()) {
            config.configValue = config.value;
        }
    }

    public static void setDefaults() {
        for(Config config : Config.values()) {
            config.value = config.defaultValue;
        }
    }

    public static void update(JsonObject json, int version) {
        Map<Config, String> versionMap = null;

        //from V0
        if (version == 0)
            versionMap = V0_CONFIG;

        if (versionMap == null)
            return;

        for (Map.Entry<Config, String> config : versionMap.entrySet()) {
            JsonElement object = json.get(config.getValue());

            if (object == null)
                continue;

            String jsonValue = object.getAsString();
            Config.valueOf(config.getKey().toString()).setValue(jsonValue);
        }
    }

    //returns true if modmenu shifts other buttons on the game menu screen
    public static boolean modmenuButton() {
        if (FabricLoader.getInstance().isModLoaded("modmenu")) {
            String buttonStyle = com.terraformersmc.modmenu.config.ModMenuConfig.MODS_BUTTON_STYLE.getValue().toString();
            return !buttonStyle.equals("SHRINK") && !buttonStyle.equals("ICON");
        }

        return false;
    }
}