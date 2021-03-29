package net.blancworks.figura.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.blancworks.figura.FiguraMod;
import net.blancworks.figura.PlayerDataManager;
import net.blancworks.figura.gui.widgets.CustomListWidgetState;
import net.blancworks.figura.gui.widgets.ModelFileListWidget;
import net.blancworks.figura.network.FiguraNetworkManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3f;
import net.minecraft.entity.LivingEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.TextColor;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.Quaternion;
import org.apache.logging.log4j.Level;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class FiguraGuiScreen extends Screen {

    public Screen parentScreen;

    public Identifier uploadTexture = new Identifier("figura", "gui/menu/upload.png");
    public Identifier playerBackgroundTexture = new Identifier("figura", "gui/menu/player_background.png");
    public Identifier scalableBoxTexture = new Identifier("figura", "gui/menu/scalable_box.png");

    public TexturedButtonWidget uploadButton;

    public MutableText name_text;
    public MutableText file_size_text;
    public MutableText model_complexity_text;

    private TextFieldWidget searchBox;
    private boolean filterOptionsShown = false;
    private int paneY;
    private int paneWidth;
    private int rightPaneX;
    private int searchBoxX;
    private int filtersX;
    
    public FiguraTrustScreen trustScreen = new FiguraTrustScreen(this);
    
    public CustomListWidgetState modelFileListState = new CustomListWidgetState();
    public ModelFileListWidget modelFileList;

    public FiguraGuiScreen(Screen parentScreen) {
        super(new LiteralText("Figura Menu"));
        this.parentScreen = parentScreen;
    }

    @Override
    protected void init() {
        super.init();

        int width = Math.min((this.width / 2) - 10 - 128, 128);
        
        this.addButton(new ButtonWidget(this.width - width - 5, this.height - 20 - 5, width, 20, new LiteralText("Back"), (buttonWidgetx) -> {
            this.client.openScreen(parentScreen);
        }));

        this.addButton(new ButtonWidget(this.width - width - 5, this.height - 42 - 5, width, 20, new LiteralText("Trust Menu"), (buttonWidgetx) -> {
            this.client.openScreen(trustScreen);
        }));

        this.addButton(new ButtonWidget(this.width - width - 5, this.height - 64 - 5, width, 20, new LiteralText("Help"), (buttonWidgetx) -> {
            Util.getOperatingSystem().open("https://github.com/TheOneTrueZandra/Figura/wiki/Figura-Panel");
        }));

        this.addButton(new ButtonWidget(this.width - width - 5, this.height - 86 - 5, width, 20, new LiteralText("Open Files Folder"), (buttonWidgetx) -> {
            Util.getOperatingSystem().open(FiguraMod.getModContentDirectory().toUri());
        }));

        uploadButton = new TexturedButtonWidget(
                this.width - 32 - 10, 5,
                32, 32,
                0, 0, 32,
                uploadTexture, 32, 64,
                (bx) -> {
                    FiguraNetworkManager.postModel();
                }
        );
        this.addButton(uploadButton);
        
        if (PlayerDataManager.localPlayer != null && PlayerDataManager.localPlayer.model != null) {
            model_complexity_text = new LiteralText(String.format("Complexity : %d", PlayerDataManager.localPlayer.model.getRenderComplexity()));
            file_size_text = getFileSizeText();
        }

        paneY = 48;
        paneWidth = this.width / 2 - 8;
        rightPaneX = width - paneWidth;

        int searchBoxWidth = paneWidth - 5 - 5;
        searchBoxX = 5;
        this.searchBox = new TextFieldWidget(this.textRenderer, searchBoxX, 22, searchBoxWidth, 20, this.searchBox, new TranslatableText("modmenu.search"));
        this.searchBox.setChangedListener((string_1) -> this.modelFileList.filter(string_1, false));
        modelFileList = new ModelFileListWidget(this.client, paneWidth, this.height, paneY , this.height - 5, 14, this.searchBox, this.modelFileList, this, modelFileListState);
        this.addChild(modelFileList);
        this.addChild(searchBox);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        super.render(matrices, mouseX, mouseY, delta);
        
        //Draw player preview.
        {
            RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
            RenderSystem.setShaderTexture(0, playerBackgroundTexture);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            drawTexture(matrices, this.width / 2, this.height / 2 - (128), 0, 0, 128, 128, 128, 128);

            drawEntity(this.width / 2 + 64, this.height / 2 - 32, 32, (this.width / 2.0F + 64) - mouseX, (this.height / 2.0F - 32 - 50) - mouseY, MinecraftClient.getInstance().player);
        }

        //Draw avatar info
        {
            RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
            RenderSystem.setShaderTexture(0, playerBackgroundTexture);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            drawTexture(matrices, this.width / 2, this.height / 2, 0, 0, 128, 128, 128, 128);

            int currY = this.height / 2 + 4 - 12;

            if (name_text != null)
                drawTextWithShadow(matrices, MinecraftClient.getInstance().textRenderer, name_text, this.width / 2 + 4, currY += 12, 16777215);
            if (file_size_text != null)
                drawTextWithShadow(matrices, MinecraftClient.getInstance().textRenderer, file_size_text, this.width / 2 + 4, currY += 12, 16777215);
            if (model_complexity_text != null)
                drawTextWithShadow(matrices, MinecraftClient.getInstance().textRenderer, model_complexity_text, this.width / 2 + 4, currY += 12, 16777215);

            if (this.getFocused() != null)
                FiguraMod.LOGGER.debug(this.getFocused().toString());
        }
        modelFileList.render(matrices, mouseX, mouseY, delta);
        searchBox.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    public void renderBackground(MatrixStack matrices) {
        super.renderBackground(matrices);
        overlayBackground(0, 0, this.width, this.height, 64, 64, 64, 255, 255);
    }

    static void overlayBackground(int x1, int y1, int x2, int y2, int red, int green, int blue, int startAlpha, int endAlpha) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, OPTIONS_BACKGROUND_TEXTURE);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        buffer.vertex(x1, y2, 0.0D).texture(x1 / 32.0F, y2 / 32.0F).color(red, green, blue, endAlpha).next();
        buffer.vertex(x2, y2, 0.0D).texture(x2 / 32.0F, y2 / 32.0F).color(red, green, blue, endAlpha).next();
        buffer.vertex(x2, y1, 0.0D).texture(x2 / 32.0F, y1 / 32.0F).color(red, green, blue, startAlpha).next();
        buffer.vertex(x1, y1, 0.0D).texture(x1 / 32.0F, y1 / 32.0F).color(red, green, blue, startAlpha).next();
        tessellator.draw();
    }
    
    private static int filesize_warning_threshold = 75000;
    private static int filesize_large_threshold = 100000;

    public void click_button(String file_name) {
        PlayerDataManager.lastLoadedFileName = file_name;
        PlayerDataManager.localPlayer.loadModelFile(file_name);

        name_text = new LiteralText(String.format("Name : %s", file_name.substring(0, Math.min(15, file_name.length()))));

        CompletableFuture.runAsync(() -> {

            for (int i = 0; i < 10; i++) {
                if (PlayerDataManager.localPlayer.texture.ready == true) {
                    break;
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    FiguraMod.LOGGER.log(Level.ERROR, e);
                }
            }

            model_complexity_text = new LiteralText(String.format("Complexity : %d", PlayerDataManager.localPlayer.model.getRenderComplexity()));
            file_size_text = getFileSizeText();
        }, Util.getMainWorkerExecutor());

    }

    public MutableText getFileSizeText() {
        int fileSize = PlayerDataManager.localPlayer.getFileSize();


        MutableText fsText = new LiteralText(String.format("File Size : %.2fkB", fileSize / 1000.0f));

        if (fileSize >= filesize_large_threshold)
            fsText.setStyle(fsText.getStyle().withColor(TextColor.parse("red")));
        else if (fileSize >= filesize_warning_threshold)
            fsText.setStyle(fsText.getStyle().withColor(TextColor.parse("orange")));
        else
            fsText.setStyle(fsText.getStyle().withColor(TextColor.parse("white")));

        return fsText;
    }

    public static void drawEntity(int x, int y, int size, float mouseX, float mouseY, LivingEntity entity) {
        float f = (float) Math.atan((double) (mouseX / 40.0F));
        float g = (float) Math.atan((double) (mouseY / 40.0F));
        MatrixStack matrixStack = RenderSystem.getModelViewStack();
        matrixStack.push();
        matrixStack.translate((float) x, (float) y, 1050.0F);
        matrixStack.scale(1.0F, 1.0F, -1.0F);
        RenderSystem.applyModelViewMatrix();
        MatrixStack matrixStack2 = new MatrixStack();
        matrixStack2.translate(0.0D, 0.0D, 1000.0D);
        matrixStack2.scale((float) size, (float) size, (float) size);
        Quaternion quaternion = Vec3f.POSITIVE_Z.getDegreesQuaternion(180.0F);
        Quaternion quaternion2 = Vec3f.POSITIVE_X.getDegreesQuaternion(g * 20.0F);
        quaternion.hamiltonProduct(quaternion2);
        matrixStack2.multiply(quaternion);
        float h = entity.bodyYaw;
        float i = entity.yaw;
        float j = entity.pitch;
        float k = entity.prevHeadYaw;
        float l = entity.headYaw;
        boolean invisible = entity.isInvisible();
        entity.bodyYaw = 180.0F + f * 20.0F;
        entity.yaw = 180.0F + f * 40.0F;
        entity.pitch = -g * 20.0F;
        entity.headYaw = entity.yaw;
        entity.prevHeadYaw = entity.yaw;
        entity.setInvisible(false);
        DiffuseLighting.method_34742();
        EntityRenderDispatcher entityRenderDispatcher = MinecraftClient.getInstance().getEntityRenderDispatcher();
        quaternion2.conjugate();
        entityRenderDispatcher.setRotation(quaternion2);
        entityRenderDispatcher.setRenderShadows(false);
        VertexConsumerProvider.Immediate immediate = MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();
        RenderSystem.runAsFancy(() -> {
            entityRenderDispatcher.render(entity, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F, matrixStack2, immediate, 15728880);
        });
        immediate.draw();
        entityRenderDispatcher.setRenderShadows(true);
        entity.bodyYaw = h;
        entity.yaw = i;
        entity.pitch = j;
        entity.prevHeadYaw = k;
        entity.headYaw = l;
        entity.setInvisible(invisible);
        matrixStack.pop();
        RenderSystem.applyModelViewMatrix();
        DiffuseLighting.enableGuiDepthLighting();
    }
}
