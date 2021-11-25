package net.blancworks.figura.gui.widgets;

import net.blancworks.figura.FiguraMod;
import net.blancworks.figura.LocalAvatarManager;
import net.blancworks.figura.LocalAvatarManager.LocalAvatar;
import net.blancworks.figura.LocalAvatarManager.LocalAvatarFolder;
import net.blancworks.figura.PlayerDataManager;
import net.blancworks.figura.gui.FiguraGuiScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;

import java.util.Map;

public class ModelFileListWidget extends CustomListWidget<PlayerListEntry, CustomListEntry> {

    public ModelFileListWidget(MinecraftClient client, int width, int height, int y1, int y2, int entryHeight, TextFieldWidget searchBox, CustomListWidget<?, ?> list, Screen parent, CustomListWidgetState<?> state) {
        super(client, width, height, y1, y2, entryHeight, searchBox, list, parent, state);
    }

    @Override
    protected void doFiltering(String searchTerm) {
        super.doFiltering(searchTerm);

        //add empty entry
        if (searchTerm.equals("")) {
            addEntry(new ModelFileListWidgetEntry("", this, LocalAvatarManager.init ? "figura.models.unselect" : "figura.models.loading", null, 0) {
                @Override
                public Text getDisplayText() {
                    return new TranslatableText(this.getName()).formatted(Formatting.ITALIC, Formatting.DARK_GRAY);
                }
            });
        }

        //add avatars
        addAvatarsToList(LocalAvatarManager.AVATARS, searchTerm.toLowerCase(), 0);
    }

    @Override
    public void select(CustomListEntry entry) {
        if (entry instanceof ModelFileListWidgetFolderEntry) {
            ModelFileListWidgetFolderEntry folder = (ModelFileListWidgetFolderEntry) entry;
            //change expanded value
            folder.expanded = !folder.expanded;

            if (folder.avatar instanceof LocalAvatarFolder)
                ((LocalAvatarFolder) folder.avatar).expanded = folder.expanded;

            //update folder list
            if (folder.expanded)
                LocalAvatarManager.FOLDER_DATA.remove(folder.getIdentifier());
            else
                LocalAvatarManager.FOLDER_DATA.put(folder.getIdentifier(), false);

            //reload and cancel selection
            this.reloadFilters();
            return;
        } else if (entry instanceof ModelFileListWidgetEntry) {
            ModelFileListWidgetEntry file = (ModelFileListWidgetEntry) entry;
            if (PlayerDataManager.localPlayer == null || !PlayerDataManager.localPlayer.isAvatarLoaded())
                return;

            FiguraGuiScreen parent = (FiguraGuiScreen) getParent();
            parent.loadLocalAvatar(file.getName(), file.getIdentifier());
        }

        super.select(entry);
    }

    private void addAvatarsToList(Map<String, LocalAvatar> list, String search, int offset) {
        if (list == null || list.isEmpty()) return;

        //add to list
        list.forEach((key, value) -> {
            if (value instanceof LocalAvatarFolder) {
                LocalAvatarFolder folder = (LocalAvatarFolder) value;
                if (hasMatchingChild(folder.children, search)) {
                    //add folder to list
                    ModelFileListWidgetFolderEntry widgetEntry = new ModelFileListWidgetFolderEntry(key, this, folder.name, value, offset, folder.expanded);
                    this.addEntry(widgetEntry);

                    //add child to list
                    if (folder.expanded)
                        addAvatarsToList(folder.children, search, offset + 1);
                }
            } else if (value.name.toLowerCase().contains(search)) {
                //add file to list if it matches the search
                this.addEntry(new ModelFileListWidgetEntry(key, this, value.name, value, offset));
            }
        });
    }

    private static boolean hasMatchingChild(Map<String, LocalAvatar> child, String search) {
        for (LocalAvatar entry : child.values()) {
            if (entry instanceof LocalAvatarFolder && hasMatchingChild(((LocalAvatarFolder) entry).children, search))
                return true;
            else if (entry.name.toLowerCase().contains(search))
                return true;
        }

        return false;
    }

    public void updateAvatarList() {
        //reload avatars
        LocalAvatarManager.loadFromDisk();
        reloadFilters();
    }

    public static class ModelFileListWidgetEntry extends CustomListEntry {
        private final String name;
        public int offset;
        public LocalAvatar avatar;

        public ModelFileListWidgetEntry(String obj, ModelFileListWidget list, String name, LocalAvatar avatar, int offset) {
            super(obj, list);
            this.name = name;
            this.offset = offset;
            this.avatar = avatar;
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
            StringBuilder space = new StringBuilder();
            for (int i = 0; i < offset; i++) {
                space.append("  ");
            }
            return new LiteralText(space + getName());
        }
    }

    public static class ModelFileListWidgetFolderEntry extends ModelFileListWidgetEntry {
        public boolean expanded;

        public ModelFileListWidgetFolderEntry(String obj, ModelFileListWidget list, String name, LocalAvatar avatar, int offset, boolean expanded) {
            super(obj, list, name, avatar, offset);
            this.expanded = expanded;
        }

        @Override
        public Text getDisplayText() {
            StringBuilder space = new StringBuilder();
            for (int i = 0; i < offset; i++) {
                space.append("  ");
            }
            return new LiteralText(space.toString()).append(new LiteralText(this.expanded ? "V " : "> ")
                    .setStyle(Style.EMPTY.withFont(FiguraMod.FIGURA_FONT)))
                    .formatted(this.expanded ? Formatting.GRAY : Formatting.DARK_GRAY)
                    .append(new LiteralText(getName()));
        }
    }
}
