package net.blancworks.figura.gui;

import net.blancworks.figura.PlayerData;
import net.blancworks.figura.PlayerDataManager;
import net.blancworks.figura.gui.widgets.CustomListWidgetState;
import net.blancworks.figura.gui.widgets.CustomTextFieldWidget;
import net.blancworks.figura.gui.widgets.PermissionListWidget;
import net.blancworks.figura.gui.widgets.PlayerListWidget;
import net.blancworks.figura.lua.api.nameplate.NamePlateAPI;
import net.blancworks.figura.lua.api.nameplate.NamePlateCustomization;
import net.blancworks.figura.trust.PlayerTrustManager;
import net.blancworks.figura.trust.TrustContainer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.ConfirmChatLinkScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
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

    private CustomTextFieldWidget searchBox;
    private CustomTextFieldWidget uuidBox;

    private int paneY;
    private int paneWidth;
    private int rightPaneX;
    private int searchBoxX;

    public PlayerListWidget playerList;
    public PermissionListWidget permissionList;
    public CustomListWidgetState<Object> playerListState = new CustomListWidgetState<>();
    public CustomListWidgetState<Object> permissionListState = new CustomListWidgetState<>();

    public ButtonWidget resetPermissionButton;
    public ButtonWidget resetAllPermissionsButton;

    public ButtonWidget clearCacheButton;

    public double pressStartX, pressStartY;
    public UUID draggedId;


    public boolean shiftPressed = false;
    public boolean altPressed = false;

    protected FiguraTrustScreen(Screen parentScreen) {
        super(new TranslatableText("figura.gui.trustmenu.title"));
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
        this.searchBox = new CustomTextFieldWidget(this.textRenderer, searchBoxX, 22, searchBoxWidth, 20, this.searchBox, new TranslatableText("figura.gui.button.search").formatted(Formatting.ITALIC));
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

        this.addChild(this.playerList);
        this.addChild(this.permissionList);
        this.addChild(this.searchBox);
        this.setInitialFocus(this.searchBox);

        this.addButton(new ButtonWidget(this.width - width - 5, this.height - 20 - 5, width, 20, new TranslatableText("figura.gui.button.back"), (buttonWidgetx) -> {

            PlayerTrustManager.saveToDisk();

            this.client.openScreen(parentScreen);
        }));

        this.addButton(new ButtonWidget(this.width - width - 10 - width, this.height - 20 - 5, width, 20, new TranslatableText("figura.gui.button.help"), (buttonWidgetx) -> this.client.openScreen(new ConfirmChatLinkScreen((bl) -> {
            //Open the trust menu from the Figura Wiki
            if (bl)
                Util.getOperatingSystem().open("https://github.com/Blancworks/Figura/wiki/Trust-Menu");
            this.client.openScreen(this);
        }, "https://github.com/Blancworks/Figura/wiki/Trust-Menu", true))));

        this.addButton(clearCacheButton = new ButtonWidget(5, this.height - 20 - 5, 140, 20, new TranslatableText("figura.gui.button.clearall"), (buttonWidgetx) -> PlayerDataManager.clearCache()));

        this.addButton(new ButtonWidget(this.width - 140 - 5, 15, 140, 20, new TranslatableText("figura.gui.button.reloadavatar"), (btx) -> {
            if (playerListState.selected instanceof PlayerListEntry) {
                PlayerDataManager.clearPlayer(((PlayerListEntry) playerListState.selected).getProfile().getId());
            }
        }));

        resetPermissionButton = new ButtonWidget(this.width - 140 - 5, 40, 140, 20, new TranslatableText("figura.gui.button.resetperm"), (btx) -> {
            try {
                TrustContainer tc = permissionList.getCurrentContainer();

                //if a perm is selected, reset only this perm
                if (playerListState != null && permissionListState.selected != null)
                    tc.resetTrust((TrustContainer.Trust) permissionListState.selected);
                //else reset all the entry perms
                else
                    tc.resetAllTrust();

                permissionList.rebuild();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        });

        resetAllPermissionsButton = new ButtonWidget(this.width - 140 - 5, 40, 140, 20, new TranslatableText("figura.gui.button.resetallperm").formatted(Formatting.RED), (btx) -> {
            try {
                //for all entries, reset all perms
                playerList.children().forEach(customListEntry -> {
                    playerListState.selected = customListEntry.getEntryObject();
                    TrustContainer tc = permissionList.getCurrentContainer();
                    tc.resetAllTrust();
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        resetAllPermissionsButton.visible = false;

        this.addButton(resetPermissionButton);
        this.addButton(resetAllPermissionsButton);

        this.uuidBox = new CustomTextFieldWidget(this.textRenderer, this.width - 290, 15, 138, 18, this.uuidBox, new LiteralText("Name/UUID").formatted(Formatting.ITALIC));
        this.uuidBox.setMaxLength(36);
        /*
        this.addChild(uuidBox);

        this.addButton(new ButtonWidget(this.width - 290, 40, 140, 20, new TranslatableText("*yoink*"), (btx) -> {
            try {
                com.mojang.authlib.GameProfile gameProfile;
                try {
                    gameProfile = new com.mojang.authlib.GameProfile(UUID.fromString(uuidBox.getText()), "");
                } catch (Exception ignored) {
                    gameProfile = new com.mojang.authlib.GameProfile(null, uuidBox.getText());
                }

                net.minecraft.block.entity.SkullBlockEntity.loadProperties(gameProfile, profile -> {
                    PlayerData data = PlayerDataManager.getDataForPlayer(profile.getId());

                    if (data != null && data.hasAvatar() && PlayerDataManager.localPlayer != null) {
                        net.minecraft.nbt.NbtCompound nbt = new net.minecraft.nbt.NbtCompound();
                        data.writeNbt(nbt);
                        nbt.putUuid("id", PlayerDataManager.localPlayer.playerId);
                        PlayerDataManager.localPlayer.loadFromNbt(nbt);
                        PlayerDataManager.localPlayer.isLocalAvatar = true;

                        net.blancworks.figura.FiguraMod.sendToast("done", "");
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
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
            UUID id = entry.getProfile().getId();
            String name = entry.getProfile().getName();

            LiteralText nameText = new LiteralText(name);
            Text uuidText = new LiteralText(id.toString()).formatted(Formatting.DARK_GRAY);

            PlayerData data = PlayerDataManager.getDataForPlayer(id);

            if (data != null && !name.equals("")) {
                NamePlateCustomization nameplateData = data.script == null ? null : data.script.nameplateCustomizations.get(NamePlateAPI.TABLIST);

                try {
                    NamePlateAPI.applyFormattingRecursive(nameText, name, nameplateData, data);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            drawTextWithShadow(matrices, textRenderer, nameText, paneWidth + 13, 22, 0xFFFFFF);
            matrices.push();
            matrices.scale(0.75f, 0.75f, 0.75f);
            drawTextWithShadow(matrices, textRenderer, uuidText, MathHelper.floor((paneWidth + 13) / 0.75f), MathHelper.floor((32) / 0.75f), 0xFFFFFF);
            matrices.pop();

            if (data != null) {
                TrustContainer trustData = data.getTrustContainer();

                //Complexity
                int currX = paneWidth + 13;
                if (data.model != null) {
                    int complexity = data.model.getRenderComplexity();
                    MutableText complexityText = new TranslatableText("figura.gui.status.complexity").formatted(Formatting.GRAY).append(" " + complexity);

                    if (trustData != null && complexity > trustData.getTrust(TrustContainer.Trust.COMPLEXITY))
                        complexityText.formatted(Formatting.RED);

                    drawTextWithShadow(matrices, textRenderer, complexityText, currX, 54, 0xFFFFFF);
                    currX += textRenderer.getWidth(complexityText) + 10;
                }

                if (data.hasAvatar()) {
                    long size = data.getFileSize();

                    //format file size
                    DecimalFormat df = new DecimalFormat("#0.00", new DecimalFormatSymbols(Locale.US));
                    df.setRoundingMode(RoundingMode.HALF_UP);
                    float fileSize = Float.parseFloat(df.format(size / 1024.0f));

                    MutableText sizeText = new TranslatableText("figura.gui.status.filesize").formatted(Formatting.GRAY).append(" " + fileSize);
                    if (size >= PlayerData.FILESIZE_LARGE_THRESHOLD)
                        sizeText.formatted(Formatting.RED);
                    else if (size >= PlayerData.FILESIZE_WARNING_THRESHOLD)
                        sizeText.formatted(Formatting.YELLOW);

                    drawTextWithShadow(matrices, textRenderer, sizeText, currX, 54, 0xFFFFFF);
                }
            }
        }

        super.render(matrices, mouseX, mouseY, delta);

        if (!resetPermissionButton.active) {
            resetPermissionButton.active = true;

            if (resetPermissionButton.isMouseOver(mouseX, mouseY)) {
                if (playerListState.selected instanceof PlayerListEntry) {
                    renderTooltip(matrices, new TranslatableText("figura.gui.button.resetperm.tooltip"), mouseX, mouseY);
                } else if (playerListState.selected instanceof Identifier) {
                    TrustContainer tc = PlayerTrustManager.getContainer((Identifier) playerListState.selected);

                    if (tc != null) {
                        renderTooltip(matrices, new TranslatableText("figura.gui.button.cantreset.tooltip"), mouseX, mouseY);
                    } else {
                        renderTooltip(matrices, new TranslatableText("figura.gui.button.resetallperm.tooltip"), mouseX, mouseY);
                    }
                }
            }

            resetPermissionButton.active = false;
        }

        if (!clearCacheButton.active) {
            clearCacheButton.active = true;
            if (clearCacheButton.isMouseOver(mouseX, mouseY)) {
                renderTooltip(matrices, new TranslatableText("figura.gui.button.clearcache.tooltip"), mouseX, mouseY);
            }
            clearCacheButton.active = false;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (draggedId != null && client.getNetworkHandler() != null) {
            PlayerListEntry entry = client.getNetworkHandler().getPlayerListEntry(draggedId);

            if (entry == null) {
                draggedId = null;
                return;
            }

            TextRenderer tr = client.textRenderer;
            Text displayText = Text.of(entry.getProfile().getName());

            drawTextWithShadow(matrices,
                    tr,
                    displayText,
                    (int) (mouseX - tr.getWidth(displayText) / 2.0f),
                    (int) (mouseY - tr.fontHeight / 2.0f),
                    0xFFFFFF);
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
            if (tc != null)
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
                        Identifier id = (Identifier) obj;
                        Identifier playerID = new Identifier("player", draggedId.toString());
                        TrustContainer tc = PlayerTrustManager.getContainer(playerID);

                        if (tc != null && (!id.getPath().equals("local") || draggedId.compareTo(MinecraftClient.getInstance().player.getUuid()) == 0))
                            tc.parentID = id;
                    } else if (obj instanceof PlayerListEntry) {
                        Identifier playerID = new Identifier("player", draggedId.toString());
                        TrustContainer tc = PlayerTrustManager.getContainer(playerID);

                        Identifier droppedID = new Identifier("player", ((PlayerListEntry) obj).getProfile().getId().toString());
                        TrustContainer droppedTC = PlayerTrustManager.getContainer(droppedID);

                        if (tc != null && droppedTC != null && !droppedTC.parentID.getPath().equals("local")) tc.parentID = droppedTC.parentID;
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
