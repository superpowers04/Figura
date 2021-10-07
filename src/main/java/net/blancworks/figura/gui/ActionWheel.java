package net.blancworks.figura.gui;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.brigadier.StringReader;
import net.blancworks.figura.PlayerData;
import net.blancworks.figura.PlayerDataManager;
import net.blancworks.figura.config.ConfigManager.Config;
import net.blancworks.figura.lua.api.actionWheel.ActionWheelCustomization;
import net.blancworks.figura.models.FiguraTexture;
import net.blancworks.figura.utils.TextUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.texture.MissingSprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Quaternion;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3f;
import net.minecraft.util.registry.Registry;
import org.luaj.vm2.LuaError;

import java.util.List;
import java.util.Objects;

public class ActionWheel extends DrawableHelper {

    private final MinecraftClient client;

    public static final Identifier ACTION_WHEEL = new Identifier("figura", "textures/gui/action_wheel.png");
    public static final Identifier ACTION_WHEEL_SELECTED = new Identifier("figura", "textures/gui/action_wheel_selected.png");
    public static final Vec3f ERROR_COLOR = new Vec3f(1.0f, 0.28f, 0.28f);
    public static final List<Text> NO_FUNCTION_MESSAGE = ImmutableList.of(new TranslatableText("gui.figura.actionwheel.nofunction"));

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
            int leftSegments = data.script.actionWheelLeftSize;
            int rightSegments = data.script.actionWheelRightSize;

            //set selected slot
            if (distance > 30 * screenScale) {
                if (angle < 180) {
                    selectedSlot = MathHelper.floor((rightSegments / 180.0) * angle);
                } else {
                    selectedSlot = MathHelper.floor((leftSegments / 180.0) * (angle - 180)) + 4;
                }
            } else {
                selectedSlot = -1;
            }

            //render wheel
            renderWheel(matrices, wheelPos, wheelSize, leftSegments, rightSegments);

            //render overlay
            for (int i = 0; i < leftSegments + rightSegments; i++) {
                int index = i < rightSegments ? i : i - rightSegments + 4;
                renderOverlay(matrices, wheelPos, wheelSize, leftSegments, rightSegments, data, index);
            }

            //render textures
            renderTextures(matrices, leftSegments, rightSegments, itemOffset, itemRadius, data);

            //render text
            if (selectedSlot != -1) {
                renderText(matrices, wheelPos, wheelSize, screenScale, data);
            }

