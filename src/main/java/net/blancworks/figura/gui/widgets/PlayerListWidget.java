package net.blancworks.figura.gui.widgets;

import com.mojang.blaze3d.systems.RenderSystem;
import net.blancworks.figura.FiguraMod;
import net.blancworks.figura.avatar.AvatarData;
import net.blancworks.figura.avatar.AvatarDataManager;
import net.blancworks.figura.gui.FiguraTrustScreen;
import net.blancworks.figura.lua.api.nameplate.NamePlateAPI;
import net.blancworks.figura.mixin.PlayerListHudAccessorMixin;
import net.blancworks.figura.trust.PlayerTrustManager;
import net.blancworks.figura.trust.TrustContainer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.ToggleButtonWidget;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PlayerListWidget extends CustomListWidget<PlayerListEntry, PlayerListWidget.PlayerListWidgetEntry> {

    public static final Identifier lockTextureID = new Identifier("figura", "textures/gui/lock.png");

    public PlayerListWidget(MinecraftClient client, int width, int height, int y1, int y2, int entryHeight, TextFieldWidget searchBox, CustomListWidget<?, ?> list, Screen parent, CustomListWidgetState<?> state) {
        super(client, width, height, y1, y2, entryHeight, searchBox, list, parent, state);
    }

    @Override
    protected void doFiltering(String searchTerm) {
        super.doFiltering(searchTerm);
        FiguraTrustScreen realScreen = (FiguraTrustScreen) getParent();

        //Foreach player
        ArrayList<PlayerListEntry> players = new ArrayList<>();
        ArrayList<PlayerListEntry> figuraPlayers = new ArrayList<>();
        List<String> addedPlayers = new ArrayList<>();

        List<PlayerListEntry> orderedPlayerList = PlayerListHudAccessorMixin.getEntryOrdering().sortedCopy(client.getNetworkHandler() == null ? new ArrayList<>() : client.getNetworkHandler().getPlayerList());
        for (PlayerListEntry listEntry : orderedPlayerList) {
            String name = listEntry.getProfile().getName().toLowerCase();
            if (addedPlayers.contains(name))
                continue;

            AvatarData data = AvatarDataManager.getDataForPlayer(listEntry.getProfile().getId());
            if (data == null || !data.hasAvatar()) {
                players.add(listEntry);
            } else {
                figuraPlayers.add(listEntry);
            }

            addedPlayers.add(name);
        }

        figuraPlayers.addAll(players);

        for (Map.Entry<Identifier, TrustContainer> entry : PlayerTrustManager.groups.entrySet()) {
            addEntry(new GroupListWidgetEntry(entry.getKey(), this) {{
                identifier = entry.getKey().toString();
                Text text = new TranslatableText("figura.trust." + entry.getKey().getPath());
                displayText = new LiteralText("").append(new LiteralText(entry.getValue().expanded ? "V " : "> ")
                        .setStyle(Style.EMPTY.withFont(FiguraMod.FIGURA_FONT)))
                        .formatted(entry.getValue().expanded ? Formatting.GRAY : Formatting.DARK_GRAY)
                        .append(text);
            }});

            if (!entry.getValue().expanded) continue;

            for (PlayerListEntry listEntry : figuraPlayers) {
                String name = listEntry.getProfile().getName();
                if (!name.toLowerCase().contains(searchTerm.toLowerCase()) || listEntry.getProfile().getId() == realScreen.draggedId || name.equals(""))
                    continue;

                //Get trust container for that player
                TrustContainer container = PlayerTrustManager.getContainer(new Identifier("player", listEntry.getProfile().getId().toString()));
                TrustContainer parent = PlayerTrustManager.getContainer(container.getParent());

                if (parent.equals(entry.getValue()))
                    addEntry(new PlayerListWidgetEntry(listEntry, this));
            }
        }
    }

    @Override
    public void select(PlayerListWidgetEntry entry) {
        if (entry instanceof GroupListWidgetEntry) {
            if (state.selected == entry.entryValue) {
                TrustContainer tc = PlayerTrustManager.getContainer((Identifier) state.selected);

                tc.expanded = !tc.expanded;

                reloadFilters();
                return;
            }
        }

        super.select(entry);

        ((FiguraTrustScreen) getParent()).permissionList.rebuild();
    }

    public PlayerListWidgetEntry getEntry(UUID id) {
        for (CustomListEntry customListEntry : this.children()) {
            if (customListEntry instanceof PlayerListWidgetEntry player && !(player instanceof GroupListWidgetEntry)) {
                UUID playerId = UUID.fromString(player.getIdentifier());

                if (playerId.compareTo(id) == 0) {
                    return player;
                }
            }
        }

        return null;
    }

    public static class PlayerListWidgetEntry extends CustomListEntry {

        public ToggleButtonWidget toggleButton;

        public PlayerListWidgetEntry(Object obj, CustomListWidget<?, ?> list) {
            super(obj, list);

            Identifier id;

            if (obj instanceof PlayerListEntry)
                id = new Identifier("player", ((PlayerListEntry) obj).getProfile().getId().toString());
            else 
                id = (Identifier) obj;

            TrustContainer tc = PlayerTrustManager.getContainer(id);

            toggleButton = new ToggleButtonWidget(0, 0, 16, 16, !tc.locked) {
                @Override
                public void onClick(double mouseX, double mouseY) {
                    tc.locked = tc.name.equals("local") || tc.name.equals(MinecraftClient.getInstance().player.getUuid().toString()) || !tc.locked;
                    toggled = !tc.locked;

                    FiguraTrustScreen trustScreen = (FiguraTrustScreen) list.getParent();
                    trustScreen.permissionList.rebuild();
                }

                @Override
                public void renderButton(MatrixStack matrices, int mouseX, int mouseY, float delta) {
                    RenderSystem.setShader(GameRenderer::getPositionTexShader);
                    RenderSystem.setShaderTexture(0, this.texture);
                    RenderSystem.disableDepthTest();

                    int i = this.u;
                    int j = this.v;
                    if (this.toggled) {
                        i += this.pressedUOffset;
                    }

                    if (this.isHovered()) {
                        j += this.hoverVOffset;
                    }

                    matrices.push();
                    drawTexture(matrices, this.x, this.y, i, j, this.width, this.height, 32, 32);
                    matrices.pop();
                    RenderSystem.enableDepthTest();
                }
            };

            toggleButton.setTextureUV(0, 0, 16, 16, lockTextureID);
        }

        @Override
        public String getIdentifier() {
            PlayerListEntry entry = (PlayerListEntry) getEntryObject();
            return entry.getProfile().getId().toString();
        }

        @Override
        public Text getDisplayText() {
            PlayerListEntry entry = (PlayerListEntry) getEntryObject();
            MutableText name;
            if (entry.getProfile().getId().compareTo(MinecraftClient.getInstance().player.getUuid()) == 0)
                name = new LiteralText("  [").styled(FiguraMod.ACCENT_COLOR).append(new TranslatableText("figura.trust.local")).append("] " + entry.getProfile().getName());
            else
                name = new LiteralText("  " + entry.getProfile().getName());

            Text badges = NamePlateAPI.getBadges(AvatarDataManager.getDataForPlayer(entry.getProfile().getId()));
            if (badges != null) name.append(badges);
            return name;
        }

        @Override
        public void mouseMoved(double mouseX, double mouseY) {
            toggleButton.mouseMoved(mouseX, mouseY);
            super.mouseMoved(mouseX, mouseY);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (toggleButton.isMouseOver(mouseX, mouseY))
                return toggleButton.mouseClicked(mouseX, mouseY, button);
            return super.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public void render(MatrixStack matrices, int index, int y, int x, int rowWidth, int rowHeight, int mouseX, int mouseY, boolean isSelected, float delta) {
            super.render(matrices, index, y, x, rowWidth, rowHeight, mouseX, mouseY, isSelected, delta);

            toggleButton.x = (x + rowWidth) - 16;
            toggleButton.y = y;
            toggleButton.render(matrices, mouseX, mouseY, delta);
        }
    }

    public static class GroupListWidgetEntry extends PlayerListWidgetEntry {
        public String identifier;
        public Text displayText;

        public GroupListWidgetEntry(Object obj, CustomListWidget<?, ?> list) {
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
