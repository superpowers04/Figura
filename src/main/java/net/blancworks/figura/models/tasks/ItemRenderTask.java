package net.blancworks.figura.models.tasks;


import net.blancworks.figura.models.shaders.FiguraRenderLayer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3f;

public class ItemRenderTask extends RenderTask {
    public final ItemStack stack;
    public final ModelTransformation.Mode mode;
    public final FiguraRenderLayer customLayer;

    public ItemRenderTask(ItemStack stack, ModelTransformation.Mode mode, boolean emissive, Vec3f pos, Vec3f rot, Vec3f scale, FiguraRenderLayer customLayer) {
        super(emissive, pos, rot, scale);
        this.stack = stack;
        this.mode = mode;
        this.customLayer = customLayer;
    }

    @Override
    public int render(MatrixStack matrices, VertexConsumerProvider vcp, int light) {
        matrices.push();

        this.transform(matrices);
        matrices.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(180));

        RenderTask.renderLayerOverride(vcp, customLayer);
        MinecraftClient client = MinecraftClient.getInstance();
        client.getItemRenderer().renderItem(stack, mode, emissive ? 0xF000F0 : light, OverlayTexture.DEFAULT_UV, matrices, vcp, 0);
        RenderTask.resetOverride(vcp);

        int complexity = 4 * client.getItemRenderer().getModel(stack, null, null, 0).getQuads(null, null, client.world.random).size();

        matrices.pop();
        return complexity;
    }
}
