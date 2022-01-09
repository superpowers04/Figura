package net.blancworks.figura.gui.widgets;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.systems.RenderSystem;
import net.blancworks.figura.avatar.AvatarData;
import net.blancworks.figura.avatar.AvatarDataManager;
import net.blancworks.figura.lua.api.actionWheel.ActionWheelCustomization;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
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

        background(matrices, center, 6);

        enabled = true;
    }

    private static void background(MatrixStack matrices, Vec2f pos, int length) {
        //texture
        RenderSystem.setShaderTexture(0, ACTION_WHEEL);

        int initialAngle = length == 2 ? 90 : 0;

        for (int i = 0; i < length; i++) {
            matrices.push();
            matrices.translate(Math.round(pos.x), Math.round(pos.y), 0f);

            drawTexture(matrices, 0, 0, 28, 28, 0f, 0f, 28, 28, 56, 28);
            matrices.pop();
        }
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
