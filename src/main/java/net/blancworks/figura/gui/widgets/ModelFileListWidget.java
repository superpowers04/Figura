package net.blancworks.figura.gui.widgets;

import net.blancworks.figura.FiguraMod;
import net.blancworks.figura.LocalPlayerData;
import net.blancworks.figura.gui.FiguraGuiScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.nio.file.Files;
import java.util.zip.ZipFile;

public class ModelFileListWidget extends CustomListWidget<PlayerListEntry, ModelFileListWidget.ModelFileListWidgetEntry> {

    public ModelFileListWidget(MinecraftClient client, int width, int height, int y1, int y2, int entryHeight, TextFieldWidget searchBox, CustomListWidget list, Screen parent, CustomListWidgetState state) {
        super(client, width, height, y1, y2, entryHeight, searchBox, list, parent, state);
    }

    @Override
    protected void doFiltering(String searchTerm) {
        super.doFiltering(searchTerm);

        File contentDirectory = LocalPlayerData.getContentDirectory().toFile();

        try {
            Files.createDirectories(contentDirectory.toPath());
        } catch (Exception e) {
            e.printStackTrace();
        }

        File[] files = contentDirectory.listFiles();
        if (files == null) return;

        for (File file : files) {
            //get file name
            String fileName = FilenameUtils.removeExtension(file.getName());

            //skip files
            if (!fileName.toLowerCase().contains(searchTerm.toLowerCase()))
                continue;

            try {
                byte load = 0;

                //zip load
                if (file.getName().endsWith(".zip")) {
                    ZipFile zipFile = new ZipFile(file.getPath());

                    if (zipFile.getEntry("model.bbmodel") != null) load = (byte) (load | 1);
                    if (zipFile.getEntry("player_model.bbmodel") != null) load = (byte) (load | 2);
                    if (zipFile.getEntry("texture.png") != null) load = (byte) (load | 4);
                    if (zipFile.getEntry("script.lua") != null) load = (byte) (load | 8);
                }
                //directory load
                else if (file.isDirectory()) {
                    if (Files.exists(file.toPath().resolve("model.bbmodel"))) load = (byte) (load | 1);
                    if (Files.exists(file.toPath().resolve("player_model.bbmodel"))) load = (byte) (load | 2);
                    if (Files.exists(file.toPath().resolve("texture.png"))) load = (byte) (load | 4);
                    if (Files.exists(file.toPath().resolve("script.lua"))) load = (byte) (load | 8);
                }

                //add to list if valid
                if (load != 0 && load != 4)
                    addEntry(new ModelFileListWidgetEntry(file.getName(), this));
            } catch (Exception e) {
                FiguraMod.LOGGER.warn("Failed to load model " + file.getName());
                e.printStackTrace();
            }
        }
    }

    @Override
    public void select(ModelFileListWidgetEntry entry) {
        super.select(entry);

        FiguraGuiScreen parent = (FiguraGuiScreen) getParent();

        parent.clickButton(entry.getEntryObject().toString());
    }

    public class ModelFileListWidgetEntry extends CustomListEntry {

        public ModelFileListWidgetEntry(String obj, CustomListWidget list) {
            super(obj, list);
        }

        @Override
        public String getIdentifier() {
            return getEntryObject().toString();
        }

        @Override
        public Text getDisplayText() {
            return new LiteralText(getEntryObject().toString());
        }
    }
}
