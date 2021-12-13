package net.blancworks.figura.mixin;

import net.blancworks.figura.avatar.AvatarData;
import net.blancworks.figura.access.PlayerEntityRendererAccess;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.PiglinEntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(PlayerEntityModel.class)
public class PlayerEntityModelMixin<T extends LivingEntity> extends BipedEntityModel<T> {

    public PlayerEntityModelMixin(ModelPart root) {
        super(root);
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, float red, float green, float blue, float alpha) {
        super.render(matrices, vertices, light, overlay, red, green, blue, alpha);

        AvatarData data = AvatarData.currentRenderingData;
        if (data == null || data.model == null || data.lastEntity == null || (data.lastEntity instanceof PlayerEntity && ((Object) this) instanceof PiglinEntityModel)) return;

        matrices.push();

        try {
            MatrixStack transformStack = new MatrixStack();

            if (data.lastEntity instanceof AbstractClientPlayerEntity entity) {
                PlayerEntityRenderer renderer = (PlayerEntityRenderer) (LivingEntityRenderer<?, ?>) MinecraftClient.getInstance().getEntityRenderDispatcher().getRenderer((PlayerEntity) data.lastEntity);
                PlayerEntityRendererAccess per = (PlayerEntityRendererAccess) renderer;

                Vec3d lastPos = entity.getPos();
                Vec3d vec3d = renderer.getPositionOffset(entity, data.deltaTime);
                double x = lastPos.getX() + vec3d.getX();
                double y = lastPos.getY() + vec3d.getY();
                double z = lastPos.getZ() + vec3d.getZ();

                transformStack.translate(x, y, z);

                float bodyYaw = MathHelper.lerpAngleDegrees(data.deltaTime, entity.prevBodyYaw, entity.bodyYaw);
                float animationProgress = entity.age + data.deltaTime;

                per.figura$setupTransformsPublic(entity, transformStack, animationProgress, bodyYaw, data.deltaTime);
            }

            transformStack.scale(-1.0F, -1.0F, 1.0F);
            transformStack.translate(0.0D, -1.5010000467300415D, 0.0D);

            data.model.render((PlayerEntityModel<T>) (Object) this, matrices, transformStack, data.getVCP(), light, overlay, alpha);
        } catch (Exception e) {
            e.printStackTrace();
        }

        matrices.pop();
    }
}
