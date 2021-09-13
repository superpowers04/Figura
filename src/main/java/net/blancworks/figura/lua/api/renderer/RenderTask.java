package net.blancworks.figura.lua.api.renderer;

import net.blancworks.figura.FiguraMod;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3f;

public abstract class RenderTask {
    public final boolean emissive;
    public final Vec3f pos;
    public final Vec3f rot;
    public final Vec3f scale;

    protected RenderTask(boolean emissive, Vec3f pos, Vec3f rot, Vec3f scale) {
        this.emissive = emissive;
        this.pos = pos == null ? Vec3f.ZERO : pos;
        this.rot = rot == null ? Vec3f.ZERO : rot;
        this.scale = scale == null ? new Vec3f(1f, 1f, 1f) : scale;
    }

    public abstract int render(MatrixStack matrices, VertexConsumerProvider vcp, int light);

    public void transform(MatrixStack matrices) {
        matrices.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(-rot.getX()));
        matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(-rot.getY()));
        matrices.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(rot.getZ()));
        matrices.translate(pos.getX() / 16f, pos.getY() / 16f, pos.getZ() / 16f);
        matrices.scale(scale.getX(), scale.getY(), scale.getZ());
    }

    public static class ItemRenderTask extends RenderTask {
        public final ItemStack stack;
        public final ModelTransformation.Mode mode;

        public ItemRenderTask(ItemStack stack, ModelTransformation.Mode mode, boolean emissive, Vec3f pos, Vec3f rot, Vec3f scale) {
            super(emissive, pos, rot, scale);
            this.stack = stack;
            this.mode = mode;
        }

        @Override
        public int render(MatrixStack matrices, VertexConsumerProvider vcp, int light) {
            matrices.push();

            this.transform(matrices);
            matrices.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(180));

            MinecraftClient client = MinecraftClient.getInstance();
            client.getItemRenderer().renderItem(stack, mode, emissive ? 0xF000F0 : light, OverlayTexture.DEFAULT_UV, matrices, vcp, 0);

            int complexity = 4 * client.getItemRenderer().getHeldItemModel(stack, null, null, 0).getQuads(null, null, client.world.random).size();

            matrices.pop();
            return complexity;
        }
    }

    public static class BlockRenderTask extends RenderTask {
        public final BlockState state;

        public BlockRenderTask(BlockState state, boolean emissive, Vec3f pos, Vec3f rot, Vec3f scale) {
            super(emissive, pos, rot, scale);
            this.state = state;
        }

        @Override
        public int render(MatrixStack matrices, VertexConsumerProvider vcp, int light) {
            matrices.push();

            this.transform(matrices);
            matrices.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(180));

            BlockPos pos;
            if (FiguraMod.currentData != null && FiguraMod.currentData.lastEntity != null) {
                pos = FiguraMod.currentData.lastEntity.getBlockPos();
            } else {
                pos = new BlockPos(Vec3d.ZERO);
            }

            MinecraftClient client = MinecraftClient.getInstance();
            client.getBlockRenderManager().renderBlock(state, pos, client.world, matrices, vcp.getBuffer(emissive ? RenderLayer.getEyes(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE) : RenderLayer.getTranslucent()), false, client.world.random);

            int complexity = 4 * client.getBlockRenderManager().getModel(state).getQuads(state, null, client.world.random).size();

            matrices.pop();
            return complexity;
        }
    }

    public static class TextRenderTask extends RenderTask {
        private final TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        private final Text text;

        protected TextRenderTask(Text text, boolean emissive, Vec3f pos, Vec3f rot, Vec3f scale) {
            super(emissive, pos, rot, scale);
            this.text = text;
        }

        @Override
        public int render(MatrixStack matrices, VertexConsumerProvider vcp, int light) {
            matrices.push();

            this.transform(matrices);
            matrices.scale(0.025f, 0.025f, 0.025f);

            int instructions = textRenderer.draw(text, 0, 0, 0xFFFFFF, false, matrices.peek().getModel(), vcp, false, 0, emissive ? 0xF000F0 : light);

            matrices.pop();
            return instructions;
        }
    }
}
