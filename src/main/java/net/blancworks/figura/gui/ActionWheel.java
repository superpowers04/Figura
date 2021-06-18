package net.blancworks.figura.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.brigadier.StringReader;
import net.blancworks.figura.PlayerData;
import net.blancworks.figura.PlayerDataManager;
import net.blancworks.figura.lua.CustomScript;
import net.blancworks.figura.lua.api.actionWheel.ActionWheelCustomization;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.item.ItemStack;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Quaternion;
import net.minecraft.util.registry.Registry;
import org.luaj.vm2.LuaError;

public class ActionWheel extends DrawableHelper {

    private final MinecraftClient client;

    public static final Identifier ACTION_WHEEL = new Identifier("figura", "textures/gui/action_wheel.png");
    public static final Identifier ACTION_WHEEL_SELECTED = new Identifier("figura", "textures/gui/action_wheel_selected.png");

    public static int selectedSlot = -1;
    public static boolean enabled = true;

    public ActionWheel(MinecraftClient client) {
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
        int itemXOffset = (int) ((x * 2.0 / 3.0) - 8);
        int itemYOffset = (int) ((y * 2.0 / 3.0) - 8);
        float angle = (float) Math.toDegrees(Math.atan2(mouseY, mouseX));
        int distance = (int) MathHelper.sqrt(MathHelper.square(mouseX) + MathHelper.square(mouseY));

        selectedSlot = distance > 38 * scale ? (MathHelper.floor((angle + 90) / 45) + 8) % 8 : -1;

        //script data
        PlayerData currentData = PlayerDataManager.localPlayer;

        //render wheel
        renderWheel(matrices, x, y, wheelSize, currentData, radius, itemXOffset, itemYOffset, scale);
    }

    public void renderWheel(MatrixStack matrices, int x, int y, int wheelSize, PlayerData data, int radius, int itemXOffset, int itemYOffset, double scale) {
        //wheel
        RenderSystem.enableBlend();

        matrices.push();

        this.client.getTextureManager().bindTexture(ACTION_WHEEL);
        matrices.translate(x - wheelSize / 2.0d, y - wheelSize / 2.0d, 0.0d);

        drawTexture(matrices, 0, 0, wheelSize, wheelSize, 0.0f, 0.0f, 16, 16, 16, 16);

        matrices.pop();

        if (data != null && data.script != null) {
            ActionWheelCustomization customization = data.script.getActionWheelCustomization("SLOT_" + (selectedSlot + 1));

            if (selectedSlot != -1) {
                //overlay
                matrices.push();

                this.client.getTextureManager().bindTexture(ACTION_WHEEL_SELECTED);

                matrices.translate(x, y, 0.0d);
                Quaternion quaternion = Vector3f.POSITIVE_Z.getDegreesQuaternion(90 * (MathHelper.floor(selectedSlot / 2.0f) + 3));
                matrices.multiply(quaternion);

                boolean hasFunction = customization != null && customization.function != null;

                drawTexture(matrices, 0, 0, wheelSize / 2, wheelSize / 2, hasFunction ? 0.0f : 16.0f, selectedSlot % 2 == 1 ? 16.0f : 0.0f, 16, 16, 32, 32);

                matrices.pop();

                //text
                matrices.push();
                matrices.translate(0, 0, 599);
                if (!hasFunction) {
                    drawTextWithShadow(matrices, this.client.textRenderer, new TranslatableText("gui.figura.actionwheel.nofunction"), (int) (this.client.mouse.getX() / scale), (int) (this.client.mouse.getY() / scale) - 10, 16733525);
                }
                else if (customization.title != null) {
                    Text title;

                    try {
                        title = Text.Serializer.fromJson(new StringReader(customization.title));
                    } catch (Exception ignored) {
                        title = new LiteralText(customization.title);
                    }

                    drawTextWithShadow(matrices, this.client.textRenderer, title, (int) (this.client.mouse.getX() / scale), (int) (this.client.mouse.getY() / scale) - 10, 16777215);
                }
                matrices.pop();
            }

            //render icons
            for (int i = 0; i < 8; i++) {
                customization = data.script.getActionWheelCustomization("SLOT_" + (i + 1));

                ItemStack item = Registry.ITEM.get(Identifier.tryParse("minecraft:air")).getDefaultStack();

                if (customization != null && customization.item != null)
                    item = customization.item;

                //radius * cos/sin angle in rads + offset
                int itemX = (int) (radius * Math.cos(Math.toRadians(45 * (i - 1.5))) + itemXOffset);
                int itemY = (int) (radius * Math.sin(Math.toRadians(45 * (i - 1.5))) + itemYOffset);

                RenderSystem.pushMatrix();
                RenderSystem.scalef(1.5f, 1.5f, 1.5f);

                this.client.getItemRenderer().renderGuiItemIcon(item, itemX, itemY);

                RenderSystem.popMatrix();
            }
        }
        else {
            //draw warning texts
            drawCenteredText(matrices, MinecraftClient.getInstance().textRenderer, new TranslatableText("gui.figura.actionwheel.warning").formatted(Formatting.UNDERLINE), x, y - 4, 16733525);
            drawCenteredText(matrices, MinecraftClient.getInstance().textRenderer, new TranslatableText("gui.figura.actionwheel.warninginfo"), x, Math.max(y - wheelSize / 2 - 10, 4), 16733525);
        }

        RenderSystem.disableBlend();
    }

    public static void play() {
        if (selectedSlot != -1) {
            PlayerData currentData = PlayerDataManager.localPlayer;

            if (currentData != null && currentData.script != null) {
                ActionWheelCustomization customization = currentData.script.getActionWheelCustomization("SLOT_" + (selectedSlot + 1));

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
