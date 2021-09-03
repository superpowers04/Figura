package net.blancworks.figura.gui;

import net.blancworks.figura.PlayerData;
import net.blancworks.figura.PlayerDataManager;
import net.blancworks.figura.gui.widgets.CustomListWidgetState;
import net.blancworks.figura.gui.widgets.PermissionListWidget;
import net.blancworks.figura.gui.widgets.PlayerListWidget;
import net.blancworks.figura.trust.PlayerTrustManager;
import net.blancworks.figura.trust.TrustContainer;
import net.blancworks.figura.trust.settings.PermissionSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.ConfirmChatLinkScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.*;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import org.lwjgl.glfw.GLFW;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.UUID;

public class FiguraTrustScreen extends Screen {

    public Screen parentScreen;

    private TextFieldWidget searchBox;
    private TextFieldWidget uuidBox;

    private int paneY;
    private int paneWidth;
    private int rightPaneX;
    private int searchBoxX;

    public PlayerListWidget playerList;
    public PermissionListWidget permissionList;
    public CustomListWidgetState playerListState = new CustomListWidgetState();
    public CustomListWidgetState permissionListState = new CustomListWidgetState();

    public ButtonWidget resetPermissionButton;
    public ButtonWidget resetAllPermissionsButton;

    public ButtonWidget clearCacheButton;

    public double pressStartX, pressStartY;
    public UUID draggedId;


    public boolean shiftPressed = false;
    public boolean altPressed = false;

    protected FiguraTrustScreen(Screen parentScreen) {
        super(new TranslatableText("gui.figura.trustmenutitle"));
        this.parentScreen = parentScreen;
    }

