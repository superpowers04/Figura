package net.blancworks.figura.gui.widgets;

import net.blancworks.figura.gui.FiguraTrustScreen;
import net.blancworks.figura.trust.PlayerTrustManager;
import net.blancworks.figura.trust.TrustContainer;
import net.blancworks.figura.trust.TrustPreset;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Identifier;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class PlayerListWidget extends CustomListWidget<PlayerListEntry, PlayerListWidget.PlayerListWidgetEntry> {

    public PlayerListWidget(MinecraftClient client, int width, int height, int y1, int y2, int entryHeight, TextFieldWidget searchBox, CustomListWidget list, Screen parent, CustomListWidgetState state) {
        super(client, width, height, y1, y2, entryHeight, searchBox, list, parent, state);
    }

    @Override
    protected void doFiltering(String searchTerm) {
        super.doFiltering(searchTerm);

        HashMap<Identifier, ArrayList<PlayerListEntry>> sortedEntries = new HashMap<Identifier, ArrayList<PlayerListEntry>>();
        ArrayList<Identifier> sortedEntriesOrdered = new ArrayList<Identifier>();

        for (Identifier preset : PlayerTrustManager.defaultGroups) {
            sortedEntries.put(preset, new ArrayList<>());
            sortedEntriesOrdered.add(preset);
        }

        //Foreach player
        for (PlayerListEntry listEntry : client.getNetworkHandler().getPlayerList()) {
            if (listEntry.getProfile().getName().contains(searchTerm)) {

                //Get trust container for that player
                TrustContainer container = PlayerTrustManager.getContainer(new Identifier("players", listEntry.getProfile().getId().toString()));
                Identifier groupName = container.getParentIdentifier();

                //Create sorting group if need be
                if (!sortedEntries.containsKey(groupName)) {
                    sortedEntries.put(groupName, new ArrayList<PlayerListEntry>());
                    sortedEntriesOrdered.add(groupName);
                }

                //Add to sorting group
                ArrayList<PlayerListEntry> list = sortedEntries.get(groupName);
                list.add(listEntry);
            }
        }

        //For all the sorted entries
        for (Identifier id : sortedEntriesOrdered) {
            TrustContainer tc = PlayerTrustManager.getContainer(id);
            ArrayList<PlayerListEntry> list = sortedEntries.get(id);

            if(tc.displayChildren) {
                addEntry(new GroupListWidgetEntry(id, this) {{
                    identifier = id.toString();
                    displayText = new LiteralText(id.getPath()).setStyle(Style.EMPTY.withColor(TextColor.parse("gray")));
                }});

                for (PlayerListEntry playerListEntry : list) {
                    addEntry(new PlayerListWidgetEntry(playerListEntry, this));
                }
            } else {
                addEntry(new GroupListWidgetEntry(id, this) {{
                    identifier = id.toString();
                    displayText = new LiteralText(id.getPath()).setStyle(Style.EMPTY.withColor(TextColor.parse("dark_gray")));
                }});
            }
        }

    }

    @Override
    public void select(PlayerListWidgetEntry entry) {

        if(entry instanceof GroupListWidgetEntry){
            if(state.selected == entry.entryValue){
                TrustContainer tc = PlayerTrustManager.getContainer((Identifier) state.selected);

                tc.displayChildren = !tc.displayChildren;
                
                reloadFilters();
                return;
            }
        }
        
        super.select(entry);
        
        ((FiguraTrustScreen) getParent()).permissionList.rebuild();
    }
    
    public class PlayerListWidgetEntry extends CustomListEntry {

        public PlayerListWidgetEntry(Object obj, CustomListWidget list) {
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
            return new LiteralText("  " + entry.getProfile().getName());
        }
    }

    public class GroupListWidgetEntry extends PlayerListWidgetEntry {

        public String identifier;
        public Text displayText;

        public GroupListWidgetEntry(Object obj, CustomListWidget list) {
            super(obj, list);
        }

        @Override
        public String getIdentifier() {
            return identifier;
        }

        @Override
        public Text getDisplayText() {
            return displayText;
        }
    }
}
