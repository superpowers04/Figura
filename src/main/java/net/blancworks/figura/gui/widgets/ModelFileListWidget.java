package net.blancworks.figura.gui.widgets;

import net.blancworks.figura.FiguraMod;
import net.blancworks.figura.LocalPlayerData;
import net.blancworks.figura.PlayerDataManager;
import net.blancworks.figura.gui.FiguraGuiScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipFile;

public class ModelFileListWidget extends CustomListWidget<PlayerListEntry, CustomListEntry> {

    public final Map<String, CustomListEntry> avatars = new LinkedHashMap<>();

    public ModelFileListWidget(MinecraftClient client, int width, int height, int y1, int y2, int entryHeight, TextFieldWidget searchBox, CustomListWidget<?, ?> list, Screen parent, CustomListWidgetState<?> state) {
        super(client, width, height, y1, y2, entryHeight, searchBox, list, parent, state);
        updateAvatarList();
    }

    @Override
    protected void doFiltering(String searchTerm) {
        super.doFiltering(searchTerm);

        //add empty entry
        if (searchTerm.equals("")) {
            addEntry(new ModelFileListWidgetEntry("", this, " ") {
                @Override
                public String getName() {
                    return null;
                }

                @Override
                public Text getDisplayText() {
                    return new TranslatableText("models.figura.unselect").formatted(Formatting.ITALIC, Formatting.DARK_GRAY);
                }
            });
        }

        //add avatars
        addAvatarsToList(avatars, searchTerm, this);
    }

    private static boolean hasAvatar(File file) {
        try {
            byte load = 0;

            //zip load
            if (file.getName().endsWith(".zip")) {
                ZipFile zipFile = new ZipFile(file.getPath());

                if (zipFile.getEntry("model.bbmodel") != null) load = (byte) (load | 1);
                else if (zipFile.getEntry("player_model.bbmodel") != null) load = (byte) (load | 2);
                else if (zipFile.getEntry("script.lua") != null) load = (byte) (load | 4);
            }
            //directory load
            else if (file.isDirectory()) {
                if (Files.exists(file.toPath().resolve("model.bbmodel"))) load = (byte) (load | 1);
                else if (Files.exists(file.toPath().resolve("player_model.bbmodel"))) load = (byte) (load | 2);
                else if (Files.exists(file.toPath().resolve("script.lua"))) load = (byte) (load | 4);
            }

            //add to list if valid
            if (load > 0) return true;
        } catch (Exception e) {
            FiguraMod.LOGGER.warn("Failed to load avatar " + file.getName());
            e.printStackTrace();
        }

        return false;
    }

    public static void loadAvatars(File contentDirectory, ModelFileListWidget list, Map<String, CustomListEntry> parent) {
        File[] files = contentDirectory.listFiles();

        if (files == null || parent == null)
            return;

        Map<String, CustomListEntry> newParent = new LinkedHashMap<>();

        //for all files in folder
        for (File file : files) {
            String path = file.getAbsolutePath();
            boolean added = parent.containsKey(path);

            if (!added) {
                if (hasAvatar(file)) {
                    //add file if has avatar
                    newParent.put(path, new ModelFileListWidgetEntry(path, list, file.getName()));
                } else if (file.isDirectory()) {
                    //load directory avatars
                    ModelFileListWidgetFolderEntry folder = new ModelFileListWidgetFolderEntry(path, list, file.getName());
                    loadAvatars(file, list, folder.children);

                    //do not add if dir is empty
                    if (!folder.children.isEmpty()) newParent.put(path, folder);
                }
            } else {
                CustomListEntry entry = parent.get(path);
                if (entry instanceof ModelFileListWidgetEntry) {
                    //do not add invalid loaded avatars
                    if (hasAvatar(file)) newParent.put(path, entry);
                }
                else if (entry instanceof ModelFileListWidgetFolderEntry folderEntry) {
                    //load avatars from subfolder
                    loadAvatars(file, list, folderEntry.children);

                    //do not add empty subfolder
                    if (!folderEntry.children.isEmpty()) newParent.put(path, folderEntry);
                }
            }
        }

        parent.clear();
        newParent.forEach((k, v) -> {
            if (v instanceof ModelFileListWidgetFolderEntry)
                parent.put(k, v);
        });
        newParent.forEach((k, v) -> {
            if (!(v instanceof ModelFileListWidgetFolderEntry))
                parent.put(k, v);
        });
    }

