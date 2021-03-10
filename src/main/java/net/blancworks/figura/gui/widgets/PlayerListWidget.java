package net.blancworks.figura.gui.widgets;

import net.blancworks.figura.FiguraMod;
import net.blancworks.figura.PlayerData;
import net.blancworks.figura.PlayerDataManager;
import net.blancworks.figura.gui.FiguraTrustScreen;
import net.blancworks.figura.trust.PlayerTrustData;
import net.blancworks.figura.trust.TrustPreset;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;

import java.lang.reflect.Array;
import java.security.acl.Group;
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

        HashMap<String, ArrayList<PlayerListEntry>> sortedEntries = new HashMap<String, ArrayList<PlayerListEntry>>();

        for (String preset : PlayerTrustData.defaultPresets) {
            sortedEntries.put(preset, new ArrayList<>());
        }
        
        for (PlayerListEntry listEntry : client.getNetworkHandler().getPlayerList()) {
            if (listEntry.getProfile().getName().contains(searchTerm)) {

                PlayerTrustData trustData = PlayerDataManager.getTrustDataForPlayer(listEntry.getProfile().getId());
                String groupName = trustData.getPermissionString("preset");

                if (!sortedEntries.containsKey(groupName)) {
                    sortedEntries.put(groupName, new ArrayList<PlayerListEntry>());
                }

                ArrayList<PlayerListEntry> list = sortedEntries.get(groupName);
                list.add(listEntry);
            }
        }
        
        for (Map.Entry<String, ArrayList<PlayerListEntry>> stringArrayListEntry : sortedEntries.entrySet()) {

            String key = stringArrayListEntry.getKey();
            ArrayList<PlayerListEntry> list = stringArrayListEntry.getValue();

            TrustPreset preset = PlayerTrustData.allPresets.get(key);
            
            if(preset.displayList) {
                addEntry(new GroupListWidgetEntry(key, this) {{
                    identifier = key;
                    displayText = new LiteralText(key).setStyle(Style.EMPTY.withColor(TextColor.parse("gray")));
                }});

                for (PlayerListEntry playerListEntry : list) {
                    addEntry(new PlayerListWidgetEntry(playerListEntry, this));
                }
            } else {
                addEntry(new GroupListWidgetEntry(key, this) {{
                    identifier = key;
                    displayText = new LiteralText(key).setStyle(Style.EMPTY.withColor(TextColor.parse("dark_gray")));
                }});
            }
        }

    }

    @Override
    public void select(PlayerListWidgetEntry entry) {

        if(entry instanceof GroupListWidgetEntry){
            if(state.selected == entry.entryValue){
                TrustPreset preset = PlayerTrustData.allPresets.get(state.selected.toString());
                
                preset.displayList = !preset.displayList;
                
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
