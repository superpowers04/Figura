package net.blancworks.figura.models.tasks;

import net.blancworks.figura.avatar.AvatarData;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3f;

import java.util.List;

public class TextRenderTask extends RenderTask {
    private final TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
    public List<Text> text;
    public int lineSpacing = 9;

    public TextRenderTask(List<Text> text, boolean emissive, Vec3f pos, Vec3f rot, Vec3f scale) {
        super(emissive, pos, rot, scale);
        this.text = text;
    }

    @Override
    public int render(AvatarData data, MatrixStack matrices, VertexConsumerProvider vcp, int light) {
        matrices.push();

        this.transform(matrices);
        matrices.scale(0.025f, 0.025f, 0.025f);

        int instructions = 0;

        for (int i = 0; i < text.size(); i++) {
            instructions += textRenderer.draw(text.get(i), 0, i * lineSpacing, 0xFFFFFF, false, matrices.peek().getModel(), vcp, false, 0, emissive ? 0xF000F0 : light);
        }

        matrices.pop();
        return instructions;
    }
}
