package net.blancworks.figura.mixin;

import net.blancworks.figura.FiguraMod;
import net.blancworks.figura.PlayerData;
import net.blancworks.figura.PlayerDataManager;
import net.blancworks.figura.access.PlayerEntityRendererAccess;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(PlayerEntityModel.class)
public class PlayerModelRendererMixin<T extends LivingEntity> extends BipedEntityModel<T> {
    public PlayerModelRendererMixin(float scale) {
        super(scale);
    }
    
    @Override
    public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, float red, float green, float blue, float alpha) {
        super.render(matrices, vertices, light, overlay, red, green, blue, alpha);

        PlayerData playerData = FiguraMod.currentData;

        if (playerData != null && playerData.model != null) {
            matrices.push();

            MatrixStack transformStack = new MatrixStack();
            if (playerData.lastEntity != null) {
                PlayerEntityRenderer renderer = (PlayerEntityRenderer) (LivingEntityRenderer<?, ?>) MinecraftClient.getInstance().getEntityRenderDispatcher().getRenderer(playerData.lastEntity);
                PlayerEntityRendererAccess per = (PlayerEntityRendererAccess) renderer;

                Vec3d lastPos = playerData.lastEntity.getPos();
                Vec3d vec3d = renderer.getPositionOffset((AbstractClientPlayerEntity) playerData.lastEntity, FiguraMod.deltaTime);
                double x = lastPos.getX() + vec3d.getX();
                double y = lastPos.getY() + vec3d.getY();
                double z = lastPos.getZ() + vec3d.getZ();

                transformStack.translate(x,y,z);

                float bodyYaw = MathHelper.lerpAngleDegrees(FiguraMod.deltaTime, playerData.lastEntity.prevBodyYaw, playerData.lastEntity.bodyYaw);
                float animationProgress = playerData.lastEntity.age + FiguraMod.deltaTime;

                per.figura$setupTransformsPublic((AbstractClientPlayerEntity) playerData.lastEntity, transformStack, animationProgress, bodyYaw, FiguraMod.deltaTime );

                transformStack.scale(-1.0F, -1.0F, 1.0F);
                transformStack.translate(0.0D, -1.5010000467300415D, 0.0D);
            }

            try {
                playerData.model.render((PlayerEntityModel<T>) (Object) this, matrices, transformStack, FiguraMod.vertexConsumerProvider, light, overlay, alpha);
            } catch (Exception e) {
                e.printStackTrace();
            }

            matrices.pop();
        }
    }
}