    @Override
    protected void init() {
        super.init();

        PlayerTrustManager.loadFromDisk();

        int width = Math.min((this.width / 2) - 10 - 128, 128);

        paneY = 48;
        paneWidth = this.width / 3 - 8;
        rightPaneX = paneWidth + 10;

        int searchBoxWidth = paneWidth - 5;
        searchBoxX = 7;
        this.searchBox = new TextFieldWidget(this.textRenderer, searchBoxX, 22, searchBoxWidth, 20, this.searchBox, new TranslatableText("gui.figura.search"));
        this.searchBox.setChangedListener((string_1) -> this.playerList.filter(string_1, false));
        this.playerList = new PlayerListWidget(this.client, paneWidth, this.height, paneY + 19, this.height - 36, 20, this.searchBox, this.playerList, this, playerListState);
        this.playerList.setLeftPos(5);
        playerList.reloadFilters();

        this.permissionList = new PermissionListWidget(
                this.client,
                this.width - rightPaneX - 5, this.height,
                paneY + 19, this.height - 36,
                24,
                null,
                this.permissionList, this, permissionListState
        );
        permissionList.setLeftPos(rightPaneX);

        this.addSelectableChild(this.playerList);
        this.addSelectableChild(this.permissionList);
        this.addSelectableChild(this.searchBox);
        this.setInitialFocus(this.searchBox);

        this.addDrawableChild(new ButtonWidget(this.width - width - 5, this.height - 20 - 5, width, 20, new TranslatableText("gui.figura.button.back"), (buttonWidgetx) -> {

            PlayerTrustManager.saveToDisk();

            this.client.openScreen(parentScreen);
        }));

        this.addDrawableChild(new ButtonWidget(this.width - width - 10 - width, this.height - 20 - 5, width, 20, new TranslatableText("gui.figura.button.help"), (buttonWidgetx) -> this.client.openScreen(new ConfirmChatLinkScreen((bl) -> {
            //Open the trust menu from the Figura Wiki
            if (bl)
                Util.getOperatingSystem().open("https://github.com/TheOneTrueZandra/Figura/wiki/Trust-Menu");
            this.client.openScreen(this);
        }, "https://github.com/TheOneTrueZandra/Figura/wiki/Trust-Menu", true))));

        this.addDrawableChild(clearCacheButton = new ButtonWidget(5, this.height - 20 - 5, 140, 20, new TranslatableText("gui.figura.button.clearall"), (buttonWidgetx) -> {
            PlayerDataManager.clearCache();
        }));

        this.addDrawableChild(new ButtonWidget(this.width - 140 - 5, 15, 140, 20, new TranslatableText("gui.figura.button.reloadavatar"), (btx) -> {

            if (playerListState.selected instanceof PlayerListEntry) {
                PlayerListEntry entry = (PlayerListEntry) playerListState.selected;

                if (entry != null) {
                    PlayerDataManager.clearPlayer(entry.getProfile().getId());
                }
            }
        }));

        resetPermissionButton = new ButtonWidget(this.width - 140 - 5, 40, 140, 20, new TranslatableText("gui.figura.button.resetperm"), (btx) -> {
            try {
                TrustContainer tc = permissionList.getCurrentContainer();

                //if a perm is selected, reset only this perm
                if (playerListState != null && permissionListState.selected != null)
                    tc.reset(((PermissionSetting) permissionListState.selected).id);
                //else reset all the entry perms
                else
                    tc.resetAll();

                permissionList.rebuild();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        });

        resetAllPermissionsButton = new ButtonWidget(this.width - 140 - 5, 40, 140, 20, new TranslatableText("gui.figura.button.resetallperm").setStyle(Style.EMPTY.withColor(TextColor.parse("red"))), (btx) -> {
            try {
                //for all entries, reset all perms
                playerList.children().forEach(customListEntry -> {
                    playerListState.selected = customListEntry.getEntryObject();
                    TrustContainer tc = permissionList.getCurrentContainer();

                    tc.resetAll();
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        resetAllPermissionsButton.visible = false;

        this.addDrawableChild(resetPermissionButton);
        this.addDrawableChild(resetAllPermissionsButton);

        this.uuidBox = new TextFieldWidget(this.textRenderer, this.width - 290, 15, 138, 18, this.uuidBox, new TranslatableText("UUID"));
        this.uuidBox.setMaxLength(36);
        /*
        this.addSelectableChild(uuidBox);

        this.addDrawableChild(new ButtonWidget(this.width - 290, 40, 140, 20, new TranslatableText("Get Avatar"), (btx) -> {
            try {
                UUID uuid = UUID.fromString(uuidBox.getText());
                PlayerDataManager.getDataForPlayer(uuid);
            } catch (Exception ignored) {}
        }));
        */

        playerList.reloadFilters();
        permissionList.rebuild();
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackgroundTexture(0);

        this.playerList.render(matrices, mouseX, mouseY, delta);
        this.permissionList.render(matrices, mouseX, mouseY, delta);
        this.searchBox.render(matrices, mouseX, mouseY, delta);
        //this.uuidBox.render(matrices, mouseX, mouseY, delta);

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
                TrustContainer trustData = data.getTrustContainer();

                if (data.model != null) {
                    int currX = paneWidth + 13;
                    //Complexity
                    {
                        int complexity = data.model.getRenderComplexity();
                        MutableText complexityText = new TranslatableText("gui.figura.complexity", complexity).setStyle(Style.EMPTY.withColor(TextColor.parse("gray")));

                        if (trustData != null) {
                            if (complexity >= trustData.getFloatSetting(PlayerTrustManager.MAX_COMPLEXITY_ID)) {
                                complexityText.setStyle(Style.EMPTY.withColor(TextColor.parse("red")));
                            }
                        }

                        drawTextWithShadow(matrices, textRenderer, complexityText, currX, 54, TextColor.parse("white").getRgb());
                        currX += textRenderer.getWidth(complexityText) + 10;
                    }

                    {
                        long size = data.model.totalSize;

                        //format file size
                        DecimalFormat df = new DecimalFormat("#0.00", new DecimalFormatSymbols(Locale.US));
                        df.setRoundingMode(RoundingMode.HALF_UP);
                        float fileSize = Float.parseFloat(df.format(size / 1000.0f));

                        MutableText sizeText = new TranslatableText("gui.figura.filesize", fileSize).setStyle(Style.EMPTY.withColor(TextColor.parse("gray")));

                        drawTextWithShadow(matrices, textRenderer, sizeText, currX, 54, TextColor.parse("white").getRgb());
                    }
                }
            }
        }

        super.render(matrices, mouseX, mouseY, delta);

        if (!resetPermissionButton.active) {
            resetPermissionButton.active = true;

            if (resetPermissionButton.isMouseOver(mouseX, mouseY)) {
                if (playerListState.selected instanceof PlayerListEntry) {
                    renderTooltip(matrices, new TranslatableText("gui.figura.button.tooltip.resetperm"), mouseX, mouseY);
                } else if (playerListState.selected instanceof Identifier) {
                    TrustContainer tc = PlayerTrustManager.getContainer((Identifier) playerListState.selected);

                    if (tc.isHidden) {
                        renderTooltip(matrices, new TranslatableText("gui.figura.button.tooltip.cantreset"), mouseX, mouseY);
                    } else {
                        renderTooltip(matrices, new TranslatableText("gui.figura.button.tooltip.resetallperm"), mouseX, mouseY);
                    }
                }
            }

            resetPermissionButton.active = false;
        }

        if (!clearCacheButton.active) {
            clearCacheButton.active = true;
            if (clearCacheButton.isMouseOver(mouseX, mouseY)) {
                renderTooltip(matrices, new TranslatableText("gui.figura.button.tooltip.clearcache"), mouseX, mouseY);
            }
            clearCacheButton.active = false;
        }


        if (draggedId != null) {
            PlayerListEntry entry = MinecraftClient.getInstance().getNetworkHandler().getPlayerListEntry(draggedId);

            if (entry == null) {
                draggedId = null;
                return;
            }

            TextRenderer tr = MinecraftClient.getInstance().textRenderer;
            Text displayText = Text.of(entry.getProfile().getName());

            drawTextWithShadow(matrices,
                    tr,
                    displayText,
                    (int) (mouseX - tr.getWidth(displayText) / 2.0f),
                    (int) (mouseY - tr.fontHeight / 2.0f),
                    TextColor.parse("white").getRgb());
        }

    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_LEFT_SHIFT)
            shiftPressed = true;
        if (keyCode == GLFW.GLFW_KEY_LEFT_ALT)
            altPressed = true;
        if (getFocused() == permissionList) {
            return permissionList.keyPressed(keyCode, scanCode, modifiers);
        }
        return super.keyPressed(keyCode, scanCode, modifiers) || this.searchBox.keyPressed(keyCode, scanCode, modifiers) || this.uuidBox.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_LEFT_SHIFT)
            shiftPressed = false;
        if (keyCode == GLFW.GLFW_KEY_LEFT_ALT)
            altPressed = false;
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char char_1, int int_1) {
        if (getFocused() == permissionList) {
            return permissionList.charTyped(char_1, int_1);
        }
        return this.searchBox.charTyped(char_1, int_1) || this.uuidBox.charTyped(char_1, int_1);
    }

    @Override
    public void onClose() {
        PlayerTrustManager.saveToDisk();
        this.client.openScreen(parentScreen);
    }

    int tickCount = 0;

    @Override
    public void tick() {

        tickCount++;

        searchBox.setTextFieldFocused(getFocused() == searchBox);
        uuidBox.setTextFieldFocused(getFocused() == uuidBox);

        if (playerListState.selected instanceof PlayerListEntry) {
            resetPermissionButton.active = shiftPressed;
        } else if (playerListState.selected instanceof Identifier) {
            TrustContainer tc = PlayerTrustManager.getContainer((Identifier) playerListState.selected);

            if (!tc.isHidden)
                resetPermissionButton.active = shiftPressed;
        } else {
            resetPermissionButton.active = false;
        }

        if (altPressed) {
            resetAllPermissionsButton.active = resetPermissionButton.active;
            resetPermissionButton.visible = false;
            resetAllPermissionsButton.visible = true;
        } else {
            resetAllPermissionsButton.visible = false;
            resetPermissionButton.visible = true;
        }

        clearCacheButton.active = shiftPressed;

        this.searchBox.tick();
        //this.uuidBox.tick();

        if (tickCount > 20) {
            tickCount = 0;
            playerList.reloadFilters();
            //permissionList.rebuild();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        //unselect perm if clicked on entry
        if (playerList.isMouseOver(mouseX, mouseY))
            permissionList.unselect();

        if (draggedId != null)
            return true;

        if (button == 0) {
            pressStartX = mouseX;
            pressStartY = mouseY;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (draggedId != null)
            return true;

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

        if (draggedId != null)
            return true;

        if (playerList.isMouseOver(mouseX, mouseY) && playerList.mouseDragged(mouseX, mouseY, button, deltaX, deltaY))
            return true;

        if (playerList.isMouseOver(mouseX, mouseY) && playerListState.selected instanceof PlayerListEntry) {
            PlayerListEntry entry = (PlayerListEntry) playerListState.selected;


            if (draggedId == null) {
                if (Math.abs(mouseX - pressStartX) + Math.abs(mouseY - pressStartY) > 2) {
                    draggedId = entry.getProfile().getId();

                    Vec2f v = playerList.getOffsetFromNearestEntry(mouseX, mouseY);

                    pressStartX = v.x;
                    pressStartY = v.y;

                    playerList.reloadFilters();
                }
            }

            return true;
        }

        if (permissionList.isMouseOver(mouseX, mouseY)) {
            return this.permissionList.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {

        if (draggedId != null) {
            if (playerList.isMouseOver(mouseX, mouseY)) {
                PlayerListWidget.PlayerListWidgetEntry listEntry = (PlayerListWidget.PlayerListWidgetEntry) playerList.getEntryAtPos(mouseX, mouseY);

                if (listEntry != null) {
                    Object obj = listEntry.getEntryObject();

                    if (obj instanceof Identifier) {
                        Identifier playerID = new Identifier("players", draggedId.toString());
                        TrustContainer tc = PlayerTrustManager.getContainer(playerID);

                        tc.setParent((Identifier) obj);
                    } else if (obj instanceof PlayerListEntry) {
                        Identifier playerID = new Identifier("players", draggedId.toString());
                        TrustContainer tc = PlayerTrustManager.getContainer(playerID);
                        Identifier droppedID = new Identifier("players", ((PlayerListEntry) obj).getProfile().getId().toString());
                        TrustContainer droppedTC = PlayerTrustManager.getContainer(droppedID);

                        tc.setParent(droppedTC.getParentIdentifier());
                    }
                }
            }
        }
        draggedId = null;


        playerList.reloadFilters();
        permissionList.rebuild();
        return false;
    }
}
