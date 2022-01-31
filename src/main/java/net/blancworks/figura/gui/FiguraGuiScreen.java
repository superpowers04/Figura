package net.blancworks.figura.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.blancworks.figura.FiguraMod;
import net.blancworks.figura.avatar.AvatarData;
import net.blancworks.figura.avatar.AvatarDataManager;
import net.blancworks.figura.avatar.LocalAvatarData;
import net.blancworks.figura.avatar.LocalAvatarManager;
import net.blancworks.figura.config.ConfigManager.Config;
import net.blancworks.figura.config.ConfigScreen;
import net.blancworks.figura.gui.widgets.CustomListWidgetState;
import net.blancworks.figura.gui.widgets.CustomTextFieldWidget;
import net.blancworks.figura.gui.widgets.ModelFileListWidget;
import net.blancworks.figura.gui.widgets.TexturedButtonWidget;
import net.blancworks.figura.network.NewFiguraNetworkManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ConfirmChatLinkScreen;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.Quaternion;
import net.minecraft.util.math.Vec3f;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FiguraGuiScreen extends Screen {

    public Screen parentScreen;

    public Identifier uploadTexture = new Identifier("figura", "textures/gui/upload.png");
    public Identifier reloadTexture = new Identifier("figura", "textures/gui/reload.png");
    public Identifier deleteTexture = new Identifier("figura", "textures/gui/delete.png");
    public Identifier expandTexture = new Identifier("figura", "textures/gui/expand.png");
    public Identifier keybindsTexture = new Identifier("figura", "textures/gui/keybinds.png");
    public Identifier soundsTexture = new Identifier("figura", "textures/gui/sounds.png");
    public Identifier playerBackgroundTexture = new Identifier("figura", "textures/gui/player_background.png");
    public Identifier expandedBackgroundTexture = new Identifier("figura", "textures/gui/expanded_background.png");

    public static final List<Style> TEXT_COLORS = List.of(
            Style.EMPTY.withColor(Formatting.WHITE),
            Style.EMPTY.withColor(Formatting.RED),
            Style.EMPTY.withColor(Formatting.YELLOW),
            Style.EMPTY.withColor(Formatting.GREEN)
    );

    public static final List<Text> DELETE_TOOLTIP = List.of(
        new TranslatableText("figura.gui.button.deleteavatar.tooltip").setStyle(TEXT_COLORS.get(1)),
        new TranslatableText("figura.gui.button.deleteavatartwo.tooltip").setStyle(TEXT_COLORS.get(1))
    );

    public static final TranslatableText UPLOAD_TOOLTIP = new TranslatableText("figura.gui.button.upload.tooltip");
    public static final List<Text> UPLOAD_LOCAL_TOOLTIP = List.of(
        new TranslatableText("figura.gui.button.uploadlocal.tooltip").setStyle(TEXT_COLORS.get(1)),
        new TranslatableText("figura.gui.button.uploadlocaltwo.tooltip").setStyle(TEXT_COLORS.get(1))
    );

    public static final List<Text> NO_CONNECTION_TOOLTIP = List.of(
            new TranslatableText("figura.gui.button.noconnection.tooltip").setStyle(TEXT_COLORS.get(1)),
            new TranslatableText("figura.gui.button.noconnectiontwo.tooltip").setStyle(TEXT_COLORS.get(1))
    );

    public static final TranslatableText MODEL_STATUS_TEXT = new TranslatableText("figura.gui.status.model");
    public static final TranslatableText TEXTURE_STATUS_TEXT = new TranslatableText("figura.gui.status.texture");
    public static final TranslatableText SCRIPT_STATUS_TEXT = new TranslatableText("figura.gui.status.script");
    public static final TranslatableText BACKEND_STATUS_TEXT = new TranslatableText("figura.gui.status.backend");

    public static final List<MutableText> STATUS_INDICATORS = List.of(
            new LiteralText("-").setStyle(Style.EMPTY.withFont(FiguraMod.FIGURA_FONT)),
            new LiteralText("*").setStyle(Style.EMPTY.withFont(FiguraMod.FIGURA_FONT)),
            new LiteralText("/").setStyle(Style.EMPTY.withFont(FiguraMod.FIGURA_FONT)),
            new LiteralText("+").setStyle(Style.EMPTY.withFont(FiguraMod.FIGURA_FONT))
    );

    public static final Text RELOAD_TOOLTIP = new TranslatableText("figura.gui.button.reloadavatar.tooltip");
    public static final Text KEYBIND_TOOLTIP = new TranslatableText("figura.gui.button.keybinds.tooltip");
    public static final Text SOUND_TOOLTIP = new TranslatableText("figura.gui.button.sounds.tooltip");
    public static final Text MODEL_FOLDER_TOOLTIP = new TranslatableText("figura.gui.button.openfolder.tooltip");

    private static final int[] OvO = {265, 265, 264, 264, 263, 262, 263, 262, 66, 65, 257};
    private static int ovo = 0;

    public TexturedButtonWidget uploadButton;
    public TexturedButtonWidget reloadButton;
    public TexturedButtonWidget deleteButton;
    public TexturedButtonWidget expandButton;
    public TexturedButtonWidget keybindsButton;
    public TexturedButtonWidget soundsButton;

    public ButtonWidget openFolderButton;
    public ButtonWidget exportNbt;
    public ButtonWidget serializeAvatar;

    public MutableText nameText;
    public MutableText fileSizeText;
    public MutableText modelComplexityText;

    private int modelSizeStatus = 0;
    private int textureStatus = 0;
    private int scriptStatus = 0;
    private int connectionStatus = 0;

    private CustomTextFieldWidget searchBox;
    private int paneY;
    private int paneWidth;
    private int searchBoxX;

    private boolean isHoldingShift = false;

    //gui sizes
    private static int guiScale, modelBgSize, modelSize;
    private static float screenScale;

    //model properties
    private boolean canRotate;
    private boolean canDrag;
    private static boolean expand;

    private float anchorX, anchorY;
    private float anchorAngleX, anchorAngleY;
    private float angleX, angleY;

    private float scaledValue;
    private final float SCALE_FACTOR = 1.1F;

    private int modelX, modelY;
    private float dragDeltaX, dragDeltaY;
    private float dragAnchorX, dragAnchorY;

    //model nameplate
    public static boolean showOwnNametag = false;
    public static boolean renderFireOverlay = true;

    public FiguraTrustScreen trustScreen = new FiguraTrustScreen(this);
    public FiguraKeyBindsScreen keyBindsScreen = new FiguraKeyBindsScreen(this);
    public FiguraSoundScreen soundsScreen = new FiguraSoundScreen(this);
    public ConfigScreen configScreen = new ConfigScreen(this);

    public CustomListWidgetState<Object> modelFileListState = new CustomListWidgetState<>();
    public ModelFileListWidget modelFileList;

    public FiguraGuiScreen(Screen parentScreen) {
        super(new TranslatableText("figura.gui.menu.title"));
        this.parentScreen = parentScreen;

        //reset model settings
        canRotate = false;
        canDrag = false;
        expand = false;
        resetModelPos();
    }

    @Override
    protected void init() {
        super.init();

        //screen size
        guiScale = (int) this.client.getWindow().getScaleFactor();
        screenScale = (float) (Math.min(this.width, this.height) / 1018.0);

        //model size
        modelBgSize = Math.min((int) ((512 / guiScale) * (screenScale * guiScale)), 258);
        modelSize = Math.min((int) ((192 / guiScale) * (screenScale * guiScale)), 96);

        //search box and model list
        paneY = 48;
        paneWidth = this.width / 3 - 8;

        int searchBoxWidth = paneWidth - 5;
        searchBoxX = 7;
        this.searchBox = new CustomTextFieldWidget(this.textRenderer, searchBoxX, 22, searchBoxWidth, 20, this.searchBox, new TranslatableText("figura.gui.button.search").formatted(Formatting.ITALIC));
        this.searchBox.setChangedListener((string_1) -> modelFileList.filter(string_1, false));
        this.modelFileList = new ModelFileListWidget(this.client, paneWidth, this.height, paneY + 19, this.height - 36, 20, this.searchBox, this.modelFileList, this, modelFileListState);
        this.modelFileList.setLeftPos(5);
        this.addSelectableChild(this.modelFileList);
        this.addSelectableChild(this.searchBox);

        int width = Math.min(this.width - (this.width / 2 + modelBgSize / 2 + 38), 140);

        //open folder
        openFolderButton = new ButtonWidget(5, this.height - 20 - 5, 140, 20, new TranslatableText("figura.gui.button.openfolder"), (buttonWidgetx) -> {
            Path modelDir = LocalAvatarData.getContentDirectory();
            try {
                if (isHoldingShift && AvatarDataManager.localPlayerPath != null) {
                    String path = AvatarDataManager.localPlayerPath;
                    modelDir = Path.of(path);

                    if (path.endsWith(".zip") || path.endsWith(".moon"))
                        modelDir = modelDir.getParent();
                }

                if (!Files.exists(modelDir))
                    Files.createDirectory(modelDir);
                Util.getOperatingSystem().open(modelDir.toUri());
            } catch (Exception e) {
                FiguraMod.LOGGER.error(e.toString());
            }
        });
        this.addDrawableChild(openFolderButton);

        //save model
        serializeAvatar = new ButtonWidget(this.width - width - 5, this.height - 75, width, 20, new TranslatableText("figura.gui.button.save"), (buttonWidgetx) -> {
            AvatarData local = AvatarDataManager.localPlayer;
            if (local != null && local.hasAvatar()) {
                net.minecraft.nbt.NbtCompound nbt = new net.minecraft.nbt.NbtCompound();
                local.writeNbt(nbt);
                String result = net.blancworks.figura.parsers.FiguraAvatarSerializer.serialize(nbt);

                if (result == null) FiguraMod.sendToast(new TranslatableText("figura.gui.button.save.error"), new TranslatableText("figura.gui.button.save.error.message"));
                else FiguraMod.sendToast(new TranslatableText("figura.gui.button.save.done"), result);
            }
        });

        //export nbt
        exportNbt = new ButtonWidget(this.width - width - 5, this.height - 50, width, 20, new TranslatableText("figura.gui.button.cache"), (buttonWidgetx) -> {
            if (AvatarDataManager.localPlayer != null) {
                AvatarDataManager.localPlayer.saveToCache();
                FiguraMod.sendToast(new TranslatableText("figura.gui.button.cache.done"), "");
            }
        });

        this.addDrawableChild(serializeAvatar);
        this.addDrawableChild(exportNbt);

        //back button
        this.addDrawableChild(new ButtonWidget(this.width - 145, this.height - 25, 140, 20, new TranslatableText("figura.gui.button.back"), (buttonWidgetx) -> {
            this.client.setScreen(parentScreen);
            LocalAvatarManager.saveFolderNbt();
        }));

        //trust button
        this.addDrawableChild(new ButtonWidget(this.width - width - 5, 15, width, 20, new TranslatableText("figura.gui.button.trustmenu"), (buttonWidgetx) -> this.client.setScreen(trustScreen)));

        //config button
        this.addDrawableChild(new ButtonWidget(this.width - width - 5, 40, width, 20, new TranslatableText("figura.gui.button.configmenu"), (buttonWidgetx) -> this.client.setScreen(configScreen)));

        //help button
        this.addDrawableChild(new ButtonWidget(this.width - width - 5, 65, width, 20, new TranslatableText("figura.gui.button.help"), (buttonWidgetx) -> this.client.setScreen(new ConfirmChatLinkScreen((bl) -> {
            if (bl) {
                Util.getOperatingSystem().open("https://github.com/Blancworks/Figura/wiki/Figura-Panel");
            }
            this.client.setScreen(this);
        }, "https://github.com/Blancworks/Figura/wiki/Figura-Panel", true))));

        //keybinds button
        keybindsButton = new TexturedButtonWidget(
                this.width - width - 30, 15,
                20, 20,
                0, 0, 20,
                keybindsTexture, 40, 40,
                (bx) -> this.client.setScreen(keyBindsScreen)
        );
        this.addDrawableChild(keybindsButton);
        keybindsButton.active = false;

        //sounds button
        soundsButton = new TexturedButtonWidget(
                this.width - width - 55, 15,
                20, 20,
                0, 0, 20,
                soundsTexture, 40, 40,
                (bx) -> this.client.setScreen(soundsScreen)
        );
        this.addDrawableChild(soundsButton);
        soundsButton.active = false;

        //delete button
        deleteButton = new TexturedButtonWidget(
                this.width / 2 + modelBgSize / 2 + 4, this.height / 2 - modelBgSize / 2,
                25, 25,
                0, 0, 25,
                deleteTexture, 50, 50,
                (bx) -> {
                    if (isHoldingShift)
                        FiguraMod.networkManager.deleteAvatar();
                }
        );
        this.addDrawableChild(deleteButton);
        deleteButton.active = false;

        //upload button
        uploadButton = new TexturedButtonWidget(
                this.width / 2 + modelBgSize / 2 + 4, this.height / 2 + modelBgSize / 2 - 25,
                25, 25,
                0, 0, 25,
                uploadTexture, 50, 50,
                (bx) -> FiguraMod.networkManager.postAvatar().thenRun(() -> System.out.println("UPLOADED AVATAR"))
        );
        this.addDrawableChild(uploadButton);

        //reload local button
        reloadButton = new TexturedButtonWidget(
                this.width / 2 + modelBgSize / 2 + 4, this.height / 2 + modelBgSize / 2 - 25 - 30,
                25, 25,
                0, 0, 25,
                reloadTexture, 25, 50,
                (bx) -> AvatarDataManager.clearLocalPlayer()
        );
        this.addDrawableChild(reloadButton);

        //expand button
        expandButton = new TexturedButtonWidget(
                Math.max(this.width / 2 - modelBgSize / 2, paneWidth + 5), this.height / 2 - modelBgSize / 2 - 15,
                15, 15,
                0, 0, 15,
                expandTexture, 30, 30,
                (bx) -> {
                    expand = !expand;
                    updateExpand();
                }
        );
        this.addDrawableChild(expandButton);

        //init updates
        LocalAvatarManager.loadFolderNbt();
        modelFileList.updateAvatarList();
        updateAvatarData();
        updateExpand();
    }

    @Override
    public void onClose() {
        this.client.setScreen(parentScreen);
        LocalAvatarManager.saveFolderNbt();
    }

    @Override
    public void tick() {
        super.tick();

        connectionStatus = NewFiguraNetworkManager.connectionStatus;
        if (FiguraMod.ticksElapsed % 20 == 0) {
            //update avatar list
            modelFileList.updateAvatarList();

            //reload data
            updateAvatarData();
        }
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        if (!expand) {
            //draw background
            this.renderBackgroundTexture(0);

            //draw player preview
            RenderSystem.setShaderTexture(0, playerBackgroundTexture);
            drawTexture(matrices, this.width / 2 - modelBgSize / 2, this.height / 2 - modelBgSize / 2, 0, 0, modelBgSize, modelBgSize, modelBgSize, modelBgSize);
        }
        else {
            //draw background
            RenderSystem.setShaderTexture(0, expandedBackgroundTexture);
            this.renderAsBackground(0);

            //render expand button 3:
            this.expandButton.render(matrices, mouseX, mouseY, delta);
        }
        drawEntity(modelX, modelY, (int) (modelSize + scaledValue), angleX, angleY, client.player);

        if (expand) return;

        //draw search box and file list
        modelFileList.render(matrices, mouseX, mouseY, delta);
        searchBox.render(matrices, mouseX, mouseY, delta);

        //draw status indicators
        Text statusText = new LiteralText("").append(STATUS_INDICATORS.get(modelSizeStatus)).append("  ").append(STATUS_INDICATORS.get(textureStatus)).append("  ").append(STATUS_INDICATORS.get(scriptStatus)).append("  ").append(STATUS_INDICATORS.get(connectionStatus));
        drawTextWithShadow(matrices, this.textRenderer, statusText, this.width - 75, 89, 0xFFFFFF);

        //draw text
        int currY = 90;
        if (nameText != null)
            drawTextWithShadow(matrices, this.textRenderer, nameText, this.width - this.textRenderer.getWidth(nameText) - 8, currY += 12, 0xFFFFFF);
        if (fileSizeText != null)
            drawTextWithShadow(matrices, this.textRenderer, fileSizeText, this.width - this.textRenderer.getWidth(fileSizeText) - 8, currY += 12, 0xFFFFFF);
        if (modelComplexityText != null)
            drawTextWithShadow(matrices, this.textRenderer, modelComplexityText, this.width - this.textRenderer.getWidth(modelComplexityText) - 8, currY + 12, 0xFFFFFF);

        //draw buttons
        super.render(matrices, mouseX, mouseY, delta);

        //mod version / warning
        if (AvatarDataManager.panic) {
            Text panic = new LiteralText("").append(new TranslatableText("figura.gui.panic.warning").formatted(Formatting.YELLOW)).append(new LiteralText(" =").setStyle(Style.EMPTY.withFont(FiguraMod.FIGURA_FONT)));
            drawCenteredText(matrices, this.textRenderer, panic, this.width / 2, this.height - 12, 0xFFFFFF);
        }
        else if (FiguraMod.latestVersionStatus == 0) {
            Text version = new LiteralText("Figura " + FiguraMod.MOD_VERSION).formatted(Formatting.DARK_GRAY, Formatting.ITALIC);
            drawCenteredText(matrices, this.textRenderer, version, this.width / 2, this.height - 12, 0xFFFFFF);
        } else if (FiguraMod.latestVersionStatus < 0) {
            Text version = new LiteralText("").append(new LiteralText("Figura " + FiguraMod.MOD_VERSION).formatted(Formatting.YELLOW, Formatting.ITALIC)).append(new LiteralText(" =").setStyle(Style.EMPTY.withFont(FiguraMod.FIGURA_FONT)));
            drawCenteredText(matrices, this.textRenderer, version, this.width / 2, this.height - 12, 0xFFFFFF);

            //status tooltip
            int textWidth = this.textRenderer.getWidth(version);
            if (mouseX >= this.width / 2 - textWidth / 2 && mouseX < this.width / 2 + textWidth / 2 && mouseY >= this.height - 12 && mouseY < this.height - 1) {
                List<Text> tooltipText = List.of(
                        new LiteralText("").append(new TranslatableText("figura.gui.newver.tooltip")).append(" ").append(new LiteralText(FiguraMod.latestVersion).formatted(Formatting.YELLOW, Formatting.UNDERLINE)),
                        new TranslatableText("figura.gui.newver.tooltip2")
                );
                matrices.push();
                matrices.translate(0, 0, 599);
                renderTooltip(matrices, tooltipText, mouseX, mouseY);
                matrices.pop();
            }
        } else {
            String load = Integer.toHexString(Math.abs(FiguraMod.ticksElapsed) % 16);
            Text version = new LiteralText("Figura " + FiguraMod.MOD_VERSION).formatted(Formatting.DARK_PURPLE, Formatting.ITALIC);
            drawCenteredText(matrices, this.textRenderer, version, this.width / 2, this.height - 12, 0xFFFFFF);

            //status tooltip
            int textWidth = this.textRenderer.getWidth(version);
            if (mouseX >= this.width / 2 - textWidth / 2 && mouseX < this.width / 2 + textWidth / 2 && mouseY >= this.height - 12 && mouseY < this.height - 1) {
                List<Text> tooltipText = List.of(
                        new LiteralText("Are you a time traveller?").formatted(Formatting.LIGHT_PURPLE),
                        new LiteralText("Latest version is: ").formatted(Formatting.LIGHT_PURPLE).append(new LiteralText(FiguraMod.latestVersion).formatted(Formatting.AQUA, Formatting.UNDERLINE))
                );
                matrices.push();
                matrices.translate(0, 0, 599);
                renderTooltip(matrices, tooltipText, mouseX, mouseY);
                matrices.pop();
            }
        }

        //tooltips
        AvatarData local = AvatarDataManager.localPlayer;
        boolean hasBackend = connectionStatus == 3;
        boolean hasLoadedAvatar = local != null && local.hasAvatar() && local.isAvatarLoaded();

        exportNbt.active = hasLoadedAvatar;
        serializeAvatar.active = hasLoadedAvatar;
        uploadButton.active = hasBackend && hasLoadedAvatar && local.isLocalAvatar;

        boolean wasUploadActive = uploadButton.active;
        uploadButton.active = true;
        if (uploadButton.isMouseOver(mouseX, mouseY)) {
            matrices.push();
            matrices.translate(0, 0, 599);

            if (wasUploadActive)
                renderTooltip(matrices, UPLOAD_TOOLTIP, mouseX, mouseY);
            else
                renderTooltip(matrices, hasBackend ? UPLOAD_LOCAL_TOOLTIP : NO_CONNECTION_TOOLTIP, mouseX, mouseY);

            matrices.pop();
        }
        uploadButton.active = wasUploadActive;

        if (reloadButton.isMouseOver(mouseX, mouseY)){
            matrices.push();
            matrices.translate(0, 0, 599);
            renderTooltip(matrices, RELOAD_TOOLTIP, mouseX, mouseY);
            matrices.pop();
        }

        if (!isHoldingShift && openFolderButton.isMouseOver(mouseX, mouseY)){
            matrices.push();
            matrices.translate(0, 0, 599);
            renderTooltip(matrices, MODEL_FOLDER_TOOLTIP, mouseX, mouseY);
            matrices.pop();
        }

        //status tooltip
        if (mouseY >= 88 && mouseY < 99) {
            List<Text> tooltip = null;

            //model
            if (mouseX >= this.width - 77 && mouseX < this.width - 61) {
                tooltip = List.of(
                        MODEL_STATUS_TEXT,
                        new LiteralText("").append(STATUS_INDICATORS.get(modelSizeStatus)).append(" ").append(new TranslatableText("figura.gui.button.status.model." + modelSizeStatus).setStyle(TEXT_COLORS.get(modelSizeStatus)))
                );
            }
            //texture
            else if (mouseX >= this.width - 58 && mouseX < this.width - 42) {
                tooltip = List.of(
                        TEXTURE_STATUS_TEXT,
                        new LiteralText("").append(STATUS_INDICATORS.get(textureStatus)).append(" ").append(new TranslatableText("figura.gui.button.status.texture." + textureStatus).setStyle(TEXT_COLORS.get(textureStatus)))
                );
            }
            //script
            else if (mouseX >= this.width - 39 && mouseX < this.width - 23) {
                tooltip = List.of(
                        SCRIPT_STATUS_TEXT,
                        new LiteralText("").append(STATUS_INDICATORS.get(scriptStatus)).append(" ").append(new TranslatableText("figura.gui.button.status.script." + scriptStatus).setStyle(TEXT_COLORS.get(scriptStatus)))
                );
            }
            //backend
            else if (mouseX >= this.width - 20 && mouseX < this.width - 4) {
                tooltip = List.of(
                        BACKEND_STATUS_TEXT,
                        new LiteralText("").append(STATUS_INDICATORS.get(connectionStatus)).append(" ").append(new TranslatableText("figura.gui.button.status.backend." + connectionStatus).setStyle(TEXT_COLORS.get(connectionStatus)))
                );
            }

            //render
            if (tooltip != null) {
                matrices.push();
                matrices.translate(0, 0, 599);
                renderTooltip(matrices, tooltip, mouseX, mouseY);
                matrices.pop();
            }
        }

        keybindsButton.active = AvatarDataManager.localPlayer != null && AvatarDataManager.localPlayer.script != null;

        boolean wasKeybindsActive = keybindsButton.active;
        keybindsButton.active = true;
        if (keybindsButton.isMouseOver(mouseX, mouseY)) {
            matrices.push();
            matrices.translate(0, 0, 599);
            renderTooltip(matrices, KEYBIND_TOOLTIP, mouseX, mouseY);
            matrices.pop();
        }
        keybindsButton.active = wasKeybindsActive;

        soundsButton.active = AvatarDataManager.localPlayer != null && AvatarDataManager.localPlayer.script != null;

        boolean wasSoundsActive = soundsButton.active;
        soundsButton.active = true;
        if (soundsButton.isMouseOver(mouseX, mouseY)) {
            matrices.push();
            matrices.translate(0, 0, 599);
            renderTooltip(matrices, SOUND_TOOLTIP, mouseX, mouseY);
            matrices.pop();
        }
        soundsButton.active = wasSoundsActive;

        if (!deleteButton.active) {
            deleteButton.active = true;
            boolean mouseOver = deleteButton.isMouseOver(mouseX, mouseY);
            deleteButton.active = false;

            if (mouseOver) {
                matrices.push();
                matrices.translate(0, 0, 599);
                renderTooltip(matrices, hasBackend ? DELETE_TOOLTIP : NO_CONNECTION_TOOLTIP, mouseX, mouseY);
                matrices.pop();
            }
        }
    }

    public void renderAsBackground(int vOffset) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.getBuffer();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        bufferBuilder.vertex(0f, this.height, 0f).texture(0f, this.height / 32f + vOffset).color(255, 255, 255, 255).next();
        bufferBuilder.vertex(this.width, this.height, 0f).texture(this.width / 32f, this.height / 32f + vOffset).color(255, 255, 255, 255).next();
        bufferBuilder.vertex(this.width, 0f, 0f).texture(this.width / 32f, vOffset).color(255, 255, 255, 255).next();
        bufferBuilder.vertex(0f, 0f, 0f).texture(0f, vOffset).color(255, 255, 255, 255).next();
        tessellator.draw();
    }

    public void loadLocalAvatar(Object stuff) {
        AvatarDataManager.localPlayer.isLocalAvatar = true;

        if (stuff instanceof String str)
            AvatarDataManager.localPlayer.loadModelFile(str);
        else if (stuff instanceof NbtCompound nbt)
            AvatarDataManager.localPlayer.loadFromNbt(nbt);
    }

    public void updateAvatarData() {
        if (AvatarDataManager.localPlayer != null && AvatarDataManager.localPlayer.hasAvatar()) {
            if (AvatarDataManager.localPlayer.loadedName != null) {
                nameText = new TranslatableText("figura.gui.status.name");
                int maxWidth = this.width / 2 - modelBgSize / 2 - 41 - this.textRenderer.getWidth(nameText);
                String toTrim = " " + AvatarDataManager.localPlayer.loadedName;

                if (this.textRenderer.getWidth(toTrim) > maxWidth)
                    toTrim = this.textRenderer.trimToWidth(toTrim, maxWidth - this.textRenderer.getWidth("...")) + "...";

                nameText.append(new LiteralText(toTrim).styled(FiguraMod.ACCENT_COLOR));
            } else {
                nameText = null;
            }

            if (AvatarDataManager.localPlayer.model != null) {
                modelComplexityText = new TranslatableText("figura.gui.status.complexity").append(new LiteralText(" " + AvatarDataManager.localPlayer.getComplexity()).styled(FiguraMod.ACCENT_COLOR));
            }
            else {
                modelComplexityText = new TranslatableText("figura.gui.status.complexity").append(new LiteralText(" " + 0).styled(FiguraMod.ACCENT_COLOR));
                modelSizeStatus = 0;
            }

            if (AvatarDataManager.localPlayer.hasAvatar()) {
                FiguraMod.doTask(() -> fileSizeText = getFileSizeText());
            }

            scriptStatus = AvatarDataManager.localPlayer.script != null ? AvatarDataManager.localPlayer.script.scriptError ? 1 : 3 : 0;
            textureStatus = AvatarDataManager.localPlayer.texture != null ? 3 : 0;
        } else {
            nameText = null;
            modelComplexityText = null;
            fileSizeText = null;

            textureStatus = 0;
            modelSizeStatus = 0;
            scriptStatus = 0;
        }

        connectionStatus = NewFiguraNetworkManager.connectionStatus;
    }

    public MutableText getFileSizeText() {
        long fileSize = AvatarDataManager.localPlayer.getFileSize();

        //format file size
        DecimalFormat df = new DecimalFormat("#0.00", new DecimalFormatSymbols(Locale.US));
        df.setRoundingMode(RoundingMode.HALF_UP);
        float size = Float.parseFloat(df.format(fileSize / 1000.0f));

        MutableText fsText = new TranslatableText("figura.gui.status.filesize").append(new LiteralText(" " + size).styled(FiguraMod.ACCENT_COLOR));

        if (fileSize >= AvatarData.FILESIZE_LARGE_THRESHOLD) {
            fsText.setStyle(TEXT_COLORS.get(1));
            modelSizeStatus = 1;
        }
        else if (fileSize >= AvatarData.FILESIZE_WARNING_THRESHOLD) {
            fsText.setStyle(TEXT_COLORS.get(2));
            modelSizeStatus = 2;
        }
        else {
            fsText.setStyle(TEXT_COLORS.get(0));
            modelSizeStatus = 3;
        }

        modelSizeStatus = AvatarDataManager.localPlayer.model != null ? modelSizeStatus : 0;

        return fsText;
    }

    public void updateExpand() {
        if (expand) {
            this.children().forEach(child -> {
                if (child instanceof ClickableWidget widget)
                    widget.visible = false;
            });

            expandButton.setPos(5, 5);
            expandButton.setUV(0, 0);
            expandButton.visible = true;

            modelFileList.updateSize(0, 0, this.height, 0);
        } else {
            this.children().forEach(child -> {
                if (child instanceof ClickableWidget widget)
                    widget.visible = true;
            });

            expandButton.setPos(Math.max(this.width / 2 - modelBgSize / 2, paneWidth + 5), this.height / 2 - modelBgSize / 2 + 1);
            expandButton.setUV(15, 0);

            modelFileList.updateSize(paneWidth, this.height, paneY + 19, this.height - 36);
            modelFileList.setLeftPos(5);

            scaledValue  = 0f;
        }

        modelX = this.width / 2;
        modelY = this.height / 2;
    }

    public void resetModelPos() {
        anchorX = 0.0F;
        anchorY = 0.0F;
        anchorAngleX = 0.0F;
        anchorAngleY = 0.0F;
        angleX = -15.0F;
        angleY = 30.0F;
        scaledValue = 0.0F;
        modelX = this.width / 2;
        modelY = this.height / 2;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        switch (button) {
            //left click - rotate
            case 0:
                //set anchor rotation
                if ((mouseX >= this.width / 2.0 - modelBgSize / 2.0 && mouseX <= this.width / 2.0 + modelBgSize / 2.0 &&
                        mouseY >= this.height / 2.0 - modelBgSize / 2.0 && mouseY <= this.height / 2.0 + modelBgSize / 2.0) || expand) {
                    //get starter mouse pos
                    anchorX = (float) mouseX;
                    anchorY = (float) mouseY;

                    //get starter rotation angles
                    anchorAngleX = angleX;
                    anchorAngleY = angleY;

                    //enable rotate
                    canRotate = true;
                }
                break;

            //right click - move
            case 1:
                //get starter mouse pos
                dragDeltaX = (float) mouseX;
                dragDeltaY = (float) mouseY;

                //also get start node pos
                dragAnchorX = modelX;
                dragAnchorY = modelY;

                //enable dragging
                canDrag = true;
                break;

            //middle click - reset pos
            case 2:
                canRotate = false;
                canDrag = false;
                resetModelPos();
                break;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        canRotate = false;
        canDrag = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        //set rotations
        if (canRotate) {
            //get starter rotation angle then get hot much is moved and divided by a slow factor
            angleX = (float) (anchorAngleX + (anchorY - mouseY) / (3.0 / guiScale));
            angleY = (float) (anchorAngleY - (anchorX - mouseX) / (3.0 / guiScale));

            //prevent rating so much down and up
            if (angleX > 90) {
                anchorY = (float) mouseY;
                anchorAngleX = 90;
                angleX = 90;
            } else if (angleX < -90) {
                anchorY = (float) mouseY;
                anchorAngleX = -90;
                angleX = -90;
            }
            //cap to 360 so we don't get extremely high unnecessary rotation values
            if (angleY >= 360 || angleY <= -360) {
                anchorX = (float) mouseX;
                anchorAngleY = 0;
                angleY = 0;
            }
        }

        //right click - move
        else if (canDrag && expand) {
            //get how much it should move
            //get actual pos of the mouse, then subtract starter X,Y
            float x = (float) (mouseX - dragDeltaX);
            float y = (float) (mouseY - dragDeltaY);

            //move it
            if (modelX >= 0 && modelX <= this.width)
                modelX = (int) (dragAnchorX + x);
            if (modelY >= 0 && modelY <= this.height)
                modelY = (int) (dragAnchorY + y);

            //if out of range - move it back
            //cant be "elsed" because it needs to be checked after the move
            modelX = modelX < 0 ? 0 : Math.min(modelX, this.width);
            modelY = modelY < 0 ? 0 : Math.min(modelY, this.height);
        }

        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        boolean result = super.keyReleased(keyCode, scanCode, modifiers);

        if (keyCode == GLFW.GLFW_KEY_LEFT_SHIFT) {
            isHoldingShift = false;
            deleteButton.active = false;
            openFolderButton.setMessage(new TranslatableText("figura.gui.button.openfolder"));
        }

        return result;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        //scroll - scale
        if (expand) {
            //set scale direction
            float scaledir = (amount > 0) ? SCALE_FACTOR : 1 / SCALE_FACTOR;

            //determine scale
            scaledValue = ((modelSize + scaledValue) * scaledir) - modelSize;

            //limit scale
            if (scaledValue <= -35) scaledValue = -35.0F;
            if (scaledValue >= 250) scaledValue = 250.0F;
        }

        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == OvO[ovo]) {
            ovo++;
            if (ovo >= OvO.length) {
                ovo = 0;
                this.client.setScreen(new NewFiguraGuiScreen(this));
            }
        } else if (ovo != 2 || keyCode != 265) {
            ovo = 0;
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE && expand) {
            expand = false;
            updateExpand();
            return false;
        }

        boolean result = super.keyPressed(keyCode, scanCode, modifiers);

        if (keyCode == GLFW.GLFW_KEY_LEFT_SHIFT) {
            isHoldingShift = true;
            deleteButton.active = connectionStatus == 3;
            openFolderButton.setMessage(new TranslatableText("figura.gui.button.openavatarfolder"));
        }

        return result;
    }

    @Override
    public void filesDragged(List<Path> paths) {
        super.filesDragged(paths);

        String string = paths.stream().map(Path::getFileName).map(Path::toString).collect(Collectors.joining(", "));
        this.client.setScreen(new ConfirmScreen((bl) -> {
            Path destPath = LocalAvatarData.getContentDirectory();
            if (bl) {
                paths.forEach((path2) -> {
                    try {
                        Stream<Path> stream = Files.walk(path2);
                        try {
                            stream.forEach((path3) -> {
                                try {
                                    Util.relativeCopy(path2.getParent(), destPath, path3);
                                } catch (IOException e) {
                                    FiguraMod.LOGGER.error("Failed to copy model file from {} to {}", path3, destPath);
                                    e.printStackTrace();
                                }

                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        stream.close();
                    } catch (Exception e) {
                        FiguraMod.LOGGER.error("Failed to copy model file from {} to {}", path2, destPath);
                        e.printStackTrace();
                    }

                });
            }
            this.client.setScreen(this);
        }, new TranslatableText("figura.gui.dropconfirm"), new LiteralText(string)));
    }

    public static void drawEntity(int x, int y, int size, float rotationX, float rotationY, LivingEntity entity) {
        if (entity == null) return;
        MatrixStack matrixStack = RenderSystem.getModelViewStack();
        matrixStack.push();
        matrixStack.translate(x, y, 1500.0D);
        matrixStack.scale(1.0F, 1.0F, -1.0F);
        RenderSystem.applyModelViewMatrix();
        MatrixStack matrixStack2 = new MatrixStack();
        matrixStack2.translate(0.0D, 0.0D, 1000.0D);
        matrixStack2.scale((float) size, (float) size, (float) size);
        Quaternion quaternion = Vec3f.POSITIVE_Z.getDegreesQuaternion(180.0F);
        Quaternion quaternion2 = Vec3f.POSITIVE_X.getDegreesQuaternion(rotationX);
        quaternion.hamiltonProduct(quaternion2);
        matrixStack2.multiply(quaternion);
        float h = entity.bodyYaw;
        float i = entity.getYaw();
        float j = entity.getPitch();
        float k = entity.prevHeadYaw;
        float l = entity.headYaw;
        boolean invisible = entity.isInvisible();
        entity.bodyYaw = 180.0F - rotationY;
        entity.setYaw(180.0F - rotationY);
        entity.setPitch(0.0F);
        entity.headYaw = entity.getYaw();
        entity.prevHeadYaw = entity.getYaw();
        entity.setInvisible(false);
        showOwnNametag = (boolean) Config.PREVIEW_NAMEPLATE.value;
        renderFireOverlay = false;
        DiffuseLighting.method_34742();
        EntityRenderDispatcher entityRenderDispatcher = MinecraftClient.getInstance().getEntityRenderDispatcher();
        quaternion2.conjugate();
        entityRenderDispatcher.setRotation(quaternion2);
        entityRenderDispatcher.setRenderShadows(false);
        VertexConsumerProvider.Immediate immediate = MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();
        int box = modelBgSize * guiScale;
        if (!expand)
            RenderSystem.enableScissor(x * guiScale - box / 2, y * guiScale - box / 2, box, box);
        RenderSystem.runAsFancy(() -> entityRenderDispatcher.render(entity, 0.0D, -1.0D, 0.0D, 0.0F, 1.0F, matrixStack2, immediate, 15728880));
        RenderSystem.disableScissor();
        immediate.draw();
        entityRenderDispatcher.setRenderShadows(true);
        entity.bodyYaw = h;
        entity.setYaw(i);
        entity.setPitch(j);
        entity.prevHeadYaw = k;
        entity.headYaw = l;
        entity.setInvisible(invisible);
        showOwnNametag = false;
        renderFireOverlay = true;
        matrixStack.pop();
        RenderSystem.applyModelViewMatrix();
        DiffuseLighting.enableGuiDepthLighting();
    }
}