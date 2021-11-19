package net.blancworks.figura.models.tasks;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector3f;

public class BlockRenderTask extends RenderTask {
    public final BlockState state;

    public BlockRenderTask(BlockState state, boolean emissive, Vector3f pos, Vector3f rot, Vector3f scale) {
        super(emissive, pos, rot, scale);
        this.state = state;
    }

    @Override
    public int render(MatrixStack matrices, VertexConsumerProvider vcp, int light) {
        matrices.push();

        this.transform(matrices);
        matrices.multiply(Vector3f.POSITIVE_X.getDegreesQuaternion(180));

        MinecraftClient client = MinecraftClient.getInstance();
        client.getBlockRenderManager().renderBlockAsEntity(state, matrices, vcp, emissive ? 0xF000F0 : light, OverlayTexture.DEFAULT_UV);

        int complexity = 4 * client.getBlockRenderManager().getModel(state).getQuads(state, null, client.world.random).size();

        matrices.pop();
        return complexity;
    }
}
