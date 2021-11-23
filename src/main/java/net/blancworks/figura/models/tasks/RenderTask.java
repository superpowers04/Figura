package net.blancworks.figura.models.tasks;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector3f;
public abstract class RenderTask {
    public final boolean emissive;
    public final Vector3f pos;
    public final Vector3f rot;
    public final Vector3f scale;

    protected RenderTask(boolean emissive, Vector3f pos, Vector3f rot, Vector3f scale) {
        this.emissive = emissive;
        this.pos = pos == null ? new Vector3f(0f, 0f, 0f) : pos;
        this.rot = rot == null ? new Vector3f(0f, 0f, 0f) : rot;
        this.scale = scale == null ? new Vector3f(1f, 1f, 1f) : scale;
    }

    public abstract int render(MatrixStack matrices, VertexConsumerProvider vcp, int light);

    public void transform(MatrixStack matrices) {
        matrices.multiply(Vector3f.POSITIVE_X.getDegreesQuaternion(-rot.getX()));
        matrices.multiply(Vector3f.POSITIVE_Y.getDegreesQuaternion(-rot.getY()));
        matrices.multiply(Vector3f.POSITIVE_Z.getDegreesQuaternion(rot.getZ()));
        matrices.translate(pos.getX() / 16f, pos.getY() / 16f, pos.getZ() / 16f);
        matrices.scale(scale.getX(), scale.getY(), scale.getZ());
    }
}
