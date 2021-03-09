package net.blancworks.figura.gui.widgets;

import net.blancworks.figura.PlayerData;
import net.blancworks.figura.PlayerDataManager;
import net.blancworks.figura.gui.FiguraGuiScreen;
import net.blancworks.figura.gui.FiguraTrustScreen;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;

public class ModelFileListWidget extends CustomListWidget<PlayerListEntry, ModelFileListWidget.ModelFileListWidgetEntry> {


    public ModelFileListWidget(MinecraftClient client, int width, int height, int y1, int y2, int entryHeight, TextFieldWidget searchBox, CustomListWidget list, Screen parent, CustomListWidgetState state) {
        super(client, width, height, y1, y2, entryHeight, searchBox, list, parent, state);
    }

    @Override
    protected void doFiltering(String searchTerm) {
        super.doFiltering(searchTerm);

        File contentDirectory = FabricLoader.getInstance().getGameDir().getParent().resolve("model_files").toFile();

        ArrayList<String> valid_loads = new ArrayList<String>();

        File[] files = contentDirectory.listFiles();

        for (File file : files) {
            String fileName = FilenameUtils.removeExtension(file.getName());
            
            if(!fileName.contains(searchTerm))
                continue;

            if (Files.exists(contentDirectory.toPath().resolve(fileName + ".bbmodel")) && Files.exists(contentDirectory.toPath().resolve(fileName + ".png"))) {
                if (valid_loads.contains(fileName))
                    continue;
                valid_loads.add(fileName);
            }
        }

        for (String valid_load : valid_loads) {
            addEntry(new ModelFileListWidgetEntry(valid_load, this));
        }
    }

    @Override
    public void select(ModelFileListWidgetEntry entry) {
        super.select(entry);

        FiguraGuiScreen parent = (FiguraGuiScreen) getParent();
        
        parent.click_button(entry.getEntryObject().toString());
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
