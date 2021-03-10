package net.blancworks.figura.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.blancworks.figura.PlayerData;
import net.blancworks.figura.PlayerDataManager;
import net.blancworks.figura.gui.widgets.CustomListWidgetState;
import net.blancworks.figura.gui.widgets.PermissionListWidget;
import net.blancworks.figura.gui.widgets.PlayerListWidget;
import net.blancworks.figura.trust.PlayerTrustData;
import net.blancworks.figura.trust.settings.PermissionFloatSetting;
import net.blancworks.figura.trust.settings.PermissionStringSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.*;
import net.minecraft.util.math.MathHelper;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class FiguraTrustScreen extends Screen {

    public Screen parentScreen;

    private TextFieldWidget searchBox;

    private Text tooltip;
    private boolean init = false;
    private boolean filterOptionsShown = false;
    private int paneY;
    private int paneWidth;
    private int rightPaneX;
    private int searchBoxX;
    private int filtersX;
    private int filtersWidth;
    private int searchRowWidth;
    public final Set<String> showModChildren = new HashSet<>();

    public PlayerListWidget playerList;
    public PermissionListWidget permissionList;
    public CustomListWidgetState playerListState = new CustomListWidgetState();
    public CustomListWidgetState permissionListState = new CustomListWidgetState();

    public ButtonWidget resetPermissionsButton;
    public ButtonWidget clearCacheButton;


    public boolean shiftPressed = false;

    protected FiguraTrustScreen(Screen parentScreen) {
        super(new LiteralText("Figura Trust Menu"));
        this.parentScreen = parentScreen;
    }

    @Override
    protected void init() {
        super.init();

        int width = (this.width / 2) - 10 - 128;

        paneY = 48;
        paneWidth = this.width / 3 - 8;
        rightPaneX = paneWidth + 10;

        int searchBoxWidth = paneWidth - 5;
        searchBoxX = 7;
        this.searchBox = new TextFieldWidget(this.textRenderer, searchBoxX, 22, searchBoxWidth, 20, this.searchBox, new TranslatableText("modmenu.search"));
        this.searchBox.setChangedListener((string_1) -> this.playerList.filter(string_1, false));
        this.playerList = new PlayerListWidget(this.client, paneWidth, this.height, paneY + 19, this.height - 36, 14, this.searchBox, this.playerList, this, playerListState);
        this.playerList.setLeftPos(5);
        playerList.reloadFilters();

        this.permissionList = new PermissionListWidget(
                this.client,
                this.width - rightPaneX - 5, this.height,
                paneY + 19, this.height - 36
                , 24,
                null,
                this.permissionList, this, permissionListState
        );
        permissionList.setLeftPos(rightPaneX);

        this.addChild(this.playerList);
        this.addChild(this.permissionList);
        this.setInitialFocus(this.searchBox);

        this.addButton(new ButtonWidget(this.width - width - 5, this.height - 20 - 5, width, 20, new LiteralText("Back"), (buttonWidgetx) -> {
            this.client.openScreen((Screen) parentScreen);
        }));

        this.addButton(clearCacheButton = new ButtonWidget(5, this.height - 20 - 5, width, 20, new LiteralText("Clear All Avatars"), (buttonWidgetx) -> {
            PlayerDataManager.clearCache();
        }));

        this.addButton(new ButtonWidget(this.width - 100 - 5, 15, 100, 20, new LiteralText("Reload Avatar"), (btx) -> {
            PlayerListEntry entry = (PlayerListEntry) playerListState.selected;

            if (entry != null) {
                PlayerDataManager.clearPlayer(entry.getProfile().getId());
            }
        }));

        resetPermissionsButton = new ButtonWidget(this.width - 100 - 5, 40, 100, 20, new LiteralText("Reset Permissions"), (btx) -> {
            PlayerListEntry entry = (PlayerListEntry) playerListState.selected;

            if (entry != null) {
                PlayerData data = PlayerDataManager.getDataForPlayer(entry.getProfile().getId());
                PlayerTrustData trustData = PlayerDataManager.getTrustDataForPlayer(entry.getProfile().getId());

                trustData.reset();
                permissionList.rebuild();
            }
        });

        this.addButton(resetPermissionsButton);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);

        this.playerList.render(matrices, mouseX, mouseY, delta);
        this.permissionList.render(matrices, mouseX, mouseY, delta);
        this.searchBox.render(matrices, mouseX, mouseY, delta);

        if (playerListState.selected instanceof PlayerListEntry) {
            PlayerListEntry entry = (PlayerListEntry) playerListState.selected;

            Text nameText = new LiteralText(entry.getProfile().getName()).setStyle(Style.EMPTY.withColor(TextColor.parse("white")));
            Text uuidText = new LiteralText(entry.getProfile().getId().toString()).setStyle(Style.EMPTY.withColor(TextColor.parse("dark_gray")));

            drawTextWithShadow(matrices, textRenderer, nameText, paneWidth + 13, 22, TextColor.parse("white").getRgb());
            matrices.push();
            matrices.scale(0.75f, 0.75f, 0.75f);
            drawTextWithShadow(matrices, textRenderer, uuidText, MathHelper.floor((paneWidth + 13) / 0.75f), MathHelper.floor((32) / 0.75f), TextColor.parse("white").getRgb());
            matrices.pop();

            if (PlayerDataManager.hasPlayerData(entry.getProfile().getId())) {
                PlayerData data = PlayerDataManager.getDataForPlayer(entry.getProfile().getId());
                PlayerTrustData trustData = PlayerDataManager.getTrustDataForPlayer(entry.getProfile().getId());

                if (data.model != null) {
                    int currX = paneWidth + 13;
                    //Complexity
                    {
                        int complexity = data.model.getRenderComplexity();
                        MutableText complexityText = new LiteralText(String.format("Complexity:%d", complexity)).setStyle(Style.EMPTY.withColor(TextColor.parse("gray")));

                        if (trustData != null) {
                            if (complexity >= ((PermissionFloatSetting) trustData.getPermission("maxComplexity")).value) {
                                complexityText = complexityText.setStyle(Style.EMPTY.withColor(TextColor.parse("red")));
                            }
                        }

                        drawTextWithShadow(matrices, textRenderer, complexityText, currX, 54, TextColor.parse("white").getRgb());
                        currX += textRenderer.getWidth(complexityText) + 10;
                    }

                    {
                        long size = data.model.totalSize;
                        MutableText sizeText = new LiteralText(String.format("File Size:%.2fkB", size / 1000.0f)).setStyle(Style.EMPTY.withColor(TextColor.parse("gray")));

                        drawTextWithShadow(matrices, textRenderer, sizeText, currX, 54, TextColor.parse("white").getRgb());
                        currX += textRenderer.getWidth(sizeText) + 10;
                    }
                }
            }
        } else if (playerListState.selected instanceof String) {
            
        }

        super.render(matrices, mouseX, mouseY, delta);


        if (!resetPermissionsButton.active) {
            resetPermissionsButton.active = true;
            if (resetPermissionsButton.isMouseOver(mouseX, mouseY)) {
                if(playerListState.selected instanceof PlayerListEntry)
                    renderTooltip(matrices, Text.of("Hold LEFT SHIFT and press to reset the permissions for this user."), mouseX, mouseY);
                else
                    renderTooltip(matrices, Text.of("Cannot reset a Preset."), mouseX, mouseY);
            }
            resetPermissionsButton.active = false;
        }

        if (!clearCacheButton.active) {
            clearCacheButton.active = true;
            if (clearCacheButton.isMouseOver(mouseX, mouseY)) {
                renderTooltip(matrices, Text.of("Hold LEFT SHIFT and press to clear all loaded avatar data."), mouseX, mouseY);
            }
            clearCacheButton.active = false;
        }

    }

    @Override
    public void renderBackground(MatrixStack matrices) {
        overlayBackground(0, 0, this.width, this.height, 64, 64, 64, 255, 255);
    }

    static void overlayBackground(int x1, int y1, int x2, int y2, int red, int green, int blue, int startAlpha, int endAlpha) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        Objects.requireNonNull(MinecraftClient.getInstance()).getTextureManager().bindTexture(DrawableHelper.OPTIONS_BACKGROUND_TEXTURE);
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        buffer.begin(7, VertexFormats.POSITION_TEXTURE_COLOR);
        buffer.vertex(x1, y2, 0.0D).texture(x1 / 32.0F, y2 / 32.0F).color(red, green, blue, endAlpha).next();
        buffer.vertex(x2, y2, 0.0D).texture(x2 / 32.0F, y2 / 32.0F).color(red, green, blue, endAlpha).next();
        buffer.vertex(x2, y1, 0.0D).texture(x2 / 32.0F, y1 / 32.0F).color(red, green, blue, startAlpha).next();
        buffer.vertex(x1, y1, 0.0D).texture(x1 / 32.0F, y1 / 32.0F).color(red, green, blue, startAlpha).next();
        tessellator.draw();
    }

    public String getSearchInput() {
        return searchBox.getText();
    }

    private boolean updateFiltersX() {
        if ((filtersWidth + textRenderer.getWidth(computeModCountText(true)) + 20) >= searchRowWidth && ((filtersWidth + textRenderer.getWidth(computeModCountText(false)) + 20) >= searchRowWidth || (filtersWidth + 150 + 20) >= searchRowWidth)) {
            filtersX = paneWidth / 2 - filtersWidth / 2;
            return !filterOptionsShown;
        } else {
            filtersX = searchRowWidth - filtersWidth + 1;
            return true;
        }
    }

    private Text computeModCountText(boolean includeLibs) {
        return new LiteralText("DUMMY");
    }

    @Override
    public boolean keyPressed(int int_1, int int_2, int int_3) {
        shiftPressed = int_1 == 340;
        if (getFocused() == permissionList) {
            return permissionList.keyPressed(int_1, int_2, int_3);
        }
        return super.keyPressed(int_1, int_2, int_3) || this.searchBox.keyPressed(int_1, int_2, int_3);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 340)
            shiftPressed = false;
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char char_1, int int_1) {
        if (getFocused() == permissionList) {
            return permissionList.charTyped(char_1, int_1);
        }
        return this.searchBox.charTyped(char_1, int_1);
    }

    
    int tickCount = 0;
    @Override
    public void tick() {

        tickCount++;
        
        if (getFocused() == permissionList) {
            searchBox.setTextFieldFocused(false);
        } else {
            searchBox.setTextFieldFocused(true);
        }

        resetPermissionsButton.active = shiftPressed && playerListState.selected instanceof PlayerListEntry;
        clearCacheButton.active = shiftPressed;

        this.searchBox.tick();
        
        if(tickCount > 20){
            tickCount = 0;
            playerList.reloadFilters();
            permissionList.rebuild();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if(shiftPressed){
            if(permissionListState.selected instanceof PermissionStringSetting){
                if(((PermissionStringSetting) permissionListState.selected).getName().equals("preset")){
                    
                    PlayerListWidget.PlayerListWidgetEntry e = (PlayerListWidget.PlayerListWidgetEntry) playerList.getEntryAtPos(mouseX, mouseY);
                    
                    if(e instanceof PlayerListWidget.GroupListWidgetEntry){

                        PlayerTrustData.moveToPreset(((PermissionStringSetting) permissionListState.selected).parentData, ((PlayerListWidget.GroupListWidgetEntry) e).identifier);
                        playerList.reloadFilters();
                        permissionList.rebuild();
                        
                        return false;
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (permissionList.isMouseOver(mouseX, mouseY)) {
            return this.permissionList.mouseScrolled(mouseX, mouseY, amount);
        }
        if (playerList.isMouseOver(mouseX, mouseY)) {
            return this.playerList.mouseScrolled(mouseX, mouseY, amount);
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (permissionList.isMouseOver(mouseX, mouseY)) {
            return this.permissionList.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        }
        if (playerList.isMouseOver(mouseX, mouseY)) {
            return this.playerList.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        }
        return false;
    }
}
