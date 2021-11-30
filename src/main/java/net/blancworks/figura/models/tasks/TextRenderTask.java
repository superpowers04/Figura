package net.blancworks.figura.models.tasks;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3f;

public class TextRenderTask extends RenderTask {
    private final TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
    private final Text text;

    public TextRenderTask(Text text, boolean emissive, Vec3f pos, Vec3f rot, Vec3f scale) {
        super(emissive, pos, rot, scale);
        this.text = text;
    }

    @Override
    public int render(MatrixStack matrices, VertexConsumerProvider vcp, int light) {
        matrices.push();

        this.transform(matrices);
        matrices.scale(0.025f, 0.025f, 0.025f);

        int instructions = textRenderer.draw(text, 0, 0, 0xFFFFFF, false, matrices.peek().getPositionMatrix(), vcp, false, 0, emissive ? 0xF000F0 : light);

        matrices.pop();
        return instructions;
    }
}
