package net.blancworks.figura.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.blancworks.figura.FiguraMod;
import net.blancworks.figura.PlayerData;
import net.blancworks.figura.PlayerDataManager;
import net.blancworks.figura.config.ConfigManager;
import net.blancworks.figura.lua.api.nameplate.NamePlateAPI;
import net.blancworks.figura.mixin.PlayerListHudAccessorMixin;
import net.blancworks.figura.trust.PlayerTrustManager;
import net.blancworks.figura.trust.TrustContainer;
import net.blancworks.figura.utils.MathUtils;
import net.blancworks.figura.utils.TextUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3f;
import net.minecraft.util.math.Vector4f;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class PlayerPopup extends DrawableHelper {

    private static final Identifier POPUP_TEXTURE = new Identifier("figura", "textures/gui/popup.png");
    private static final Identifier POPUP_MINI_TEXTURE = new Identifier("figura", "textures/gui/popup_mini.png");
    public static double lastX;
    private static int index = 0;
    public static int listIndex = 0;
    public static int listSize = -1;
    private static boolean enabled = false;
    public static boolean miniEnabled = false;
    public static Entity entity;

    private static final List<Text> buttons = List.of(
            new TranslatableText("gui.figura.playerpopup.cancel"),
            new TranslatableText("gui.figura.playerpopup.reload"),
            new TranslatableText("gui.figura.playerpopup.increasetrust"),
            new TranslatableText("gui.figura.playerpopup.decreasetrust")
    );

    public static void renderMini(MatrixStack matrices, PlayerListEntry entry, int x, int y) {
        enabled = true;
        miniEnabled = true;

        if (entry != null) {
            RenderSystem.setShaderTexture(0, POPUP_MINI_TEXTURE);
            matrices.push();

            matrices.translate(x - 51, y-2, 0);

            drawTexture(matrices, 0, 0, 0f, 13f, 48, 13, 64, 64);

            int color = ConfigManager.ACCENT_COLOR.apply(Style.EMPTY).getColor().getRgb();

            RenderSystem.setShaderColor(((color >> 16) & 0xFF) / 255f, ((color >>  8) & 0xFF) / 255f, (color & 0xFF) / 255f, 1f);
            drawTexture(matrices, 0, 0, 0f, 0f, 48, 13, 64, 64);

            for (int i = 0; i < 4; i++) {
                drawTexture(matrices, 1 + (i*12), 1, i*11, 25 + (index == i ? 11 : 0), 11, 11, 64, 64);
            }

            RenderSystem.setShaderColor(1,1,1,1);
            matrices.pop();
        } else {
            listIndex = 0;
        }
    }

    public static void render(MatrixStack matrices) {
        MinecraftClient client = MinecraftClient.getInstance();
        VertexConsumerProvider vcp = FiguraMod.vertexConsumerProvider;

        PlayerData data = entity == null ? null : PlayerDataManager.getDataForPlayer(entity.getUuid());
        if (data == null || vcp == null || (entity.isInvisibleTo(client.player) && entity != client.player)) {
            entity = null;
            return;
        }

        TextRenderer textRenderer = client.textRenderer;
        RenderSystem.disableDepthTest();
        matrices.push();

        //world to screen space
        Vec3f worldPos = new Vec3f(entity.getLerpedPos(client.getTickDelta()));
        worldPos.add(0f, entity.getHeight() + 0.1f, 0f);

        Vector4f vec = MathUtils.worldToScreenSpace(worldPos);
        if (vec.getZ() < 1) return;

        float w = client.getWindow().getScaledWidth();
        float h = client.getWindow().getScaledHeight();
        float s = (float) MathHelper.clamp(client.getWindow().getHeight() * 0.035f / vec.getW() * (1f / client.getWindow().getScaleFactor()), 1f, 16f);

        matrices.translate((vec.getX() + 1f) / 2f * w, (vec.getY() + 1f) / 2f * h, -100f);
        matrices.scale(s / 2f, s / 2f, 1f);

        //title
        Text title = buttons.get(index);
        TextUtils.renderOutlineText(textRenderer, title, -textRenderer.getWidth(title) / 2f, -40, 0xFFFFFF, 0x202020, matrices);

        int color = ConfigManager.ACCENT_COLOR.apply(Style.EMPTY).getColor().getRgb();

        //background
        RenderSystem.setShaderTexture(0, POPUP_TEXTURE);
        drawTexture(matrices, -36, -30, 72, 30, 0f, 0f, 72, 30, 72, 96);

        RenderSystem.setShaderColor(((color >> 16) & 0xFF) / 255f, ((color >>  8) & 0xFF) / 255f, (color & 0xFF) / 255f, 1f);
        drawTexture(matrices, -36, -30, 72, 30, 0f, 30f, 72, 30, 72, 96);

        //icons
        matrices.translate(0f, 0f, -2f);
        for (int i = 0; i < 4; i++) {
            drawTexture(matrices, -36 + (18 * i), -23, 18, 18, 18f * i, i == index ? 78f : 60f, 18, 18, 72, 96);
        }

        //playername
        MutableText name = data.playerName.shallowCopy().formatted(Formatting.BLACK);
        Text badges = NamePlateAPI.getBadges(data);
        if (badges != null) name.append(badges);

        Text trust = new TranslatableText("gui.figura." + data.getTrustContainer().parentID.getPath()).formatted(Formatting.BLACK);

        matrices.scale(0.5f, 0.5f, 0.5f);
        matrices.translate(0f, 0f, -1f);
        textRenderer.draw(matrices, name, -66, -55, 0xFFFFFF);
        textRenderer.draw(matrices, trust, -textRenderer.getWidth(trust) + 66, -55, 0xFFFFFF);

        //return
        matrices.pop();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        data.hasPopup = true;
        enabled = true;
        miniEnabled = true;
    }

    public static boolean mouseScrolled(double d) {
        if (miniEnabled) {
            boolean holdingShift = InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(), GLFW.GLFW_KEY_LEFT_SHIFT);
            if (holdingShift) {
                listIndex -= (int)Math.signum(d);
                listIndex %= listSize;
                if (listIndex < 0)
                    listIndex += listSize;
                return enabled;
            }
        }

        if (enabled) index = (int) (index - d + 4) % 4;
        return enabled;
    }

    public static void hotbarKeyPressed(int i) {
        if (enabled) index = i % 4;
    }

    public static void execute() {
        PlayerData data = entity == null ? null : PlayerDataManager.getDataForPlayer(entity.getUuid());

        if (data != null) {
            data.hasPopup = false;
            MutableText playerName = new LiteralText("").append(data.playerName);
            Text badges = NamePlateAPI.getBadges(data);
            if (badges != null) playerName.append(badges);

            switch (index) {
                case 1 -> {
                    if (data.hasAvatar() && data.isAvatarLoaded()) {
                        PlayerDataManager.clearPlayer(entity.getUuid());
                        FiguraMod.sendToast(playerName, "gui.figura.toast.avatar.reload.title");
                    }
                }
                case 2 -> {
                    TrustContainer tc = data.getTrustContainer();
                    if (PlayerTrustManager.increaseTrust(tc))
                        FiguraMod.sendToast(playerName, new TranslatableText("gui.figura.toast.avatar.trust.title").append(new TranslatableText("gui.figura." + tc.parentID.getPath())));
                }
                case 3 -> {
                    TrustContainer tc = data.getTrustContainer();
                    if (PlayerTrustManager.decreaseTrust(tc))
                        FiguraMod.sendToast(playerName, new TranslatableText("gui.figura.toast.avatar.trust.title").append(new TranslatableText("gui.figura." + tc.parentID.getPath())));
                }
            }
        }

        index = 0;
        enabled = false;
        entity = null;
    }
}