    private static void addAvatarsToList(Map<String, CustomListEntry> list, String search, ModelFileListWidget widget) {
        if (list == null) return;

        //add to list
        for (CustomListEntry entry : new ArrayList<>(list.values())) {
            if (entry instanceof ModelFileListWidgetFolderEntry folder) {
                //add folder to list
                widget.addEntry(folder);

                if (folder.expanded) {
                    //increase offset
                    folder.children.forEach((k, value) -> {
                        if (value instanceof ModelFileListWidgetEntry fileEntry)
                            fileEntry.offset = folder.offset + 1;
                        else if (value instanceof ModelFileListWidgetFolderEntry folderEntry)
                            folderEntry.offset = folder.offset + 1;
                    });

                    //add child to list
                    addAvatarsToList(folder.children, search, widget);
                }
            } else if (entry instanceof ModelFileListWidgetEntry file) {
                //add file to list if it matches the search
                if (file.getName().contains(search.toLowerCase()))
                    widget.addEntry(file);
            }
        }
    }

    public void updateAvatarList() {
        reloadFilters();

        //reload avatars
        FiguraMod.doTask(() -> {
            File contentDirectory = LocalPlayerData.getContentDirectory().toFile();

            try {
                Files.createDirectories(contentDirectory.toPath());
                ModelFileListWidget.loadAvatars(contentDirectory, this, this.avatars);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void select(CustomListEntry entry) {
        if (entry instanceof ModelFileListWidgetFolderEntry folder) {
            folder.expanded = !folder.expanded;
            this.reloadFilters();
            return;
        } else if (entry instanceof ModelFileListWidgetEntry file) {
            if (!PlayerDataManager.localPlayer.isAvatarLoaded())
                return;

            FiguraGuiScreen parent = (FiguraGuiScreen) getParent();
            parent.loadLocalAvatar(file.getName(), file.getIdentifier());
        }

        super.select(entry);
    }

    public static class ModelFileListWidgetEntry extends CustomListEntry {

        private final String name;
        public int offset = 0;

        public ModelFileListWidgetEntry(String obj, CustomListWidget<?, ?> list, String name) {
            super(obj, list);
            this.name = name;
        }

        public String getName() {
            return this.name;
        }

        @Override
        public String getIdentifier() {
            return getEntryObject().toString();
        }

        @Override
        public Text getDisplayText() {
            return new LiteralText("  ".repeat(offset) + getName());
        }
    }

    public static class ModelFileListWidgetFolderEntry extends CustomListEntry {

        private final Map<String, CustomListEntry> children = new LinkedHashMap<>();
        public boolean expanded = true;

        private final String name;
        public int offset = 0;

        public ModelFileListWidgetFolderEntry(String obj, CustomListWidget<?, ?> list, String name) {
            super(obj, list);
            this.name = name;
        }

        public String getName() {
            return this.name;
        }

        @Override
        public String getIdentifier() {
            return getEntryObject().toString();
        }

        @Override
        public Text getDisplayText() {
            return new LiteralText("  ".repeat(offset)).append(new LiteralText(this.expanded ? "V " : "> ")
                    .setStyle(Style.EMPTY.withFont(FiguraMod.FIGURA_FONT)))
                    .setStyle(Style.EMPTY.withColor(TextColor.parse(this.expanded ? "gray" : "dark_gray")))
                    .append(new LiteralText(getName()));
        }
    }
}
