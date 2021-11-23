package net.blancworks.figura.models.tasks;


import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.item.ItemStack;

public class ItemRenderTask extends RenderTask {
    public final ItemStack stack;
    public final ModelTransformation.Mode mode;

    public ItemRenderTask(ItemStack stack, ModelTransformation.Mode mode, boolean emissive, Vector3f pos, Vector3f rot, Vector3f scale) {
        super(emissive, pos, rot, scale);
        this.stack = stack;
        this.mode = mode;
    }

    @Override
    public int render(MatrixStack matrices, VertexConsumerProvider vcp, int light) {
        matrices.push();

        this.transform(matrices);
        matrices.multiply(Vector3f.POSITIVE_X.getDegreesQuaternion(180));

        MinecraftClient client = MinecraftClient.getInstance();
        client.getItemRenderer().renderItem(stack, mode, emissive ? 0xF000F0 : light, OverlayTexture.DEFAULT_UV, matrices, vcp);

        int complexity = 4 * client.getItemRenderer().getHeldItemModel(stack, null, null).getQuads(null, null, client.world.random).size();

        matrices.pop();
        return complexity;
    }
}
