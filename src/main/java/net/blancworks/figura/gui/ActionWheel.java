package net.blancworks.figura.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.brigadier.StringReader;
import net.blancworks.figura.Config;
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
import net.minecraft.util.math.Vec2f;
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
        //screen
        Vec2f screenSize = new Vec2f(this.client.getWindow().getWidth() / 2.0f, this.client.getWindow().getHeight() / 2.0f);
        float screenScale = (float) this.client.getWindow().getScaleFactor();

        //mouse
        Vec2f mousePos = new Vec2f((float) this.client.mouse.getX() - screenSize.x, (float) this.client.mouse.getY() - screenSize.y);
        float angle = getAngle(mousePos.x, mousePos.y);
        float distance = MathHelper.sqrt(mousePos.x * mousePos.x + mousePos.y * mousePos.y);

        //wheel
        Vec2f wheelPos = new Vec2f(screenSize.x / screenScale, screenSize.y / screenScale);
        int wheelSize = 192;

        //item
        Vec2f itemOffset = new Vec2f((wheelPos.x * 2.0f / 3.0f) - 8, (wheelPos.y * 2.0f / 3.0f) - 8);
        int itemRadius = 42;

        //script data
        PlayerData data = PlayerDataManager.localPlayer;

        //render
        RenderSystem.enableBlend();

        if (data != null && data.script != null) {
            int segments = data.script.actionWheelSize;
            selectedSlot = distance > 30 * screenScale ? MathHelper.floor((segments / 360.0) * angle) : -1;

            //render wheel
            renderWheel(matrices, wheelPos, wheelSize, segments / 2);

            //render overlay and text
            if (selectedSlot != -1) {
                renderOverlay(matrices, wheelPos, wheelSize, segments / 2, data);
                renderText(matrices, wheelPos, wheelSize, screenScale, data);
            }

            //render items
            renderItems(segments, itemOffset, itemRadius, data);
        }
        else {
            //draw default wheel
            renderWheel(matrices, wheelPos, wheelSize, 4);

            //draw warning texts
            drawCenteredText(
                    matrices, MinecraftClient.getInstance().textRenderer,
                    new TranslatableText("gui.figura.actionwheel.warning").formatted(Formatting.UNDERLINE),
                    (int) wheelPos.x, (int) wheelPos.y - 4,
                    16733525
            );
            drawCenteredText(
                    matrices, MinecraftClient.getInstance().textRenderer,
                    new TranslatableText("gui.figura.actionwheel.warninginfo"),
                    (int) wheelPos.x, (int) Math.max(wheelPos.y - wheelSize / 2.0 - 10, 4),
                    16733525
            );
        }

        RenderSystem.disableBlend();
    }

    public float getAngle(float x, float y) {
        float ang = (float) Math.toDegrees(MathHelper.atan2(x, -y));
        return ang < 0 ? 360 + ang : ang;
    }

    public void renderWheel(MatrixStack matrices, Vec2f pos, int size, int segments) {
        //texture
        this.client.getTextureManager().bindTexture(ACTION_WHEEL);

        //draw right side
        matrices.push();
        matrices.translate(pos.x, pos.y - size / 2.0d, 0.0d);
        drawTexture(matrices, 0, 0, size / 2, size, 8.0f * (segments - 1), 0.0f, 8, 16, 32, 16);
        matrices.pop();

        //draw left side
        matrices.push();

        matrices.translate(pos.x, pos.y + size / 2.0d, 0.0d);
        Quaternion quaternion = Vector3f.POSITIVE_Z.getDegreesQuaternion(180);
        matrices.multiply(quaternion);

        drawTexture(matrices, 0, 0, size / 2, size, 8.0f * (segments - 1), 0.0f, 8, 16, 32, 16);

        matrices.pop();
    }

    public void renderOverlay(MatrixStack matrices, Vec2f pos, int size, int segments, PlayerData data) {
        //modifiable variables
        double y = pos.y;
        float angle = 0.0f;
        int height = size / 2;
        float u = 0.0f;
        float v = 0.0f;
        int regionHeight = 8;

        switch (segments) {
            case 1: {
                y = selectedSlot % 2 == 1 ? pos.y + size / 2.0d : pos.y - size / 2.0d;
                angle = 180f * selectedSlot;
                height = size;
                regionHeight = 16;
                break;
            }
            case 2: {
                angle = 90f * (selectedSlot - 1f);
                u = 8.0f;
                break;
            }
            case 3: {
                if (selectedSlot % 3 != 2) {
                    y += (selectedSlot < 3 ? -1 : 1) * size / 2.0d;

                    if (selectedSlot % 3 == 1) {
                        y += (selectedSlot < 3 ? 1 : -1) * size / 4.0d;
                        v = 8.0f;
                    }

                    u = 16.0f;
                }
                else {
                    u = 8.0f;
                    v = 8.0f;
                }

                angle = 180f * MathHelper.floor(selectedSlot / 3.0d);
                break;
            }
            case 4: {
                angle = 90f * (MathHelper.floor(selectedSlot / 2.0d) + 3f);
                u = 24.0f;
                v = selectedSlot % 2 == 1 ? 8.0f : 0.0f;
                break;
            }
        }

        //texture
        this.client.getTextureManager().bindTexture(ACTION_WHEEL_SELECTED);

        //draw
        matrices.push();

        matrices.translate(pos.x, y, 0.0d);
        Quaternion quaternion = Vector3f.POSITIVE_Z.getDegreesQuaternion(angle);
        matrices.multiply(quaternion);

        ActionWheelCustomization customization = data.script.getActionWheelCustomization("SLOT_" + (selectedSlot + 1));
        boolean hasFunction = customization != null && customization.function != null;

        drawTexture(matrices, 0, 0, size / 2, height, u, hasFunction ? v : v + 16.0f, 8, regionHeight, 32, 32);

        matrices.pop();
    }

    public void renderText(MatrixStack matrices, Vec2f pos, int size, float scale, PlayerData data) {
        //customization
        ActionWheelCustomization customization = data.script.getActionWheelCustomization("SLOT_" + (selectedSlot + 1));

        Text text = new TranslatableText("gui.figura.actionwheel.nofunction");
        int textColor = Formatting.RED.getColorValue();

        if (customization != null && customization.function != null) {
            if (customization.title == null)
                return;

            try {
                text = Text.Serializer.fromJson(new StringReader(customization.title));
            } catch (Exception ignored) {
                text = new LiteralText(customization.title);
            }

            textColor = Formatting.WHITE.getColorValue();
        }

        //text pos
        Vec2f textPos;
        int titleLen = this.client.textRenderer.getWidth(text) / 2;

        switch ((int) Config.entries.get("actionWheelPos").value) {
            //top
            case 1: textPos = new Vec2f(pos.x - titleLen, (float) Math.max(pos.y - size / 2.0 - 10, 4)); break;
            //bottom
            case 2: textPos = new Vec2f(pos.x - titleLen, (float) Math.min(pos.y + size / 2.0 + 4, this.client.getWindow().getHeight() - 12)); break;
            //center
            case 3: textPos = new Vec2f(pos.x - titleLen, pos.y - 4); break;
            //default mouse
            default: textPos = new Vec2f((float) this.client.mouse.getX() / scale, (float) this.client.mouse.getY() / scale - 10); break;
        }

        //draw
        matrices.push();
        matrices.translate(0, 0, 599);
        drawTextWithShadow(matrices, this.client.textRenderer, text, (int) textPos.x, (int) textPos.y, textColor);
        matrices.pop();
    }

    public void renderItems(int segments, Vec2f offset, int radius, PlayerData data) {
        for (int i = 0; i < segments; i++) {
            //get item
            ActionWheelCustomization customization = data.script.getActionWheelCustomization("SLOT_" + (i + 1));

            ItemStack item = Registry.ITEM.get(Identifier.tryParse("minecraft:air")).getDefaultStack();

            if (customization != null && customization.item != null)
                item = customization.item;

            //radius * cos/sin angle in rads + offset
            float angle = (float) Math.toRadians(360.0 / segments * (i - (segments - 2) / 4.0));
            Vec2f pos = new Vec2f(radius * MathHelper.cos(angle) + offset.x, radius * MathHelper.sin(angle) + offset.y);

            //render
            RenderSystem.pushMatrix();
            RenderSystem.scalef(1.5f, 1.5f, 1.5f);

            this.client.getItemRenderer().renderGuiItemIcon(item, (int) pos.x, (int) pos.y);

            RenderSystem.popMatrix();
        }
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
