package net.blancworks.figura.models.tasks;

import net.blancworks.figura.models.shaders.FiguraRenderLayer;
import net.blancworks.figura.models.shaders.FiguraVertexConsumerProvider;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3f;

public abstract class RenderTask {
    public boolean emissive;
    public Vec3f pos;
    public Vec3f rot;
    public Vec3f scale;
    private static FiguraRenderLayer storedOverride;

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

    public static void renderLayerOverride(VertexConsumerProvider vcp, FiguraRenderLayer override) {
        if (vcp instanceof FiguraVertexConsumerProvider) {
            storedOverride = ((FiguraVertexConsumerProvider) vcp).overrideLayer;
            ((FiguraVertexConsumerProvider) vcp).overrideLayer = override;
        }
    }

    public static void resetOverride(VertexConsumerProvider vcp) {
        if (vcp instanceof FiguraVertexConsumerProvider) {
            ((FiguraVertexConsumerProvider) vcp).overrideLayer = storedOverride;
        }
    }
}
