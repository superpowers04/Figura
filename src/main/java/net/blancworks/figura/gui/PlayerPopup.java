package net.blancworks.figura.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.blancworks.figura.PlayerData;
import net.blancworks.figura.PlayerDataManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;

import java.util.List;

public class PlayerPopup extends DrawableHelper {

    private static final Identifier POPUP_TEXTURE = new Identifier("figura", "textures/gui/popup.png");
    private static int index = 0;
    private static boolean enabled = false;
    public static Entity entity;

    private static final List<Text> buttons = List.of(
            new TranslatableText("gui.figura.playerpopup.cancel"),
            new TranslatableText("gui.figura.playerpopup.reload"),
            new TranslatableText("gui.figura.playerpopup.increasetrust"),
            new TranslatableText("gui.figura.playerpopup.decreasetrust")
    );

    public static boolean render(AbstractClientPlayerEntity entity, MatrixStack matrices, EntityRenderDispatcher dispatcher, PlayerData data) {
        if (entity != PlayerPopup.entity || data == null) return false;

        matrices.push();
        matrices.translate(0f, entity.getHeight() + 0.5f, 0f);
        matrices.multiply(dispatcher.getRotation());
        matrices.scale(-0.025f, -0.025f, 0.025f);
        matrices.translate(0f, 0f, -3f);

        RenderSystem.setShaderTexture(0, POPUP_TEXTURE);
        RenderSystem.enableDepthTest();

        //background
        drawTexture(matrices, -36, -12, 72, 24, 0f, 0f, 72, 24, 72, 60);

        //icons
        matrices.translate(0f, 0f, -1f);
        for (int i = 0; i < 4; i++) {
            drawTexture(matrices, -36 + (18 * i), -11, 18, 18, 18f * i, i == index ? 42f : 24f, 18, 18, 72, 60);
        }

        //text
        drawCenteredTextWithShadow(matrices, MinecraftClient.getInstance().textRenderer, buttons.get(index).asOrderedText(), 0, -24, 0xFFFFFF);

        matrices.pop();
        enabled = true;
        return true;
    }

    public static boolean mouseScrolled(double d) {
        if (enabled) index = ((int) (index - d) + 4) % 4;
        return enabled;
    }

    public static void execute() {
        PlayerData data = PlayerDataManager.getDataForPlayer(entity.getUuid());

        if (data != null) {
            switch (index) {
                case 1 -> {
                    if (data.hasAvatar() && data.isAvatarLoaded()) PlayerDataManager.clearPlayer(entity.getUuid());
                }
                case 2 -> {
                    //todo
                }
                case 3 -> {
                    //todo 2
                }
            }
        }

        index = 0;
        enabled = false;
        entity = null;
    }
}
