package net.blancworks.figura.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.blancworks.figura.FiguraMod;
import net.blancworks.figura.PlayerDataManager;
import net.blancworks.figura.network.FiguraNetworkManager;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.entity.LivingEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.TextColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.Quaternion;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.Level;
import org.lwjgl.system.MathUtil;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

public class FiguraGuiScreen extends Screen {

    public Screen parentScreen;

    public Identifier connectedTexture = new Identifier("figura", "gui/menu/connected.png");
    public Identifier disconnectedTexture = new Identifier("figura", "gui/menu/disconnected.png");
    public Identifier uploadTexture = new Identifier("figura", "gui/menu/upload.png");
    public Identifier playerBackgroundTexture = new Identifier("figura", "gui/menu/player_background.png");
    public Identifier scalableBoxTexture = new Identifier("figura", "gui/menu/scalable_box.png");

    public TexturedButtonWidget connectionStatusButton;
    public TexturedButtonWidget disconnectedStatusButton;
    public TexturedButtonWidget uploadButton;

    public ArrayList<ButtonWidget> avatar_load_buttons = new ArrayList<>();
    public ArrayList<String> all_valid_loads = new ArrayList<>();
    public int curr_load_list_index = 0;

    public MutableText name_text;
    public MutableText file_size_text;
    public MutableText model_complexity_text;

    public FiguraGuiScreen(Screen parentScreen) {
        super(new LiteralText("Figura Menu"));
        this.parentScreen = parentScreen;
    }

    @Override
    protected void init() {
        super.init();

        this.addButton(new ButtonWidget(this.width - 64 - 5, this.height - 20 - 5, 64, 20, new LiteralText("Back"), (buttonWidgetx) -> {
            this.client.openScreen((Screen) parentScreen);
            //this.client.mouse.lockCursor();
        }));

        connectionStatusButton = new TexturedButtonWidget(
                this.width - 32 - 5, 5,
                32, 32,
                0, 0, 32,
                connectedTexture, 32, 64,
                (bx) -> {
                    FiguraNetworkManager.authUser();
                }
        );
        this.addButton(connectionStatusButton);
        connectionStatusButton.active = false;

        disconnectedStatusButton = new TexturedButtonWidget(
                this.width - 32 - 5, 5,
                32, 32,
                0, 0, 32,
                disconnectedTexture, 32, 64,
                (bx) -> {
                    FiguraNetworkManager.authUser();
                }
        );
        this.addButton(disconnectedStatusButton);
        disconnectedStatusButton.active = false;

        uploadButton = new TexturedButtonWidget(
                this.width - 64 - 10, 5,
                32, 32,
                0, 0, 32,
                uploadTexture, 32, 64,
                (bx) -> {
                    FiguraNetworkManager.postModel();
                }
        );
        this.addButton(uploadButton);
        uploadButton.active = false;
        uploadButton.visible = false;

        generateValidAvatarButtons();

        if (PlayerDataManager.localPlayer != null && PlayerDataManager.localPlayer.model != null) {
            model_complexity_text = new LiteralText(String.format("Complexity : %d", PlayerDataManager.localPlayer.model.getRenderComplexity()));
            file_size_text = new LiteralText(String.format("File Size : %d", PlayerDataManager.localPlayer.getFileSize()));
        }
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);

        if (FiguraNetworkManager.hasAuthKey()) {
            connectionStatusButton.active = true;
            connectionStatusButton.visible = true;
            uploadButton.active = true;
            uploadButton.visible = true;
            disconnectedStatusButton.active = false;
            disconnectedStatusButton.visible = false;
        } else {
            disconnectedStatusButton.active = true;
            disconnectedStatusButton.visible = true;
            connectionStatusButton.active = false;
            connectionStatusButton.visible = false;
        }

        //Draw player preview.
        {
            MinecraftClient.getInstance().getTextureManager().bindTexture(playerBackgroundTexture);
            drawTexture(matrices, this.width / 2 - (128 / 2), this.height / 2 - (128), 0, 0, 128, 128, 128, 128);

            drawEntity(this.width / 2, this.height / 2 - 32, 32, 0, 0, MinecraftClient.getInstance().player);
        }

        //Draw avatar info
        {
            MinecraftClient.getInstance().getTextureManager().bindTexture(playerBackgroundTexture);
            drawTexture(matrices, this.width / 2 - (128 / 2), this.height / 2, 0, 0, 128, 128, 128, 128);

            int currY = this.height / 2 + 4 - 12;

            if (name_text != null)
                drawTextWithShadow(matrices, MinecraftClient.getInstance().textRenderer, name_text, this.width / 2 - (128 / 2) + 4, currY += 12, 16777215);
            if (file_size_text != null)
                drawTextWithShadow(matrices, MinecraftClient.getInstance().textRenderer, file_size_text, this.width / 2 - (128 / 2) + 4, currY += 12, 16777215);
            if (model_complexity_text != null)
                drawTextWithShadow(matrices, MinecraftClient.getInstance().textRenderer, model_complexity_text, this.width / 2 - (128 / 2) + 4, currY += 12, 16777215);
            
            if(this.getFocused() != null)
                FiguraMod.LOGGER.debug(this.getFocused().toString());
        }

        //Draw list of valid models
        {
            //drawScaledTexture(stack, 0, 0, 100, 100, 14, 14, 2);
        }


