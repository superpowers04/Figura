package net.blancworks.figura.gui.widgets;

import net.blancworks.figura.gui.FiguraTrustScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

public class PlayerListWidget extends CustomListWidget<PlayerListEntry, PlayerListWidget.PlayerListWidgetEntry> {

    public PlayerListWidget(MinecraftClient client, int width, int height, int y1, int y2, int entryHeight, TextFieldWidget searchBox, CustomListWidget list, Screen parent, CustomListWidgetState state) {
        super(client, width, height, y1, y2, entryHeight, searchBox, list, parent, state);
    }

    @Override
    protected void doFiltering(String searchTerm) {
        super.doFiltering(searchTerm);

        for (PlayerListEntry listEntry : client.getNetworkHandler().getPlayerList()) {
            if (listEntry.getProfile().getName().contains(searchTerm)) {
                addEntry(new PlayerListWidgetEntry(listEntry, this));
            }
        }
    }

    public class PlayerListWidgetEntry extends CustomListEntry {

        public PlayerListWidgetEntry(PlayerListEntry obj, CustomListWidget list) {
            super(obj, list);
        }

        @Override
        public String getIdentifier() {
            PlayerListEntry entry = (PlayerListEntry) getEntryObject();
            return entry.getProfile().getId().toString();
        }

        @Override
        public Text getDisplayText() {
            PlayerListEntry entry = (PlayerListEntry) getEntryObject();
            return new LiteralText(entry.getProfile().getName());
        }
    }
}
