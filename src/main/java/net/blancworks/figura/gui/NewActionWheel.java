package net.blancworks.figura.gui;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.systems.RenderSystem;
import net.blancworks.figura.avatar.AvatarData;
import net.blancworks.figura.avatar.AvatarDataManager;
import net.blancworks.figura.lua.api.actionwheel.ActionWheelCustomization;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3f;

import java.util.List;

public class NewActionWheel extends DrawableHelper {

    private static final Identifier ACTION_WHEEL = new Identifier("figura", "textures/gui/new_action_wheel.png");
    private static final Vec3f ERROR_COLOR = new Vec3f(1f, 0.28f, 0.28f);
    private static final List<Text> NO_FUNCTION_MESSAGE = ImmutableList.of(new TranslatableText("figura.actionwheel.nofunction"));

    public static String selectedSlot;
    public static boolean enabled = false;

    public static void render(MatrixStack matrices) {
        MinecraftClient client = MinecraftClient.getInstance();
        Vec2f center = new Vec2f(client.getWindow().getScaledWidth() / 2f, client.getWindow().getScaledHeight() / 2f);

        AvatarData data = AvatarDataManager.localPlayer;
        int length = 8;

        float initialAngle = (float) Math.toRadians(length == 2 ? 0 : 90);
        float angleStep = (float) Math.toRadians(360f / length);

        matrices.push();
        matrices.translate(Math.round(center.x) - 14f, Math.round(center.y) - 14f, 0f);

        for (int i = 0; i < length; i++) {
            float angle = angleStep * i - initialAngle;

            //background
            Vec2f pos = new Vec2f(48 * MathHelper.cos(angle), 48 * MathHelper.sin(angle)); //radius * cos/sin angle in rads + offset

            matrices.push();
            matrices.translate(pos.x, pos.y, 0f);

            RenderSystem.setShaderTexture(0, ACTION_WHEEL);
            drawTexture(matrices, 0, 0, 28, 28, 0f, 0f, 28, 28, 56, 28);

            matrices.pop();

            //item
            Vec2f pos2 = new Vec2f(24 * MathHelper.cos(angle), 24 * MathHelper.sin(angle));
            MatrixStack matrices2 = RenderSystem.getModelViewStack();

            matrices2.push();
            matrices2.translate(Math.round(center.x) - 8f + pos2.x, Math.round(center.y) - 8f + pos2.y, 0f);

            MinecraftClient.getInstance().getItemRenderer().renderGuiItemIcon(Items.DIAMOND_AXE.getDefaultStack(), (int) pos2.x, (int) pos2.y);

            matrices2.pop();
            RenderSystem.applyModelViewMatrix();
        }

        matrices.pop();

        enabled = true;
    }

    public static boolean mouseScrolled(double d) {
        if (enabled) System.out.println(d);
        return enabled;
    }

    public static void play() {
        if (selectedSlot == null)
            return;

        AvatarData currentData = AvatarDataManager.localPlayer;
        if (currentData != null && currentData.script != null) {
            ActionWheelCustomization slot = currentData.script.newActionWheelSlots.get(selectedSlot);

            if (slot != null && slot.function != null) {
                currentData.script.runActionWheelFunction(slot.function);
            }
        }

        selectedSlot = null;
    }
}
