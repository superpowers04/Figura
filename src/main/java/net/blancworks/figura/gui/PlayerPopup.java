package net.blancworks.figura.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.blancworks.figura.FiguraMod;
import net.blancworks.figura.avatar.AvatarData;
import net.blancworks.figura.avatar.AvatarDataManager;
import net.blancworks.figura.config.ConfigManager;
import net.blancworks.figura.gui.widgets.PlayerListWidget;
import net.blancworks.figura.lua.api.nameplate.NamePlateAPI;
import net.blancworks.figura.trust.PlayerTrustManager;
import net.blancworks.figura.trust.TrustContainer;
import net.blancworks.figura.utils.MathUtils;
import net.blancworks.figura.utils.TextUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
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
    private static final Identifier POPUP_TEXTURE_MINI = new Identifier("figura", "textures/gui/popup_mini.png");
    private static int index = 0;

    public static boolean enabled = false;
    public static boolean miniEnabled = false;
    public static int miniSelected = 0;
    public static int miniSize = 1;

    public static AvatarData data;

    public static final List<Text> BUTTONS = List.of(
            new TranslatableText("figura.playerpopup.cancel"),
            new TranslatableText("figura.playerpopup.reload"),
            new TranslatableText("figura.playerpopup.increasetrust"),
            new TranslatableText("figura.playerpopup.decreasetrust"),
            new TranslatableText("figura.playerpopup.trustmenu")
    );

    private static final Text TRUST_TEXT = new LiteralText("").append(new LiteralText("! ").setStyle(Style.EMPTY.withFont(FiguraMod.FIGURA_FONT))).append(new TranslatableText("figura.playerpopup.trustissue"));
    private static final Text SCRIPT_TEXT = new LiteralText("").append(new LiteralText("▲ ").setStyle(Style.EMPTY.withFont(FiguraMod.FIGURA_FONT))).append(new TranslatableText("figura.playerpopup.scriptissue"));

    private static final FiguraTrustScreen TRUST_SCREEN = new FiguraTrustScreen(null);

    public static void renderMini(MatrixStack matrices) {
        if (data == null || enabled)
            return;

        matrices.push();
        RenderSystem.setShaderTexture(0, POPUP_TEXTURE_MINI);
        matrices.translate(-62f, -2f, 0f);

        //background
        drawTexture(matrices, 0, 0, 0f, 0f, 60, 13, 60, 48);

        int color = ConfigManager.ACCENT_COLOR.apply(Style.EMPTY).getColor().getRgb();
        RenderSystem.setShaderColor(((color >> 16) & 0xFF) / 255f, ((color >>  8) & 0xFF) / 255f, (color & 0xFF) / 255f, 1f);

        //foreground
        drawTexture(matrices, 0, 0, 0f, 13f, 60, 13, 60, 48);

        //buttons
        for (int i = 0; i < BUTTONS.size(); i++) {
            drawTexture(matrices, 1 + i * 11, 1, 11f * i, index == i ? 37f : 26f, 11, 11, 60, 48);
        }

        matrices.pop();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        miniEnabled = true;
    }

    public static void render(MatrixStack matrices) {
        if (miniEnabled || data == null) return;

        Entity entity = data.lastEntity;
        MinecraftClient client = MinecraftClient.getInstance();

        if (entity == null || (entity.isInvisibleTo(client.player) && entity != client.player)) {
            data = null;
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

        //trust/script warning
        float offset = -4.5f;
        if (data.script != null && data.script.scriptError || data.trustIssues) {
            Text toRender = data.trustIssues ? TRUST_TEXT : SCRIPT_TEXT;
            int color = data.trustIssues ? Formatting.YELLOW.getColorValue() : Formatting.RED.getColorValue();

            matrices.push();
            matrices.scale(0.5f, 0.5f, 0.5f);
            TextUtils.renderOutlineText(textRenderer, toRender, -textRenderer.getWidth(toRender) / 2f, -70, color, 0x202020, matrices);
            matrices.pop();
        } else {
            offset = 0f;
        }

        //title
        Text title = BUTTONS.get(index);
        TextUtils.renderOutlineText(textRenderer, title, -textRenderer.getWidth(title) / 2f, -40 + offset, 0xFFFFFF, 0x202020, matrices);

        int color = ConfigManager.ACCENT_COLOR.apply(Style.EMPTY).getColor().getRgb();

        //background
        RenderSystem.setShaderTexture(0, POPUP_TEXTURE);
        drawTexture(matrices, -45, -30, 90, 30, 0f, 0f, 90, 30, 90, 96);

        RenderSystem.setShaderColor(((color >> 16) & 0xFF) / 255f, ((color >>  8) & 0xFF) / 255f, (color & 0xFF) / 255f, 1f);
        drawTexture(matrices, -45, -30, 90, 30, 0f, 30f, 90, 30, 90, 96);

        //icons
        matrices.translate(0f, 0f, -2f);
        for (int i = 0; i < BUTTONS.size(); i++) {
            drawTexture(matrices, -45 + (18 * i), -23, 18, 18, 18f * i, i == index ? 78f : 60f, 18, 18, 90, 96);
        }

        //playername
        MutableText name = data.name.shallowCopy().formatted(Formatting.BLACK);
        Text badges = NamePlateAPI.getBadges(data);
        if (badges != null) name.append(badges);

        Text trust = new TranslatableText("figura.trust." + data.getTrustContainer().getParent().getPath()).formatted(Formatting.BLACK);

        matrices.scale(0.5f, 0.5f, 0.5f);
        matrices.translate(0f, 0f, -1f);
        textRenderer.draw(matrices, name, -84, -55, 0xFFFFFF);
        textRenderer.draw(matrices, trust, -textRenderer.getWidth(trust) + 84, -55, 0xFFFFFF);

        //finish rendering
        matrices.pop();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        data.hasPopup = true;
        enabled = true;
    }

    public static boolean mouseScrolled(double d) {
        boolean shift = InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(), GLFW.GLFW_KEY_LEFT_SHIFT);

        if (enabled || (miniEnabled && shift)) index = (int) (index - d + BUTTONS.size()) % BUTTONS.size();
        else if (miniEnabled) miniSelected = (int) (miniSelected - d + miniSize) % miniSize;

        return enabled || miniEnabled;
    }

    public static void hotbarKeyPressed(int i) {
        if (enabled || miniEnabled) index = i % BUTTONS.size();
    }

    public static void execute() {
        if (data != null) {
            data.hasPopup = false;
            MutableText playerName = new LiteralText("").append(data.name);
            Text badges = NamePlateAPI.getBadges(data);
            if (badges != null) playerName.append(badges);

            switch (index) {
                case 1 -> {
                    if (data.hasAvatar() && data.isAvatarLoaded()) {
                        AvatarDataManager.clearPlayer(data.entityId);
                        FiguraMod.sendToast(playerName, "figura.toast.avatar.reload.title");
                    }
                }
                case 2 -> {
                    TrustContainer tc = data.getTrustContainer();
                    if (PlayerTrustManager.increaseTrust(tc))
                        FiguraMod.sendToast(playerName, new TranslatableText("figura.toast.avatar.trust.title").append(new TranslatableText("figura.trust." + tc.getParent().getPath())));
                }
                case 3 -> {
                    TrustContainer tc = data.getTrustContainer();
                    if (PlayerTrustManager.decreaseTrust(tc))
                        FiguraMod.sendToast(playerName, new TranslatableText("figura.toast.avatar.trust.title").append(new TranslatableText("figura.trust." + tc.getParent().getPath())));
                }
                case 4 -> {
                    MinecraftClient.getInstance().setScreen(TRUST_SCREEN);
                    TRUST_SCREEN.searchBox.setText(data.name.getString());

                    PlayerListWidget.PlayerListWidgetEntry entry = TRUST_SCREEN.playerList.getEntry(data.entityId);
                    if (entry != null)
                        TRUST_SCREEN.playerList.select(entry);
                }
            }
        }

        enabled = false;
        miniEnabled = false;

        index = 0;
        data = null;
    }
}
