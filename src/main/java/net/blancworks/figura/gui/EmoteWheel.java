package net.blancworks.figura.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.blancworks.figura.PlayerData;
import net.blancworks.figura.PlayerDataManager;
import net.blancworks.figura.lua.CustomScript;
import net.blancworks.figura.lua.api.emoteWheel.EmoteWheelCustomization;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.item.ItemStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Style;
import net.minecraft.text.TextColor;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Quaternion;
import net.minecraft.util.registry.Registry;
import org.luaj.vm2.LuaError;

public class EmoteWheel extends DrawableHelper {

    private final MinecraftClient client;

    public static final Identifier EMOTE_WHEEL = new Identifier("figura", "textures/gui/emote_wheel.png");
    public static final Identifier EMOTE_WHEEL_SELECTED = new Identifier("figura", "textures/gui/emote_wheel_selected.png");
    public static final Identifier EMOTE_WHEEL_LINE = new Identifier("figura", "textures/gui/emote_wheel_line.png");

    public static int selectedSlot = -1;
    public static boolean enabled = true;

    public EmoteWheel(MinecraftClient client) {
        this.client = client;
    }

    public void render(MatrixStack matrices) {
        //screen math
        double width = this.client.getWindow().getWidth() / 2.0d;
        double height = this.client.getWindow().getHeight() / 2.0d;
        double scale = this.client.getWindow().getScaleFactor();

        int mouseX = (int) (this.client.mouse.getX() - width);
        int mouseY = (int) (this.client.mouse.getY() - height);

        int x = (int) (width / scale);
        int y = (int) (height / scale);
        int wheelSize = 192;

        int radius = (int) (68 * 2.0 / 3.0);
        int itemXOffset = (int) ((x - 8 * 1.5) * 2.0 / 3.0);
        int itemYOffset = (int) ((y - 8 * 1.5) * 2.0 / 3.0);

        float angle = (float) Math.toDegrees(Math.atan2(mouseY, mouseX));
        int distance = (int) MathHelper.sqrt(MathHelper.square(mouseX) + MathHelper.square(mouseY));

        selectedSlot = distance > 38 * scale ? (MathHelper.floor((angle + 90) / 45) + 8) % 8 : -1;

        //script data
        PlayerData currentData = PlayerDataManager.localPlayer;

        //render wheel
        renderWheel(matrices, x, y, wheelSize, currentData);

        //render icons
        renderIcons(radius, itemXOffset, itemYOffset, currentData);

        //render line
        renderLine(matrices, x, y, angle, distance, scale);
    }

    public void renderWheel(MatrixStack matrices, int x, int y, int wheelSize, PlayerData data) {
        //wheel
        RenderSystem.enableBlend();

        matrices.push();

        this.client.getTextureManager().bindTexture(EMOTE_WHEEL);
        matrices.translate(x - wheelSize / 2.0d, y - wheelSize / 2.0d, 0.0d);

        drawTexture(matrices, 0, 0, wheelSize, wheelSize, 0.0f, 0.0f, 16, 16, 16, 16);

        matrices.pop();

        //overlay
        if (data != null && data.script != null) {
            EmoteWheelCustomization customization = data.script.getEmoteWheelCustomization("SLOT_" + (selectedSlot + 1));

            if (selectedSlot != -1 && customization != null && customization.function != null) {
                //text
                if (customization.title != null)
                    drawCenteredText(matrices, MinecraftClient.getInstance().textRenderer, new LiteralText(customization.title), x, y - 4, 16777215);

                matrices.push();

                this.client.getTextureManager().bindTexture(EMOTE_WHEEL_SELECTED);

                matrices.translate(x, y, 0.0d);
                Quaternion quaternion = Vector3f.POSITIVE_Z.getDegreesQuaternion(90 * (MathHelper.floor(selectedSlot / 2.0f) + 3));
                matrices.multiply(quaternion);

                drawTexture(matrices, 0, 0, wheelSize / 2, wheelSize / 2, 0.0f, selectedSlot % 2 == 1 ? 16.0f : 0.0f, 16, 16, 16, 32);

                matrices.pop();
            }
        }
        else {
            drawCenteredText(matrices, MinecraftClient.getInstance().textRenderer, new TranslatableText("gui.figura.emotewheel.warning"), x, y - 4, 16733525);
            drawCenteredText(matrices, MinecraftClient.getInstance().textRenderer, new TranslatableText("gui.figura.emotewheel.warninginfo"), x, 4, 16733525);
        }
    }

    public void renderIcons(int radius, int itemXOffset, int itemYOffset, PlayerData data) {
        for (int i = 0; i < 8; i++) {
            ItemStack item = Registry.ITEM.get(Identifier.tryParse("minecraft:barrier")).getDefaultStack();

            if (data != null && data.script != null) {
                EmoteWheelCustomization customization = data.script.getEmoteWheelCustomization("SLOT_" + (i + 1));

                if (customization != null && customization.item != null) {
                    item = customization.item;
                }
            }

            //radius * cos/sin angle in rads + offset
            int itemX = (int) (radius * Math.cos(Math.toRadians(45 * (i - 1.5))) + itemXOffset);
            int itemY = (int) (radius * Math.sin(Math.toRadians(45 * (i - 1.5))) + itemYOffset);

            RenderSystem.pushMatrix();
            RenderSystem.scalef(1.5f, 1.5f, 1.5f);

            this.client.getItemRenderer().renderGuiItemIcon(item, itemX, itemY);

            RenderSystem.popMatrix();
        }
    }

    public void renderLine(MatrixStack matrices, int x, int y, float angle, int distance, double scale) {

        this.client.getTextureManager().bindTexture(EMOTE_WHEEL_LINE);

        matrices.push();

        matrices.translate(x, y, 0.0d);
        Quaternion quaternion = Vector3f.POSITIVE_Z.getDegreesQuaternion(angle);
        matrices.multiply(quaternion);

        drawTexture(matrices, 0, 0, (int) (distance / scale), 1, 0.0f, 0.0f, 1, 1, 1, 1);

        matrices.pop();
    }

    public static void play() {
        if (selectedSlot != -1) {
            PlayerData currentData = PlayerDataManager.localPlayer;

            if (currentData != null && currentData.script != null) {
                EmoteWheelCustomization customization = currentData.script.getEmoteWheelCustomization("SLOT_" + (selectedSlot + 1));

                if (customization != null && customization.function != null) {
                    try {
                        customization.function.call();
                    } catch (Exception error) {
                        if (error instanceof LuaError) {
                            String msg = error.getMessage();
                            msg = msg.replace("\t", "   ");
                            String[] messageParts = msg.split("\n");

                            for (String part : messageParts) {
                                CustomScript.sendChatMessage(new LiteralText(part).setStyle(Style.EMPTY.withColor(TextColor.parse("red"))));
                            }
                        }

                        error.printStackTrace();
                    }
                }
            }

            enabled = false;
            selectedSlot = -1;
        }
    }
}
