package net.blancworks.figura;

import net.fabricmc.loader.api.FabricLoader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class Config {
    public static BooleanConfig previewNameTag;
    public static BooleanConfig nameTagMark;

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
                do {
                    String[] content = line.split("=");

                    if (content.length >= 2 && line.charAt(0) != '#') {
                        switch (content[0]) {
                            case "previewNameTag":
                                previewNameTag = new BooleanConfig(Boolean.parseBoolean(content[1]), true);
                                break;
                            case "nameTagMark":
                                nameTagMark = new BooleanConfig(Boolean.parseBoolean(content[1]), true);
                                break;
                        }
                    }
                    line = br.readLine();
                } while (line != null);

                br.close();
            }
        }
        catch (Exception e) {
            FiguraMod.LOGGER.warn("Failed to load config file! Generating a new one...");
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
            writer.write("nameTagMark=" + nameTagMark.value);

            writer.close();
        }
        catch (Exception e) {
            FiguraMod.LOGGER.error("Failed to save config file!");
            e.printStackTrace();
        }
    }

    public static void setDefaults() {
        previewNameTag = new BooleanConfig(true, true);
        nameTagMark = new BooleanConfig(true, true);
    }

    public static class BooleanConfig {
        public boolean value;
        public boolean defaultValue;

        public BooleanConfig(boolean value, boolean defaultValue) {
            this.value = value;
            this.defaultValue = defaultValue;
        }
    }
}