package net.blancworks.figura;

import net.fabricmc.loader.api.FabricLoader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class Config {
    public static ConfigEntry<Boolean> previewNameTag;
    public static ConfigEntry<Boolean> nameTagMark;
    public static ConfigEntry<Integer> buttonLocation;
    public static ConfigEntry<Boolean> useNewNetwork;
    public static ConfigEntry<Boolean> useLocalServer;
    public static ConfigEntry<Integer> scriptLog;
    public static ConfigEntry<Boolean> listMark;
    public static ConfigEntry<Boolean> chatMark;

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
                        switch (content[0]) {
                            case "previewNameTag":
                                previewNameTag = new ConfigEntry<>(Boolean.parseBoolean(content[1]), true);
                                break;
                            case "nameTagMark":
                                nameTagMark = new ConfigEntry<>(Boolean.parseBoolean(content[1]), true);
                                break;
                            case "useNewNetwork":
                                useNewNetwork = new ConfigEntry<>(Boolean.parseBoolean(content[1]), true);
                                break;
                            case "useLocalServer":
                                useLocalServer = new ConfigEntry<>(Boolean.parseBoolean(content[1]), true);
                                break;
                            case "buttonLocation":
                                int i = Integer.parseInt(content[1]) % 5;
                                if (i < 0) i += 4;
                                buttonLocation = new ConfigEntry<>(i, 3);
                                break;
                            case "scriptLog":
                                int j = Integer.parseInt(content[1]) % 3;
                                if (j < 0) j += 2;
                                scriptLog = new ConfigEntry<>(j, 0);
                                break;
                            case "listMark":
                                listMark = new ConfigEntry<>(Boolean.parseBoolean(content[1]), true);
                                break;
                            case "chatMark":
                                chatMark = new ConfigEntry<>(Boolean.parseBoolean(content[1]), true);
                                break;
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
            writer.write("previewNameTag=" + previewNameTag.value + "\n\n");

            writer.write("### Adds The Mark △ to the NameTag of players using Figura ### - default true\n");
            writer.write("nameTagMark=" + nameTagMark.value + "\n\n");

            writer.write("### Toggles between using the new or old network for online models\n");
            writer.write("useNewNetwork=" + useNewNetwork.value + "\n\n");

            writer.write("### Toggles between using a local server or the main online server for online models\n");
            writer.write("useLocalServer=" + useLocalServer.value + "\n\n");
            
            writer.write("### Location where the Figura settings button should be ###\n");
            writer.write("### 0 - top left | 1 - top right | 2 - bottom left | 3 - bottom right | 4 - icon ### - default 3\n");
            writer.write("buttonLocation=" + buttonLocation.value + "\n\n");

            writer.write("### Script debug log output location ###\n");
            writer.write("### 0 - console and chat | 1 - console only | 2 - chat only ### - default 0\n");
            writer.write("scriptLog=" + scriptLog.value + "\n\n");

            writer.write("### Adds The Mark △ to the tab list of players using Figura ### - default true\n");
            writer.write("listMark=" + listMark.value + "\n\n");

            writer.write("### Adds The Mark △ to the chat names of players using Figura ### - default true\n");
            writer.write("chatMark=" + chatMark.value);

            writer.close();
        }
        catch (Exception e) {
            FiguraMod.LOGGER.error("Failed to save config file!");
            e.printStackTrace();
        }
    }

    public static void setDefaults() {
        previewNameTag = new ConfigEntry<>(true);
        nameTagMark = new ConfigEntry<>(true);
        buttonLocation = new ConfigEntry<>(3);
        useNewNetwork = new ConfigEntry<>(false);
        useLocalServer = new ConfigEntry<>(false);
        scriptLog = new ConfigEntry<>(0);
        listMark = new ConfigEntry<>(true);
        chatMark = new ConfigEntry<>(true);
    }

    public static class ConfigEntry<T> {
        public T value;
        public T defaultValue;

        public ConfigEntry(T value, T defaultValue) {
            this.value = value;
            this.defaultValue = defaultValue;
        }

        public ConfigEntry(T value) {
            this.value = value;
            this.defaultValue = value;
        }
    }
}