        super.render(matrices, mouseX, mouseY, delta);
    }

    public void generateValidAvatarButtons() {

        avatar_load_buttons.clear();
        int buttonCount = (int) Math.floor((this.height - 10) / 24) - 1;

        File contentDirectory = FabricLoader.getInstance().getGameDir().getParent().resolve("model_files").toFile();
        ArrayList<String> valid_loads = new ArrayList<String>();
        File[] files = contentDirectory.listFiles();

        for (File file : files) {
            String fileName = FilenameUtils.removeExtension(file.getName());

            if (Files.exists(contentDirectory.toPath().resolve(fileName + ".bbmodel")) && Files.exists(contentDirectory.toPath().resolve(fileName + ".png"))) {
                if (valid_loads.contains(fileName))
                    continue;
                valid_loads.add(fileName);
            }
        }

        for (int i = 0; i < buttonCount; i++) {
            int fileNameIndex = (curr_load_list_index * buttonCount) + i;

            if (fileNameIndex >= valid_loads.size())
                continue;

            int finalI = i;
            ButtonWidget button = new ButtonWidget(
                    5, (i * 24) + 5, (this.width / 2) - 10 - 64, 20,
                    new LiteralText(valid_loads.get(i)),
                    (bx) -> {
                        click_button(valid_loads.get(finalI));
                    }
            );

            avatar_load_buttons.add(button);
            addButton(button);
        }


    }

    private static int filesize_warning_threshold = 75000;
    private static int filesize_large_threshold = 100000;

    public void click_button(String file_name) {
        PlayerDataManager.localPlayer.loadModelFile(file_name);

        name_text = new LiteralText(String.format("Name : %s", file_name.substring(0, Math.min(15, file_name.length()))));

        CompletableFuture.runAsync(() -> {

            for (int i = 0; i < 1000; i++) {
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

            int fileSize = PlayerDataManager.localPlayer.getFileSize();


            MutableText fsText = new LiteralText(String.format("File Size : %.2fkB", fileSize/1000.0f));

            if (fileSize >= filesize_large_threshold)
                fsText.setStyle(fsText.getStyle().withColor(TextColor.parse("red")));
            else if (fileSize >= filesize_warning_threshold)
                fsText.setStyle(fsText.getStyle().withColor(TextColor.parse("orange")));
            else
                fsText.setStyle(fsText.getStyle().withColor(TextColor.parse("white")));
            
            file_size_text = fsText;
        }, Util.getMainWorkerExecutor());

    }
    
    /*public void drawScaledTexture(MatrixStack stack, int x, int y, int width, int height, int textureWidth, int textureHeight, int borderSize){
        MinecraftClient.getInstance().getTextureManager().bindTexture(scalableBoxTexture);
        
        //tl, tm, tr
        drawTexture(stack, x, x, 0, 0, borderSize, borderSize, textureWidth, textureHeight);
        drawTexture(stack, x, x, borderSize, 0, width - (borderSize * 2), borderSize, textureWidth, textureHeight);
        drawTexture(stack, x, x, textureWidth - borderSize, 0, borderSize, borderSize, textureWidth, textureHeight);
    }*/

    public static void drawEntity(int x, int y, int size, float mouseX, float mouseY, LivingEntity entity) {
        float f = (float) Math.atan((double) (mouseX / 40.0F));
        float g = (float) Math.atan((double) (mouseY / 40.0F));
        RenderSystem.pushMatrix();
        RenderSystem.translatef((float) x, (float) y, 1050.0F);
        RenderSystem.scalef(1.0F, 1.0F, -1.0F);
        MatrixStack matrixStack = new MatrixStack();
        matrixStack.translate(0.0D, 0.0D, 1000.0D);
        matrixStack.scale((float) size, (float) size, (float) size);
        Quaternion quaternion = Vector3f.POSITIVE_Z.getDegreesQuaternion(180.0F);
        Quaternion quaternion2 = Vector3f.POSITIVE_X.getDegreesQuaternion((g * 20.0F) - 15);
        quaternion.hamiltonProduct(quaternion2);
        matrixStack.multiply(quaternion);
        float h = entity.bodyYaw;
        float i = entity.yaw;
        float j = entity.pitch;
        float k = entity.prevHeadYaw;
        float l = entity.headYaw;
        entity.bodyYaw = 180.0F + f * 20.0F;
        entity.yaw = 180.0F + f * 40.0F;
        entity.pitch = -g * 20.0F;
        entity.headYaw = entity.yaw;
        entity.prevHeadYaw = entity.yaw;
        EntityRenderDispatcher entityRenderDispatcher = MinecraftClient.getInstance().getEntityRenderDispatcher();
        quaternion2.conjugate();
        entityRenderDispatcher.setRotation(quaternion2);
        entityRenderDispatcher.setRenderShadows(false);
        VertexConsumerProvider.Immediate immediate = MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();
        RenderSystem.runAsFancy(() -> {
            entityRenderDispatcher.render(entity, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F, matrixStack, immediate, 15728880);
        });
        immediate.draw();
        entityRenderDispatcher.setRenderShadows(true);
        entity.bodyYaw = h;
        entity.yaw = i;
        entity.pitch = j;
        entity.prevHeadYaw = k;
        entity.headYaw = l;
        RenderSystem.popMatrix();
    }


}