            //render items
            renderItems(leftSegments, rightSegments, itemOffset, itemRadius, data);
        }
        else {
            //draw default wheel
            renderWheel(matrices, wheelPos, wheelSize, 4, 4);

            //draw warning texts
            drawCenteredTextWithShadow(
                    matrices, MinecraftClient.getInstance().textRenderer,
                    new TranslatableText("gui.figura.actionwheel.warning").formatted(Formatting.UNDERLINE).asOrderedText(),
                    (int) wheelPos.x, (int) wheelPos.y - 4,
                    16733525
            );
            drawCenteredTextWithShadow(
                    matrices, MinecraftClient.getInstance().textRenderer,
                    new TranslatableText("gui.figura.actionwheel.warninginfo").asOrderedText(),
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

    public void renderWheel(MatrixStack matrices, Vec2f pos, int size, int leftSegments, int rightSegments) {
        //texture
        RenderSystem.setShaderTexture(0, ACTION_WHEEL);

        //draw right side
        matrices.push();

        matrices.translate(Math.round(pos.x), Math.round(pos.y - size / 2.0d), 0.0d);
        drawTexture(matrices, 0, 0, size / 2, size, 8.0f * (rightSegments - 1), 0.0f, 8, 16, 32, 16);

        matrices.pop();

        //draw left side
        matrices.push();

        matrices.translate(Math.round(pos.x), Math.round(pos.y + size / 2.0d), 0.0d);
        Quaternion quaternion = Vec3f.POSITIVE_Z.getDegreesQuaternion(180);
        matrices.multiply(quaternion);

        drawTexture(matrices, 0, 0, size / 2, size, 8.0f * (leftSegments - 1), 0.0f, 8, 16, 32, 16);

        matrices.pop();
    }

    public void renderOverlay(MatrixStack matrices, Vec2f pos, int size, int leftSegments, int rightSegments, PlayerData data, int slot) {
        ActionWheelCustomization customization = data.script.getActionWheelCustomization("SLOT_" + (slot + 1));

        //property variables
        boolean hasFunction = false;
        boolean hasColor = false;
        boolean hasHoverColor = false;
        boolean isSelected = selectedSlot == slot;
        Vec3f overlayColor = new Vec3f(1.0f, 1.0f, 1.0f);

        if (customization != null) {
            hasFunction = customization.function != null;
            hasColor = customization.color != null;
            hasHoverColor = customization.hoverColor != null;
        }

        //set default color
        if (hasColor)
            overlayColor = customization.color;

        //if is selected, but has no function, set to error color
        //if it has function and has an hover color, set to the hover color
        if (isSelected) {
            if (!hasFunction) {
                overlayColor = ERROR_COLOR;
            } else if (hasHoverColor) {
                overlayColor = customization.hoverColor;
            }
        } else if (!hasColor) {
            return;
        }

        //modifiable variables
        int segments;
        int selected;

        if (slot < 4) {
            segments = rightSegments;
            selected = slot;
        } else {
            segments = leftSegments;
            selected = slot - 4 + leftSegments;
        }

        double y = pos.y;
        float angle = 0.0f;
        int height = size / 2;
        float u = 0.0f;
        float v = 0.0f;
        int regionHeight = 8;

        switch (segments) {
            case 1 -> {
                y = selected % 2 == 1 ? pos.y + size / 2.0d : pos.y - size / 2.0d;
                angle = 180f * selected;
                height = size;
                regionHeight = 16;
            }
            case 2 -> {
                angle = 90f * (selected - 1f);
                u = 8.0f;
            }
            case 3 -> {
                if (selected % 3 != 2) {
                    y += (selected < 3 ? -1 : 1) * size / 2.0d;

                    if (selected % 3 == 1) {
                        y += (selected < 3 ? 1 : -1) * size / 4.0d;
                        v = 8.0f;
                    }

                    u = 16.0f;
                }
                else {
                    u = 8.0f;
                    v = 8.0f;
                }

                angle = 180f * MathHelper.floor(selected / 3.0d);
            }
            case 4 -> {
                angle = 90f * (MathHelper.floor(selected / 2.0d) + 3f);
                u = 24.0f;
                v = selected % 2 == 1 ? 8.0f : 0.0f;
            }
        }

        //texture
        RenderSystem.setShaderTexture(0, ACTION_WHEEL_SELECTED);

        //draw
        matrices.push();

        matrices.translate(Math.round(pos.x), Math.round(y), 0.0d);
        Quaternion quaternion = Vec3f.POSITIVE_Z.getDegreesQuaternion(angle);
        matrices.multiply(quaternion);

        RenderSystem.setShaderColor(overlayColor.getX(), overlayColor.getY(), overlayColor.getZ(), 1.0f);
        drawTexture(matrices, 0, 0, size / 2, height, u, v, 8, regionHeight, 32, 16);

        matrices.pop();
    }

    public void renderTextures(MatrixStack matrices, int leftSegments, int rightSegments, Vec2f offset, int radius, PlayerData data) {
        for (int i = 0; i < leftSegments + rightSegments; i++) {

            int index;
            float angle;
            if (i < rightSegments) {
                index = i;
                angle = (float) Math.toRadians(180.0 / rightSegments * (index - ((rightSegments - 1) * 0.5)));
            } else {
                index = i - rightSegments + 4;
                angle = (float) Math.toRadians(180.0 / leftSegments * (index - 4 - ((leftSegments - 1) * 0.5) + leftSegments));
            }

            //radius * cos/sin angle in rads + offset
            Vec2f pos = new Vec2f(radius * MathHelper.cos(angle) + offset.x, radius * MathHelper.sin(angle) + offset.y);

            //render textures
            ActionWheelCustomization cust = data.script.getActionWheelCustomization("SLOT_" + (index + 1));
            if (cust != null && cust.texture != ActionWheelCustomization.TextureType.None && cust.uvOffset != null && cust.uvSize != null) {

                //texture
                Identifier textureId;
                switch (cust.texture) {
                    case Cape -> textureId = Objects.requireNonNullElse(data.playerListEntry.getCapeTexture(), FiguraTexture.DEFAULT_ID);
                    case Elytra -> textureId = Objects.requireNonNullElse(data.playerListEntry.getElytraTexture(), new Identifier("minecraft", "textures/entity/elytra.png"));
                    case Resource -> textureId = MinecraftClient.getInstance().getResourceManager().containsResource(cust.texturePath) ? cust.texturePath : MissingSprite.getMissingSpriteId();
                    case Skin -> textureId = data.playerListEntry.getSkinTexture();
                    case Custom -> textureId = data.texture != null ? data.texture.id : MissingSprite.getMissingSpriteId();
                    default -> textureId = FiguraTexture.DEFAULT_ID;
                }

                RenderSystem.setShaderTexture(0, textureId);
                RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
                matrices.push();
                matrices.scale(1.5f, 1.5f, 1.5f);

                Vec2f off = cust.uvOffset;
                Vec2f len = cust.uvSize;
                Vec2f siz = cust.textureSize;
                Vec2f scl = cust.textureScale;

                drawTexture(matrices, (int) (pos.x + 8 - (siz.x * scl.x) / 2), (int) (pos.y + 8 - (siz.y * scl.y) / 2), (int) (siz.x * scl.x), (int) (siz.y * scl.y), off.x, off.y, (int) (len.x), (int) (len.y), (int) (siz.x), (int) (siz.y));

                matrices.pop();
            }
        }
    }

    public void renderText(MatrixStack matrices, Vec2f pos, int size, float scale, PlayerData data) {
        //customization
        ActionWheelCustomization customization = data.script.getActionWheelCustomization("SLOT_" + (selectedSlot + 1));

        List<Text> lines = NO_FUNCTION_MESSAGE;
        int textColor = Formatting.RED.getColorValue();

        if (customization != null && customization.function != null) {
            if (customization.title == null)
                return;

            Text text;
            try {
                text = Text.Serializer.fromJson(new StringReader(customization.title));

                if (text == null)
                    throw new Exception("Error parsing JSON string");
            } catch (Exception ignored) {
                text = new LiteralText(customization.title);
            }
            lines = TextUtils.splitText(text, "\n");

            textColor = Formatting.WHITE.getColorValue();
        }

        //text pos
        Vec2f textPos;
        int titleLen = this.client.textRenderer.getWidth(lines.get(0)) / 2;

        switch ((int) Config.ACTION_WHEEL_TITLE_POS.value) {
            //top
            case 1 -> textPos = new Vec2f(pos.x - titleLen, (float) Math.max(pos.y - size / 2.0 - 10, 4));
            //bottom
            case 2 -> textPos = new Vec2f(pos.x - titleLen, (float) Math.min(pos.y + size / 2.0 + 4, this.client.getWindow().getHeight() - 12));
            //center
            case 3 -> textPos = new Vec2f(pos.x - titleLen, pos.y - 4);
            //default mouse
            default -> textPos = new Vec2f((float) this.client.mouse.getX() / scale, (float) this.client.mouse.getY() / scale - 10);
        }

        //draw
        matrices.push();
        matrices.translate(0, 0, 599);
        int i = 0;
        for (Text text : lines) {
            drawTextWithShadow(matrices, this.client.textRenderer, text, (int) textPos.x, (int) textPos.y + (i-lines.size()+1)*9, textColor);
            i++;
        }
        matrices.pop();
    }

    public void renderItems(int leftSegments, int rightSegments, Vec2f offset, int radius, PlayerData data) {
        for (int i = 0; i < leftSegments + rightSegments; i++) {

            int index;
            float angle;
            if (i < rightSegments) {
                index = i;
                angle = (float) Math.toRadians(180.0 / rightSegments * (index - ((rightSegments - 1) * 0.5)));
            } else {
                index = i - rightSegments + 4;
                angle = (float) Math.toRadians(180.0 / leftSegments * (index - 4 - ((leftSegments - 1) * 0.5) + leftSegments));
            }

            //radius * cos/sin angle in rads + offset
            Vec2f pos = new Vec2f(radius * MathHelper.cos(angle) + offset.x, radius * MathHelper.sin(angle) + offset.y);

            //get item - defaults to air
            ItemStack item = Registry.ITEM.get(Identifier.tryParse("minecraft:air")).getDefaultStack();

            ActionWheelCustomization cust = data.script.getActionWheelCustomization("SLOT_" + (index + 1));
            if (cust != null) {
                if (cust.texture != ActionWheelCustomization.TextureType.None && cust.uvOffset != null && cust.uvSize != null)
                    continue;

                if (selectedSlot == index && cust.hoverItem != null) {
                    item = cust.hoverItem;
                } else if (cust.item != null) {
                    item = cust.item;
                }
            }

            //render
            MatrixStack matrixStack = RenderSystem.getModelViewStack();
            matrixStack.push();
            matrixStack.scale(1.5f, 1.5f, 1.5f);

            this.client.getItemRenderer().renderGuiItemIcon(item, (int) pos.x, (int) pos.y);

            matrixStack.pop();
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
                            currentData.script.logLuaError((LuaError) error);
                        } else {
                            error.printStackTrace();
                        }
                    }
                }
            }

            enabled = false;
            selectedSlot = -1;
        }
    }
}
