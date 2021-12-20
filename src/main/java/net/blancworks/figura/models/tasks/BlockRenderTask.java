package net.blancworks.figura.models.tasks;

import net.blancworks.figura.models.shaders.FiguraRenderLayer;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3f;

public class BlockRenderTask extends RenderTask {
    public BlockState state;
    public FiguraRenderLayer customLayer;

    public BlockRenderTask(BlockState state, boolean emissive, Vec3f pos, Vec3f rot, Vec3f scale, FiguraRenderLayer customLayer) {
        super(emissive, pos, rot, scale);
        this.state = state;
        this.customLayer = customLayer;
    }

    @Override
    public int render(MatrixStack matrices, VertexConsumerProvider vcp, int light) {
        matrices.push();

        this.transform(matrices);
        matrices.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(180));

        RenderTask.renderLayerOverride(vcp, customLayer);
        MinecraftClient client = MinecraftClient.getInstance();
        client.getBlockRenderManager().renderBlockAsEntity(state, matrices, vcp, emissive ? 0xF000F0 : light, OverlayTexture.DEFAULT_UV);
        RenderTask.resetOverride(vcp);

        int complexity = 4 * client.getBlockRenderManager().getModel(state).getQuads(state, null, client.world.random).size();

        matrices.pop();
        return complexity;
    }
}
