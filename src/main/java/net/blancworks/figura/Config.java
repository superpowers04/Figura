package net.blancworks.figura;

import net.fabricmc.loader.api.FabricLoader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;

public class Config {
    public static final Map<String, ConfigEntry> entries = new HashMap<>();

    private static final File file = new File(FabricLoader.getInstance().getConfigDir().resolve("figura.properties").toString());

    public static void initialize() {
        setDefaults();
        loadConfig();
        saveConfig();
    }

    public static void loadConfig() {
        try {
            if(file.exists()) {
                BufferedReader br = new BufferedReader(new FileReader(file));
                String line = br.readLine();

                while (line != null) {
                    String[] content = line.split("=");

                    if (content.length >= 2 && line.charAt(0) != '#') {
                        if (entries.containsKey(content[0])) {

                            ConfigEntry entry = entries.get(content[0]);
                            try {
                                if (entry.modValue != null) {
                                    int value = Integer.parseInt(content[1]) % (int) entry.modValue;
                                    if (value < 0) value += (int) entry.modValue;

                                    entry.setValue(String.valueOf(value));
                                } else {
                                    entry.setValue(content[1]);
                                }
                            } catch (Exception e) {
                                entry.value = entry.defaultValue;
                            }
                        }
                    }
                    line = br.readLine();
                }
                br.close();
            }
        }
        catch (Exception e) {
            FiguraMod.LOGGER.warn("Failed to load config file! Generating a new one...");
            e.printStackTrace();
            setDefaults();
            saveConfig();
        }
    }

    public static void saveConfig() {
        try {
            FileWriter writer = new FileWriter(file);

            writer.write("### Displays your NameTag on the model preview screen ### - default true\n");
            writer.write("previewNameTag=" + entries.get("previewNameTag").value + "\n\n");

            writer.write("### Adds The Mark △ to the NameTag of players using Figura ### - default true\n");
            writer.write("nameTagMark=" + entries.get("nameTagMark").value + "\n\n");

            writer.write("### Toggles between using the new or old network for online models\n");
            writer.write("useNewNetwork=" + entries.get("useNewNetwork").value + "\n\n");

            writer.write("### Toggles between using a local server or the main online server for online models\n");
            writer.write("useLocalServer=" + entries.get("useLocalServer").value + "\n\n");
            
            writer.write("### Location where the Figura settings button should be ###\n");
            writer.write("### 0 - top left | 1 - top right | 2 - bottom left | 3 - bottom right | 4 - icon ### - default 3\n");
            writer.write("buttonLocation=" + entries.get("buttonLocation").value + "\n\n");

            writer.write("### Script debug log output location ###\n");
            writer.write("### 0 - console and chat | 1 - console only | 2 - chat only ### - default 0\n");
            writer.write("scriptLog=" + entries.get("scriptLog").value + "\n\n");

            writer.write("### Adds The Mark △ to the tab list of players using Figura ### - default true\n");
            writer.write("listMark=" + entries.get("listMark").value + "\n\n");

            writer.write("### Adds The Mark △ to the chat names of players using Figura ### - default true\n");
            writer.write("chatMark=" + entries.get("chatMark").value);

            writer.close();
        }
        catch (Exception e) {
            FiguraMod.LOGGER.error("Failed to save config file!");
            e.printStackTrace();
        }
    }

    public static void copyConfig() {
        entries.forEach((s, configEntry) -> configEntry.setValue(configEntry.configValue.toString()));
    }

    public static void discardConfig() {
        entries.forEach((s, configEntry) -> configEntry.configValue = configEntry.value);
    }

    public static void setDefaults() {
        entries.clear();
        entries.put("previewNameTag", new ConfigEntry<>(true));
        entries.put("nameTagMark", new ConfigEntry<>(true));
        entries.put("buttonLocation", new ConfigEntry<>(3, 5));
        entries.put("useNewNetwork", new ConfigEntry<>(false));
        entries.put("useLocalServer", new ConfigEntry<>(false));
        entries.put("scriptLog", new ConfigEntry<>(0, 3));
        entries.put("listMark", new ConfigEntry<>(true));
        entries.put("chatMark", new ConfigEntry<>(true));
    }

    public static class ConfigEntry<T> {
        public T value;
        public T defaultValue;
        public T configValue;
        public T modValue;

        public ConfigEntry(T value) {
            this(value, null);
        }

        public ConfigEntry(T value, T modValue) {
            this.value = value;
            this.defaultValue = value;
            this.configValue = value;
            this.modValue = modValue;
        }

        @SuppressWarnings("unchecked")
        private void setValue(String text) {
            try {
                if (value instanceof String)
                    value = (T) text;
                else if (value instanceof Boolean)
                    value = (T) Boolean.valueOf(text);
                else if (value instanceof Integer)
                    value = (T) Integer.valueOf(text);
                else if (value instanceof Float)
                    value = (T) Float.valueOf(text);
                else if (value instanceof Long)
                    value = (T) Long.valueOf(text);
                else if (value instanceof Double)
                    value = (T) Double.valueOf(text);
                else if (value instanceof Byte)
                    value = (T) Byte.valueOf(text);
                else if (value instanceof Short)
                    value = (T) Short.valueOf(text);
            } catch (Exception e) {
                value = defaultValue;
            }

            configValue = value;
        }
    }
}